package cn.gbtmedia.gbt28181.server.media;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.client.TcpPassiveClient;
import cn.gbtmedia.gbt28181.server.media.client.UdpClient;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.media.server.netty.SinglePortMediaServer;
import cn.gbtmedia.gbt28181.server.media.server.TcpActiveServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 单端口收流
 * @author xqs
 */
@Slf4j
@Component("gbt28181SinglePortMediaService")
@ConditionalOnProperty(prefix = "server-config.gbt28181", name = "mediaModel", havingValue = "single")
public class SinglePortMediaService implements MediaService{

    @Override
    public MediaServer createServer(MediaParam mediaParam) {
        MediaServer mediaServer = null;
        String mediaTransport = mediaParam.getMediaTransport();
        if(MediaTransport.udp.name().equals(mediaTransport)){
            mediaServer = new SinglePortMediaServer();
        }
        if(MediaTransport.tcpPassive.name().equals(mediaTransport)){
            mediaServer = new SinglePortMediaServer();
        }
        if(MediaTransport.tcpActive.name().equals(mediaTransport)){
            mediaServer = new TcpActiveServer();
        }
        if(mediaServer == null){
            log.error("createServer no mediaTransport {}",mediaTransport);
            return null;
        }
        mediaServer.setMediaParam(mediaParam);
        mediaServer.setSsrc(mediaParam.getSsrc());
        mediaServer.setMediaTransport(mediaTransport);
        ServerConfig serverConfig = ServerConfig.getInstance();
        String serverIp = serverConfig.getPublicIp();
        int flvPort = serverConfig.getGbt28181().getFlvPort();
        String httpFlv = String.format("http://%s:%s/video/%s.flv",serverIp,flvPort, mediaParam.getSsrc());
        mediaServer.setHttpFlv(httpFlv);
        // udp 或者 tcpPassive 直接启动流媒体服务器，绑定对应端口
        if(mediaTransport.equals(MediaTransport.udp.name()) || mediaTransport.equals(MediaTransport.tcpPassive.name()) ){
            boolean started = mediaServer.start();
            if(!started){
                return null;
            }
        }else {
            // 就算是tcpActive 默认也返回个ip 不然信令下发没有携带会报错
            mediaServer.setMediaIp(serverConfig.getAccessIp());
            mediaServer.setMediaPort(serverConfig.getGbt28181().getMediaSinglePort());
        }
        return mediaServer;
    }

    @Override
    public MediaClient createClient(MediaParam mediaParam) {
        MediaClient mediaClient = null;
        String mediaTransport = mediaParam.getMediaTransport();
        if(MediaTransport.udp.name().equals(mediaTransport)){
            mediaClient = new UdpClient();
        }
        if(MediaTransport.tcpPassive.name().equals(mediaTransport)){
            mediaClient = new TcpPassiveClient();
        }
        if(MediaTransport.tcpActive.name().equals(mediaTransport)){
            log.error("singlePort model no support tcpActiveClient");
        }
        if(mediaClient == null){
            log.error("createClient no mediaTransport {}",mediaTransport);
            return null;
        }
        mediaClient.setMediaParam(mediaParam);
        mediaClient.setSsrc(mediaParam.getSsrc());
        mediaClient.setCallId(mediaParam.getCallId());
        mediaClient.setMediaTransport(mediaTransport);
        ServerConfig serverConfig = ServerConfig.getInstance();
        String serverIp = serverConfig.getPublicIp();
        int flvPort = serverConfig.getGbt28181().getFlvPort();
        String httpFlv = String.format("http://%s:%s/video/%s.flv",serverIp,flvPort, mediaParam.getSsrc());
        mediaClient.setHttpFlv(httpFlv);
        return mediaClient;
    }
}
