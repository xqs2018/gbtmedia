package cn.gbtmedia.gbt28181.server.media.zlmediakit;

import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.MediaService;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.zlmediakit.ZlmediakitApi;
import cn.gbtmedia.zlmediakit.ZlmediakitConfig;
import cn.gbtmedia.zlmediakit.ZlmediakitEvent;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author xqs
 */
@Slf4j
@Data
@Component("gbt28181ZlmediakitMediaService")
@ConditionalOnProperty(prefix = "server-config.gbt28181", name = "mediaModel", havingValue = "zlmediakit")
@Import(ZlmediakitConfig.class)
public class ZlmediakitMediaService implements MediaService{

    public static ZlmediakitMediaService getInstance(){
        return SpringUtil.getBean(ZlmediakitMediaService.class);
    }

    @Resource
    private ZlmediakitConfig zlmConfig;

    @Resource
    private ZlmediakitApi zlmApi;

    private Map<String, ZlmediakitMediaServer> mediaServerMap = new ConcurrentHashMap<>();

    @Override
    public MediaServer createServer(MediaParam mediaParam) {
        String mediaTransport = mediaParam.getMediaTransport();
        ZlmediakitMediaServer mediaServer = new ZlmediakitMediaServer();
        mediaServer.setMediaParam(mediaParam);
        mediaServer.setSsrc(mediaParam.getSsrc());
        mediaServer.setMediaTransport(mediaTransport);
        mediaServer.setZlmApi(zlmApi);
        mediaServer.setZlmConfig(zlmConfig);
        mediaServer.setZlmService(this);
        String streamId = String.format("%08x", Integer.parseInt(mediaParam.getSsrc())).toUpperCase();
        mediaServer.setStreamId(streamId);
        String httpFlv = String.format("http://%s:%s/%s/%s.live.flv", zlmConfig.getPublicIp(), zlmConfig.getHttpPort(),"rtp", streamId);
        mediaServer.setHttpFlv(httpFlv);
        // udp 或者 tcpPassive 直接启动流媒体服务器，绑定对应端口
        if(mediaTransport.equals(MediaTransport.udp.name()) || mediaTransport.equals(MediaTransport.tcpPassive.name()) ){
            boolean started = mediaServer.start();
            if(!started){
                return null;
            }
        }else {
            // 就算是tcpActive 默认也返回个ip 不然信令下发没有携带会报错
            mediaServer.setMediaIp(zlmConfig.getAccessIp());
            mediaServer.setMediaPort(zlmConfig.getRtpProxyPort());
        }
        mediaServerMap.put(streamId,mediaServer);
        return mediaServer;
    }

    @Override
    public MediaClient createClient(MediaParam mediaParam) {
        String mediaTransport = mediaParam.getMediaTransport();
        ZlmediakitMediaClient mediaClient = new ZlmediakitMediaClient();
        mediaClient.setMediaParam(mediaParam);
        mediaClient.setSsrc(mediaParam.getSsrc());
        mediaClient.setCallId(mediaParam.getCallId());
        mediaClient.setMediaTransport(mediaTransport);
        mediaClient.setZlmApi(zlmApi);
        mediaClient.setZlmConfig(zlmConfig);
        mediaClient.setZlmService(this);
        mediaClient.setMediaServer(mediaParam.getMediaServer());
        // tcpActive 直接启动流媒体客户端，绑定对应端口
        if(mediaTransport.equals(MediaTransport.tcpActive.name())){
            boolean started = mediaClient.start();
            if(!started){
                return null;
            }
        }
        return mediaClient;
    }

    @EventListener
    public void zlmediakitEvent(ZlmediakitEvent event){
        ZlmediakitMediaServer mediaServer = mediaServerMap.get(event.getStream());
        if(mediaServer == null){
            return;
        }
        int type = event.getType();
        if(type == 1){
            log.info("zlmediakitEvent regist ssrc {}",mediaServer.getSsrc());
            mediaServer.onStreamRegist();
            return;
        }
        if(type == 2){
            log.info("zlmediakitEvent unRegist ssrc {}",mediaServer.getSsrc());
            mediaServer.stop();
            return;
        }
        if(type == 3){
            log.info("zlmediakitEvent noneReader ssrc {}",mediaServer.getSsrc());
            mediaServer.stop();
        }
    }
}
