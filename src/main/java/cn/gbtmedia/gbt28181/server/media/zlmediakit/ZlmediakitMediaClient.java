package cn.gbtmedia.gbt28181.server.media.zlmediakit;

import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.zlmediakit.ZlmediakitApi;
import cn.gbtmedia.zlmediakit.ZlmediakitConfig;
import cn.hutool.json.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;


/**
 * @author xqs
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ZlmediakitMediaClient extends MediaClient {

    private ZlmediakitConfig zlmConfig;

    private ZlmediakitApi zlmApi;

    private ZlmediakitMediaService zlmService;

    @Override
    protected void doStart() throws Exception {
        ZlmediakitMediaServer mediaServer = (ZlmediakitMediaServer) getMediaServer();
        JSONObject params = new JSONObject();
        params.set("vhost","__defaultVhost__");
        params.set("app", "rtp");
        params.set("stream", mediaServer.getStreamId());
        params.set("ssrc", ssrc);
        if(mediaParam.getMediaTransport().equals(MediaTransport.tcpActive.name())){
            int freePort = zlmApi.getFreePort();
            params.set("src_port", freePort);
            log.info("startSendRtpPassive params {}",params);
            JSONObject result = zlmApi.startSendRtpPassive(params);
            log.info("startSendRtpPassive result {}",result);
            mediaIp = zlmConfig.getAccessIp();
            mediaPort = freePort;
        }else {
            params.set("dst_url", mediaIp);
            params.set("dst_port", mediaPort);
            params.set("is_udp", mediaTransport.equals(MediaTransport.udp.name())?"1":"0");
            log.info("startSendRtp params {}",params);
            JSONObject result = zlmApi.startSendRtp(params);
            log.info("startSendRtp result {}",result);
        }
    }

    @Override
    protected void doStop() {
        ZlmediakitMediaServer mediaServer = (ZlmediakitMediaServer) getMediaServer();
        JSONObject params = new JSONObject();
        params.set("vhost","__defaultVhost__");
        params.set("app","rtp");
        params.set("stream", mediaServer.getStreamId());
        params.set("ssrc",ssrc);
        log.info("stopSendRtp params {}",params);
        JSONObject result = zlmApi.stopSendRtp(params);
        log.info("stopSendRtp result {}",result);
    }

    @Override
    public void doSendRtpMessage(RtpMessage rtpMessage) throws Exception {

    }
}
