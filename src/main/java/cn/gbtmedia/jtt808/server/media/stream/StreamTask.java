package cn.gbtmedia.jtt808.server.media.stream;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import cn.gbtmedia.jtt808.server.flv.FlvSubscriber;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author xqs
 */
@Slf4j
@Data
public class StreamTask implements Runnable{

    private volatile boolean isStop;

    private volatile boolean isStreamReady;

    private final String mediaKey;

    private ArrayBlockingQueue<Jtt1078RtpMessage> messageQueue = new ArrayBlockingQueue<>(1024 *10);

    private final List<FlvSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private FlvEncoder flvEncoder = new FlvEncoder();

    private final List<Runnable> streamReadyCallback = new CopyOnWriteArrayList<>();

    public void addStreamReadyCallback(Runnable runnable){
        streamReadyCallback.add(runnable);
    }

    public void addSubscriber(FlvSubscriber subscriber){
        subscribers.add(subscriber);
    }

    public void removeSubscriber(FlvSubscriber subscriber){
        subscribers.removeIf(s -> subscriber.getChannel().id().asLongText()
                .equals(s.getChannel().id().asLongText()));
    }

    public void pushJtt1078RtpMessage(Jtt1078RtpMessage message){
        messageQueue.add(message);
    }

    public boolean stop(int type){
        // 发布停止 ，但是还有订阅者，强行停止
        if(type == 1){
            int size = subscribers.size();
            log.info("jtt1078 flv stop publish mediaKey {}",mediaKey);
        }
        // 订阅停止
        if(type == 2){
            // 如果已经发布上流，不停止
            if(flvHeader.readableBytes() > 0){
                return false;
            }
            // 还有订阅者
            int size = subscribers.size();
            if(size > 0){
                return false;
            }
        }
        isStop = true;
        flvEncoder.close();
        return true;
    }

    private ByteBuf flvHeader = Unpooled.buffer();

    private ByteBuf firstVideoTag = Unpooled.buffer();

    private ByteBuf lastVideoITag = Unpooled.buffer();

    @Override
    public void run() {
        flvEncoder.onFlvData(data -> {
            // 第一个是flvHeader
            if(flvHeader.readableBytes() == 0){
                flvHeader.writeBytes(data);
                log.info("jtt1078 flv on flvHeader mediaKey {}",mediaKey);
                return;
            }
            // 第二个是firstVideoTag
            if(firstVideoTag.readableBytes() == 0){
                firstVideoTag.writeBytes(data);
                log.info("jtt1078 flv on firstVideoTag mediaKey {}",mediaKey);
                return;
            }
            // 缓存最后一个I帧 秒开
            if(data.getByte(0) == 9 && data.getByte(11) == 0x17){
                lastVideoITag.clear();
                lastVideoITag.writeBytes(data.copy());
                lastVideoITag.setMedium(4,0);
                lastVideoITag.setByte(7,0);
            }
            // 必须等待基础信息
            if(flvHeader.readableBytes() == 0 || firstVideoTag.readableBytes() == 0 || lastVideoITag.readableBytes() == 0){
                return;
            }
            if(!isStreamReady){
                ServerConfig config = ServerConfig.getInstance();
                log.info("jtt1078 flv publish http://{}:{}/video/{}.flv",config.getPublicIp(),config.getJtt808().getFlvPort(),mediaKey);
                isStreamReady = true;
                streamReadyCallback.forEach(Runnable::run);
            }
            for(FlvSubscriber subscriber : subscribers){
                Channel channel = subscriber.getChannel();
                if(!channel.isActive()){
                    continue;
                }
                // 先发送flvHeader 和 firstVideoTag
                if(!subscriber.isSendHeader()){
                    channel.writeAndFlush(flvHeader.copy())
                            .addListener((ChannelFutureListener) future -> {
                                if (!future.isSuccess()) {log.error("send flvHeader error",future.cause());}});
                    channel.writeAndFlush(firstVideoTag.copy())
                            .addListener((ChannelFutureListener) future -> {
                                if (!future.isSuccess()) {log.error("send firstVideoTag error",future.cause());}});
                    subscriber.setSendHeader(true);
                }
                // 重置时间戳， 每个播放者的时从0开始
                byte type = data.getByte(0);
                // 读取原来时间
                int low24Bits =  data.getUnsignedMedium(4);
                int high8Bits = data.getUnsignedByte(7) & 0xFF;
                long time =  ((long) (high8Bits << 24) | low24Bits) & 0xFFFFFFFFL;
                if(type == 9){
                    if(subscriber.getLastVideoTime() == 0){
                        subscriber.setLastVideoTime(time);
                    }
                    long cut = time - subscriber.getLastVideoTime();
                    subscriber.setLastVideoTime(time);
                    subscriber.setVideoTime(subscriber.getVideoTime() + cut);
                    // 重新设置时间
                    data.setMedium(4,(int) (subscriber.getVideoTime() & 0x00FFFFFF));
                    data.setByte(7,(int) ((subscriber.getVideoTime() >> 24) & 0xFF));
                }
                if(type == 8){
                    if(subscriber.getLastAudioTime() == 0){
                        subscriber.setLastAudioTime(time);
                    }
                    long cut = time - subscriber.getLastAudioTime();
                    subscriber.setLastAudioTime(time);
                    subscriber.setAudioTime(subscriber.getAudioTime() + cut);
                    // 重新设置时间
                    data.setMedium(4,(int) (subscriber.getAudioTime() & 0x00FFFFFF));
                    data.setByte(7,(int) ((subscriber.getAudioTime() >> 24) & 0xFF));
                }
                if(log.isTraceEnabled()){
                    log.trace("streamTask resetTime {} => {}",time,((long) ((data.getUnsignedByte(7) & 0xFF) << 24) |
                            data.getUnsignedMedium(4)) & 0xFFFFFFFFL);
                }
                channel.writeAndFlush(data.copy())
                        .addListener((ChannelFutureListener) future -> {
                            if (!future.isSuccess()) {log.error("send flv error",future.cause());}});
            }
        });

        // 某些设备上报的时间超过的32位，这里从0开始
        long lastVideoTime = 0;
        long videoTime = 0;
        long lastAudioTime = 0;
        long audioTime = 0;
        while (!isStop){
            try {
                Jtt1078RtpMessage message = messageQueue.take();
                int dataType = message.getDataType();
                // 视频
                if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02) {
                    // 重新计算时间
                    if(lastVideoTime == 0){
                        lastVideoTime = message.getTimestamp();
                    }
                    long cut = message.getTimestamp() - lastVideoTime;
                    lastVideoTime = message.getTimestamp();
                    videoTime = videoTime+ cut;
                    flvEncoder.addH264(videoTime, message.getPayload());
                }
                // 音频
                if (dataType == 0x03) {
                    // 重新计算时间
                    if(lastAudioTime == 0){
                        lastAudioTime = message.getTimestamp();
                    }
                    long cut = message.getTimestamp() - lastAudioTime;
                    lastAudioTime = message.getTimestamp();
                    audioTime = audioTime+ cut;

                    if(message.getPt() == 6){
                        flvEncoder.addG711a(audioTime, message.getPayload());
                    }
                    else if(message.getPt() == 26){
                        flvEncoder.addAdpcm(audioTime, message.getPayload());
                    }
                    else if(message.getPt() == 19){

                    }
                    else {
                        if(!isStreamReady){
                            log.error("jtt1078 unknown pt {} streamKey {}",message.getPt(),mediaKey);
                        }
                    }
                }
            }catch (Exception ex){
                log.error("jtt1078StreamManger StreamTask err",ex);
            }
        }
    }
}
