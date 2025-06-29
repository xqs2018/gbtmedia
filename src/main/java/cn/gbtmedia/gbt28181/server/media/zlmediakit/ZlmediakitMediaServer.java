package cn.gbtmedia.gbt28181.server.media.zlmediakit;

import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.event.MediaServerStopEvent;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.zlmediakit.ZlmediakitApi;
import cn.gbtmedia.zlmediakit.ZlmediakitConfig;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
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
public class ZlmediakitMediaServer extends MediaServer {

    private ZlmediakitConfig zlmConfig;

    private ZlmediakitApi zlmApi;

    private ZlmediakitMediaService zlmService;

    private String streamId;

    @Override
    public int getViewNum(){
        JSONObject params = new JSONObject();
        params.set("stream",streamId);
        JSONObject mediaList = zlmApi.getMediaList(params);
        JSONArray data = mediaList.getJSONArray("data");
        if(ObjectUtil.isNotEmpty(data)){
            JSONObject v = (JSONObject) data.get(0);
            return v.getInt("totalReaderCount");
        }
        return 0;
    }

    @Override
    public String getRxRate(){
        JSONObject params = new JSONObject();
        params.set("stream",streamId);
        JSONObject mediaList = zlmApi.getMediaList(params);
        JSONArray data = mediaList.getJSONArray("data");
        if(ObjectUtil.isNotEmpty(data)){
            JSONObject v = (JSONObject) data.get(0);
            return (v.getInt("bytesSpeed")/1024) + "kb/s";
        }
        return "0kb/s";
    }

    @Override
    public boolean start(){
        try {
            if(isStart){
                return true;
            }
            isStart = true;
            log.info("mediaServer start ssrc {}",ssrc);
            doStart();
        }catch (Exception ex){
            log.error("mediaServer start ex",ex);
            stop();
            return false;
        }
        return true;
    }

    @Override
    public void stop(){
        if(isStop){
            return;
        }
        isStop = true;
        log.info("mediaServer stop ssrc {}",ssrc);
        doStop();
        MediaServerStopEvent event = new MediaServerStopEvent(this, ssrc);
        SpringUtil.getApplicationContext().publishEvent(event);
    }

    @Override
    protected void doStart() throws Exception {
        String mediaTransport = mediaParam.getMediaTransport();
        String mediaIp = zlmConfig.getAccessIp();
        int mediaPort = 0;
        if(zlmConfig.getMediaModel().equals("single")){
            mediaPort = zlmConfig.getRtpProxyPort();
        }
        if(zlmConfig.getMediaModel().equals("multiple")){
            int tcp_mode = 0;
            if(mediaTransport.equals(MediaTransport.tcpPassive.name())){
                tcp_mode = 1;
            }
            if(mediaTransport.equals(MediaTransport.tcpActive.name())){
                tcp_mode = 2;
            }
            JSONObject params = new JSONObject();
            params.set("port",0);
            params.set("tcp_mode", tcp_mode);
            params.set("stream_id",streamId);
            log.info("openRtpServer params {}",params);
            JSONObject result = zlmApi.openRtpServer(params);
            log.info("openRtpServer result {}",result);
            if(result.getInt("code") != 0){
                throw new RuntimeException("openRtpServer err " + result);
            }
            mediaPort = result.getInt("port");
        }
        if(mediaTransport.equals(MediaTransport.tcpActive.name())){
            JSONObject params = new JSONObject();
            params.set("dst_url", getMediaIp());
            params.set("dst_port", getMediaPort());
            params.set("stream_id", streamId);
            log.info("connectRtpServer params {}",params);
            JSONObject result = zlmApi.connectRtpServer(params);
            log.info("connectRtpServer result {}",result);
        }else {
            setMediaIp(mediaIp);
            setMediaPort(mediaPort);
        }
    }

    @Override
    protected synchronized void doStop() {
        if(zlmConfig.getMediaModel().equals("multiple")){
            JSONObject params = new JSONObject();
            params.set("stream_id",streamId);
            log.info("closeRtpServer params {}",params);
            JSONObject result = zlmApi.closeRtpServer(params);
            log.info("closeRtpServer result {}",result);
        }
        zlmService.getMediaServerMap().remove(streamId);
        // 停止录像
        stopRecord();
    }

    private volatile boolean isStreamRegist;

    public void onStreamRegist(){
        if(isStreamRegist){
            return;
        }
        isStreamRegist = true;
        // 开始录像
        startRecord();

        getMediaLatch().countDown();
        getStreamLatch().countDown();
    }

    @Override
    protected String getRecorderUrl(){
        // 使用内网ip拉流录制
        return String.format("http://%s:%s/%s/%s.live.flv", zlmConfig.getAccessIp(), zlmConfig.getHttpPort(),"rtp", streamId);
    }

    @Override
    public void addMediaClient(MediaClient mediaClient){
        super.addMediaClient(mediaClient);

    }

    @Override
    public void removeMediaClient(MediaClient mediaClient){
        super.removeMediaClient(mediaClient);
    }

    @Override
    public void doSendRtpMessage(RtpMessage rtpMessage) throws Exception {

    }
}
