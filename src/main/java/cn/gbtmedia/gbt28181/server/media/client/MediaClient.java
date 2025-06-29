package cn.gbtmedia.gbt28181.server.media.client;

import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.event.MediaClientStopEvent;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
@Data
public abstract class MediaClient {

    protected MediaParam mediaParam;

    protected volatile boolean isStop;

    protected volatile boolean isStart;

    protected String callId;

    protected String ssrc;

    protected String mediaIp;

    protected int mediaPort;

    protected String mediaTransport;

    protected String httpFlv;

    private MediaServer mediaServer;

    private final Map<String,Consumer<RtpMessage>> consumerRtpMessageMap = new ConcurrentHashMap<>();

    private boolean isSendRtpMessage;

    private boolean isReceiveRtpMessage;

    private long[] rxCount = new long[3];

    private long[] txCount = new long[3];

    private volatile boolean isSendspsAndpps;

    private final List<RtpMessage> spsAndppsCache = new CopyOnWriteArrayList<>();

    public void addRtpMessageListener(String key ,Consumer<RtpMessage> consumer){
        consumerRtpMessageMap.put(key,consumer);
    }

    public void removeRtpMessageListener(String key){
        consumerRtpMessageMap.remove(key);
    }

    public String getRxRate(){return (rxCount[2] / 1204) +"kb/s";}

    public String getTxRate(){return (txCount[2] / 1204) +"kb/s";}

    public boolean start(){
        try {
            if(isStart){
                return true;
            }
            isStart = true;
            log.info("mediaClient start callId {}",callId);
            doStart();
        }catch (Exception ex){
            log.error("mediaClient start ex",ex);
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
        log.info("mediaClient stop callId {}",callId);
        doStop();
        MediaClientStopEvent event = new MediaClientStopEvent(this, callId);
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
                log.info("sendRtpMessage callId {} ssrc {}",callId, rtpMessage.getSsrc());
                isSendRtpMessage = true;
            }
            // 速率计算
            rateCount(txCount, rtpMessage.getPayload().length + 12);
            // 发送数据
            doSendRtpMessage(rtpMessage);
        } catch (Exception ex) {
            log.error("sendRtpMessage ex",ex);
            stop();
        }
    }

    public abstract void doSendRtpMessage(RtpMessage rtpMessage) throws Exception;

    public void receiveRtpMessage(RtpMessage rtpMessage){
        try {
            // 已经开始推流
            if(!isReceiveRtpMessage){
                log.info("receiveRtpMessage callId {} ssrc {}",callId, rtpMessage.getSsrc());
                isReceiveRtpMessage = true;
            }
            // 速率计算
            rateCount(rxCount, rtpMessage.getPayload().length + 12);
            // 转发发送数据  最开始的包有sps和pps 需要缓存下来  h265 是vps sps pps i帧 统一缓存四个包
            if(spsAndppsCache.size() < 4){
                spsAndppsCache.add(rtpMessage);
            }else {
                if(!mediaServer.isSendspsAndpps()){
                    log.info("send sps and pps to mediaServer ssrc {}",mediaServer.getSsrc());
                    spsAndppsCache.forEach(mediaServer::sendRtpMessage);
                    mediaServer.setSendspsAndpps(true);
                }
                mediaServer.sendRtpMessage(rtpMessage);
            }
            consumerRtpMessageMap.values().forEach(c->c.accept(rtpMessage));
        }catch (Exception ex){
            log.error("receiveRtpMessage ex",ex);
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
