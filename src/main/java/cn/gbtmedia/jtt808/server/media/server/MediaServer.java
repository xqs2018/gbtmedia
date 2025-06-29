package cn.gbtmedia.jtt808.server.media.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.server.media.MediaParam;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import cn.gbtmedia.jtt808.server.media.event.MediaServerStopEvent;
import cn.gbtmedia.jtt808.server.media.stream.StreamManger;
import cn.gbtmedia.jtt808.server.media.stream.StreamTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Data
@Slf4j
public abstract class MediaServer {

    protected MediaParam mediaParam;

    protected String mediaKey;

    protected String mediaType;

    protected String mediaIp;

    protected int mediaPort;

    protected String httFlv;

    protected volatile boolean isStop;

    protected volatile boolean isStart;

    private StreamTask streamTask;

    private final CountDownLatch mediaLatch = new CountDownLatch(1);

    private final CountDownLatch streamLatch = new CountDownLatch(1);

    private boolean isSendJtt1078RtpMessage;

    private boolean isReceiveJtt1078RtpMessage;

    private long[] rxCount = new long[3];

    private long[] txCount = new long[3];

    public String getRxRate(){return (rxCount[2] / 1204) +"kb/s";}

    public String getTxRate(){return (txCount[2] / 1204) +"kb/s";}

    public int getViewNum(){
        return streamTask==null?0:streamTask.getSubscribers().size();
    }

    public boolean awaitMedia(){
        try {
            return mediaLatch.await(ServerConfig.getInstance().getJtt808().getMediaTimeOut(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error("awaitMedia ex",ex);
            return false;
        }
    }

    public boolean awaitStream(){
        try {
            return streamLatch.await(ServerConfig.getInstance().getJtt808().getMediaTimeOut(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error("awaitStream ex",ex);
            return false;
        }
    }

    private void startStreamTask(){
        streamTask = StreamManger.getInstance().getStreamTask(mediaKey);
        streamTask.addStreamReadyCallback(streamLatch::countDown);
    }

    public boolean start(){
        try {
            if(isStart){
                return true;
            }
            isStart = true;
            log.info("mediaServer start mediaKey {}",mediaKey);
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
        log.info("mediaServer stop mediaKey {}",mediaKey);
        StreamManger.getInstance().unPublish(mediaKey);
        doStop();
        MediaServerStopEvent event = new MediaServerStopEvent(this, mediaKey);
        SpringUtil.getApplicationContext().publishEvent(event);
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop();

    public abstract void doSend1078RtpMessage(Jtt1078RtpMessage Jtt1078RtpMessage) throws Exception;

    public void sendJtt1078RtpMessage(Jtt1078RtpMessage message){
        if(!isStart){
            return;
        }
        if (isStop){
            return;
        }
        try {
            // 已经开始推流
            if(!isSendJtt1078RtpMessage){
                log.info("sendJtt1078RtpMessage mediaKey {}  clientId {} channelNo {}", mediaKey,
                        message.getClientIdStr(), message.getChannelNo());
                isSendJtt1078RtpMessage = true;
            }
            // 速率计算
            rateCount(txCount, message.getPayload().length + 28);
            // 发送数据
            doSend1078RtpMessage(message);
        } catch (Exception ex) {
            log.error("sendJtt1078RtpMessage ex", ex);
            stop();
        }
    }

    public void receiveJtt1078RtpMessage(Jtt1078RtpMessage message){
        try {
            // 已经开始推流
            if(!isReceiveJtt1078RtpMessage){
                log.info("receiveJtt1078RtpMessage mediaKey {}  clientId {} channelNo {}",mediaKey,
                        message.getClientIdStr(), message.getChannelNo());
                isReceiveJtt1078RtpMessage = true;
                mediaLatch.countDown();
            }
            // 速率计算
            rateCount(rxCount, message.getPayload().length + 28);
            streamTask.pushJtt1078RtpMessage(message);
        }catch (Exception ex){
            log.error("receiveJtt1078RtpMessage ex", ex);
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
}
