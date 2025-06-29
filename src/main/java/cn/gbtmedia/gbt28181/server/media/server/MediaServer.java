package cn.gbtmedia.gbt28181.server.media.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.event.MediaServerStopEvent;
import cn.gbtmedia.gbt28181.server.media.record.RecordManger;
import cn.gbtmedia.gbt28181.server.media.record.RecordParam;
import cn.gbtmedia.gbt28181.server.media.record.RecordTask;
import cn.gbtmedia.gbt28181.server.media.stream.StreamManger;
import cn.gbtmedia.gbt28181.server.media.stream.StreamTask;
import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * @author xqs
 */
@Data
@Slf4j
public abstract class MediaServer {

    protected MediaParam mediaParam;

    protected volatile boolean isStop;

    protected volatile boolean isStart;

    protected String callId;

    protected String ssrc;

    protected String mediaIp;

    protected int mediaPort;

    protected String mediaTransport;

    protected String httpFlv;

    private final List<MediaClient> mediaClients = new CopyOnWriteArrayList<>();

    private final Map<String,Consumer<RtpMessage>> consumerRtpMessageMap = new ConcurrentHashMap<>();

    private final CountDownLatch mediaLatch = new CountDownLatch(1);

    private final CountDownLatch streamLatch = new CountDownLatch(1);

    private StreamTask streamTask;

    private RecordTask recordTask;

    private boolean isSendRtpMessage;

    private boolean isReceiveRtpMessage;

    private long[] rxCount = new long[3];

    private long[] txCount = new long[3];

    private volatile boolean isSendspsAndpps;

    private long lastReceiveRtpMessageTime;

    private final List<RtpMessage> spsAndppsCache = new CopyOnWriteArrayList<>();

    public void addMediaClient(MediaClient mediaClient){
        mediaClients.add(mediaClient);
    }

    public void removeMediaClient(MediaClient mediaClient){
        mediaClients.removeIf(s -> s.getCallId().equals(mediaClient.getCallId()));
    }

    public void addRtpMessageListener(String key ,Consumer<RtpMessage> consumer){
        consumerRtpMessageMap.put(key,consumer);
    }

    public void removeRtpMessageListener(String key){
        consumerRtpMessageMap.remove(key);
    }

    public boolean awaitMedia(){
        try {
             return mediaLatch.await(ServerConfig.getInstance().getGbt28181().getMediaTimeOut(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error("awaitMedia ex",ex);
            return false;
        }
    }

    public boolean awaitStream(){
        try {
            return streamLatch.await(ServerConfig.getInstance().getGbt28181().getMediaTimeOut(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error("awaitStream ex",ex);
            return false;
        }
    }

    public int getViewNum(){
        return streamTask==null?0:streamTask.getSubscribers().size();
    }

    public String getRecordProgress(){
        return recordTask != null?recordTask.getRecordFileSecond().get() +"/" + mediaParam.getRecordSecond()
                :streamTask.getRecordFileSecond() +"/" + mediaParam.getRecordSecond();
    }

    public String getRxRate(){return (rxCount[2] / 1204) +"kb/s";}

    public String getTxRate(){return (txCount[2] / 1204) +"kb/s";}

    public void resetSsrc(String newSsrc){
        log.info("reset streamTask ssrc {} newSsrc {} callId {}",ssrc,newSsrc, callId);
        StreamManger.getInstance().unPublish(ssrc);
        ssrc = newSsrc;
        startStreamTask();
    }

    private void startStreamTask(){
        streamTask = StreamManger.getInstance().getStreamTask(ssrc);
        streamTask.setRecordPath(mediaParam.getRecordPath());
        streamTask.setRecordSecond(mediaParam.getRecordSecond());
        streamTask.setRecordSlice(mediaParam.isRecordSlice());
        // 流内部开启录制 TODO
        streamTask.setRecordEnable(false);
        if(MediaType.download.name().equals(mediaParam.getMediaType())){
           //streamTask.setModifyStamp(1);
        }
        streamTask.addStreamReadyCallback(streamLatch::countDown);
    }

    public boolean start(){
        try {
            if(isStart){
                return true;
            }
            isStart = true;
            log.info("mediaServer start ssrc {}",ssrc);
            startStreamTask();
            doStart();
        }catch (Exception ex){
            log.error("mediaServer start ex",ex);
            stop();
            return false;
        }
        return true;
    }

    public void stop(){
        if(isStop){
            return;
        }
        isStop = true;
        log.info("mediaServer stop ssrc {}",ssrc);
        StreamManger.getInstance().unPublish(ssrc);
        doStop();
        // 停止录像
        stopRecord();
        MediaServerStopEvent event = new MediaServerStopEvent(this, ssrc);
        SpringUtil.getApplicationContext().publishEvent(event);
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop();

    public void sendRtpMessage(RtpMessage rtpMessage){
        if(!isStart){
            return;
        }
        if (isStop){
            return;
        }
        try {
            // 已经开始推流
            if(!isSendRtpMessage){
                log.info("sendRtpMessage ssrc {}", rtpMessage.getSsrc());
                isSendRtpMessage = true;
            }
            // 速率计算
            rateCount(txCount, rtpMessage.getPayload().length + 12);
            // 发送数据
            doSendRtpMessage(rtpMessage);
        } catch (Exception ex) {
          log.error("sendRtpMessage ex", ex);
          stop();
        }
    }

    public abstract void doSendRtpMessage(RtpMessage rtpMessage) throws Exception;

    public void receiveRtpMessage(RtpMessage rtpMessage){
        try {
            // 已经开始推流
            if(!isReceiveRtpMessage){
                log.info("receiveRtpMessage ssrc {}",rtpMessage.getSsrc());
                isReceiveRtpMessage = true;
                mediaLatch.countDown();
            }
            // 速率计算
            rateCount(rxCount, rtpMessage.getPayload().length + 12);
            // 把流转发到其它客户端  最开始的包有sps和pps 需要缓存下来  h265 是vps sps pps i帧 统一缓存四个包
            if(spsAndppsCache.size() < 4){
                spsAndppsCache.add(rtpMessage);
            }else {
                mediaClients.forEach(v->{
                    if(!v.isSendspsAndpps()){
                        log.info("send sps and pps to mediaClient callId {}",v.getCallId());
                        spsAndppsCache.forEach(v::sendRtpMessage);
                        v.setSendspsAndpps(true);
                    }
                    v.sendRtpMessage(rtpMessage);
                });
            }
            consumerRtpMessageMap.values().forEach(c->c.accept(rtpMessage));
            streamTask.pushRtpMessage(rtpMessage);
            // 检查是否需要录制
            startRecord();
        }catch (Exception ex){
            log.error("receiveRtpMessage ex", ex);
            stop();
        }
    }

    private void rateCount(long[] count,int dataLength){
        long time = System.currentTimeMillis();
        boolean second = time - count[0] > 1000;
        count[0] = second ? time : count[0];
        count[2] = second ? count[1] : count[2];
        count[1] = second ? dataLength : count[1] + dataLength;
    }

    protected synchronized void startRecord(){
        // 流已经开启录制了，不用单独启动录制任务
        if(streamTask != null && streamTask.isRecordEnable()){
            return;
        }
        if(ObjectUtil.isNotEmpty(mediaParam.getRecordPath()) && recordTask == null){
            RecordParam param = new RecordParam();
            param.setSsrc(ssrc);
            param.setPullUrl(getRecorderUrl());
            param.setRecordPath(mediaParam.getRecordPath());
            param.setRecordSecond(mediaParam.getRecordSecond());
            param.setRecordSlice(mediaParam.isRecordSlice());
            recordTask = RecordManger.getInstance().createTask(param);
            recordTask.start();
        }
    }

    protected void stopRecord(){
        if(recordTask != null){
            recordTask.stop();
        }
    }

    protected String getRecorderUrl(){
        // 使用本机ip拉流录制
        ServerConfig serverConfig = ServerConfig.getInstance();
        String serverIp ="127.0.0.1";
        int flvPort = serverConfig.getGbt28181().getFlvPort();
        return String.format("http://%s:%s/video/%s.flv",serverIp,flvPort, mediaParam.getSsrc());
    }

}
