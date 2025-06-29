package cn.gbtmedia.gbt28181.server.media.stream;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.gbt28181.server.flv.FlvSubscriber;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
@Data
public class StreamTask implements Runnable{

    private volatile boolean isStop;

    private volatile boolean isStreamReady;

    private final String ssrc;

    private FlvEncoder flvEncoder = new FlvEncoder();

    private ArrayBlockingQueue<RtpMessage> messageQueue = new ArrayBlockingQueue<>(1024 *10);

    private final List<FlvSubscriber> subscribers = new CopyOnWriteArrayList<>();

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

    public void pushRtpMessage(RtpMessage message){
        messageQueue.add(message);
    }

    public boolean stop(int type){
        // 发布停止 ，但是还有订阅者，强行停止
        if(type == 1){
            int size = subscribers.size();
            log.info("gbt28181 stream stop publish ssrc {}",ssrc);
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
        doRecord();
        subscribers.forEach(FlvSubscriber::close);
        return true;
    }

    // 重置设备上报的时间戳
    private long lastVideoTime = 0;
    private long videoTime = 0;
    private long lastAudioTime = 0;
    private long audioTime = 0;
    private long lastVideoPts = 0;

    /**
     * 时间戳生成方式
     * 0 源视频流绝对时间戳，不做任何改变
     * 1 接收数据时的系统时间戳 录像回放需要这个，支持倍速 flvjs不支持，jessibuca支持这么配置时间
     * 2 采用源视频流时间戳相对时间戳(增长量)
     */
    private int modifyStamp = 2;

    @Override
    public void run() {
        try {
            flvEncoder.onFlvData(consumerFlvData());
            // 播放减少缓存数据
            //  ffplay  -probesize 1024 http://172.22.31.22:10002/video/0102000001.flv
            while (!isStop){
                RtpMessage message = messageQueue.take();
                if(message.getPt() == 98){
                    // 重新计算时间
                    if(lastVideoTime == 0){
                        lastVideoTime = message.getTimestamp();
                    }
                    long cut = message.getTimestamp() - lastVideoTime;
                    lastVideoTime = message.getTimestamp();
                    videoTime = videoTime+ cut;

                    flvEncoder.addH264(message.getTimestamp()/90, message.getPayload());
                }
                else if(message.getPt() == 8){
                    // 重新计算时间
                    if(lastAudioTime == 0){
                        lastAudioTime = message.getTimestamp();
                    }
                    long cut = message.getTimestamp() - lastAudioTime;
                    lastAudioTime = message.getTimestamp();
                    audioTime = audioTime+ cut;

                    flvEncoder.addG711a(audioTime, message.getPayload());
                }
                // 解析国标ps流
                else if(message.getPt() == 96){
                    parsePS(message);
                }else {
                    if(!isStreamReady){
                        log.error("unknown pt {} ssrc {} seq {} timestamp {}",message.getPt(),message.getSsrc()
                                ,message.getSequenceNumber(),message.getTimestamp());
                    }
                }
            }

        }catch (Exception ex){
            log.error("streamTask err",ex);
        }
    }

    /**
     * 存放pes流 [PES header + ( h264 data or G711)] .... ( [PES header + ( h264 data or G711)]
     */
    private ByteBuf pesBuf = Unpooled.buffer(1024 * 100);

    /**
     * 解析国标ps流
     */
    private int psVideoType = 0x1b;
    private int psAudioType = 0x90;
    private volatile boolean psTypeLog;

    private void parsePS(RtpMessage message){
        byte[] payload = message.getPayload();
        // https://blog.csdn.net/fanyun_01/article/details/120537670
        // https://www.cnblogs.com/dong1/p/11051708.html
        // https://blog.yasking.org/a/hikvision-rtp-ps-stream-parser.html
        // 1. 首帧封装 RTP + PS header + PS system header + PS system Map + ( [PES header + h264 data(SPS)] + [PES header + h264 data(PPS)]) ....
        // 2. 非首帧封装 RTP + PS header + ( [PES header + ( h264 data or G711)] .... ( [PES header + ( h264 data or G711)] )
        // 4. 非首帧子包封装 RTP + 续2 ( [... PES header + ( h264 data or G711)]
        // PS header 开头 00 00 01 ba
        if(payload.length > 4 && payload[0] == 0x00 && payload[1] ==0x00 && payload[2] ==0x01 && (payload[3]&0xff) == 0xba){
            // 待提取的pes数据开始下标
            int pesHeaderStart = 0;

            // PS header[13] 代表后续数据长度标识
            int psHeaderLength =  payload[13] & 7;
            // PS header 开始 0  结束 13(前面数据个数) + 1(后续字段长度标识) + psHeaderLength(后续字段长度)
            int psHeaderEnd = 13 + 1 + psHeaderLength;
            byte[] psHeader = new byte[psHeaderEnd];
            System.arraycopy(payload,0,psHeader,0,psHeader.length);
            if(log.isTraceEnabled()){
                log.trace("psHeader hex  \n [{}]", ByteBufUtil.hexDump(psHeader));
            }

            // 1. 如果是下一个包是 00 00 01 bb 开头 则是首帧 PS system header
            if(payload[psHeaderEnd] == 0 && payload[psHeaderEnd +1] == 0 && payload[psHeaderEnd +2] == 01 && (payload[psHeaderEnd +3]&0xff) == 0xbb ) {

                // PS system header[4] [5] 代表后续数据长度标识
                int psSystemHeaderLength = ByteUtil.byte2ToInt(payload[psHeaderEnd + 4],payload[psHeaderEnd + 5]);
                // PS system header 开始 nextPackStart 结束 nextPackStart + 4(开始标记) + 2(后续字段长度标识)  + psSystemHeaderLength(后续字段长度)
                int psSystemHeaderEnd = psHeaderEnd + 4 + 2  + psSystemHeaderLength;
                byte[] psSystemHeader = new byte[psSystemHeaderEnd - psHeaderEnd];
                System.arraycopy(payload, psHeaderEnd,psSystemHeader,0,psSystemHeader.length);
                if(log.isTraceEnabled()){
                    log.trace("psSystemHeader hex  \n [{}]", ByteBufUtil.hexDump(psSystemHeader));
                }

                // PS system Map 包开始 00 00 00 bc
                // PS system Map[4] [5] 代表后续数据长度标识
                int psSystemMapLength = ByteUtil.byte2ToInt(payload[psSystemHeaderEnd + 4],payload[psSystemHeaderEnd + 5]);
                // PS system Map 开始 psSystemMapStart 结束 psSystemMapStart + 4(开始标记) + 2(后续字段长度标识)  + psMapHeaderLength(后续字段长度)
                int psSystemMapEnd = psSystemHeaderEnd + 4 + 2  + psSystemMapLength;
                byte[] psSystemMap = new byte[psSystemMapEnd - psSystemHeaderEnd];
                System.arraycopy(payload, psSystemHeaderEnd,psSystemMap,0,psSystemMap.length);
                if(log.isTraceEnabled()){
                    log.trace("psSystemMap hex  \n [{}]", ByteBufUtil.hexDump(psSystemMap));
                }
                // 读取视频和音频格式
                // https://blog.csdn.net/jctian000/article/details/80308977
                // https://zhuanlan.zhihu.com/p/595923734
                // https://github.com/use-go/ps-rtp-streams/tree/master/packet
                //  H.264 视频流： 0x1B；
                //  H.265 视频流： 0x24；
                //  G.711 音频流： 0x90；
                //  G.722.1 音频流： 0x92；
                //  G.723.1 音频流： 0x93；
                // 音视频格式
                // stream_type[1]  elementary_stream_id[1]  elementary_stream_info_length[2]
                // ...
                // stream_type[1]  elementary_stream_id[1]  elementary_stream_info_length[2]
                int readIndex = 12;
                psVideoType = 0x1b; // h264
                psAudioType = 0x90; // g711a
                while (readIndex + 4 < psSystemMap.length){
                    int stream_type = psSystemMap[readIndex] & 0xFF;
                    int elementary_stream_id = psSystemMap[++readIndex] & 0xFF;
                    if(log.isTraceEnabled()){
                        log.trace("stream_type {} element_stream_id {}",String.format("%02X ", stream_type),String.format("%02X ", elementary_stream_id));
                    }
                    if(elementary_stream_id == 0xe0){
                        psVideoType = stream_type;
                    }
                    if(elementary_stream_id == 0xc0){
                        psAudioType = stream_type;
                    }
                    int elementary_stream_info_length = ByteUtil.byte2ToInt(psSystemMap[++readIndex],psSystemMap[++readIndex]);
                    readIndex = readIndex + elementary_stream_info_length + 1;
                }
                if(!psTypeLog){
                    log.info("ssrc {} PS system Map videoType {} audioType {}",ssrc, Integer.toHexString(psVideoType), Integer.toHexString(psAudioType));
                    psTypeLog = true;
                }

                // 最后就是pes数据
                pesHeaderStart = psSystemMapEnd;
            }
            // 2. 非首帧 下一个包就是pes数据
            else{
                pesHeaderStart = psHeaderEnd;
            }
            // 提取出pesData  PES header + ( h264 data ) or (G711) ... PES header + ( h264 data ) or (G711)
            byte[] pesData = new byte[payload.length - pesHeaderStart];
            System.arraycopy(payload, pesHeaderStart,pesData,0,pesData.length);
            if(log.isTraceEnabled()){
                //log.trace("pesData hex  \n [{}]", ByteBufUtil.hexDump(pesData));
            }

            pesBuf.writeBytes(pesData);
        }
        // 3. 非首帧子包封装
        else {
            if(log.isTraceEnabled()){
                //log.trace("pesData sub hex \n [{}]", ByteBufUtil.hexDump(payload));
            }

            pesBuf.writeBytes(payload);
        }
        // 读取 pes流 [PES header + ( h264 data or G711)] .... ( [PES header + ( h264 data or G711)]
        // PES header 开头 00 00 01 e0/c0 e0是视频 c0是音频
        while (true) {
            byte[] chunk = null;
            for (int i = 0; i < pesBuf.readableBytes() - 3; i++) {
                int a = pesBuf.getByte(i + 0) & 0xff;
                int b = pesBuf.getByte(i + 1) & 0xff;
                int c = pesBuf.getByte(i + 2) & 0xff;
                int d = pesBuf.getByte(i + 3) & 0xff;
                if ((a == 0x00 && b == 0x00 && c == 0x01 && d == 0xe0)||(a == 0x00 && b == 0x00 && c == 0x01 && d == 0xc0)){
                    if (i == 0) {
                        continue;
                    }
                    chunk = new byte[i];
                    pesBuf.readBytes(chunk);
                    pesBuf.discardReadBytes();
                }
            }
            if (chunk == null) {
                break;
            }
            if (chunk.length < 4){
                continue;
            }
            // 码流类型，视频流或音频流
            int mediaType = chunk[3] & 0xff;
            // PES header基本信息长度为 9  [8]为附加信息长度
            int attachDataLength = chunk[8] & 0xFF;
            // 可能为0
            if(attachDataLength == 0){
                continue;
            }
            byte[] pesHeader = new byte[9 + attachDataLength];
            System.arraycopy(chunk, 0, pesHeader, 0, pesHeader.length);
            if(log.isTraceEnabled()){
                log.trace("pesHeader hex \n [{}]", ByteBufUtil.hexDump(pesHeader));
            }
            // 00 00 01 E0  // PES起始码（视频流）
            // 33 4B        // PES分组长度字段
            // 80           // 标志字段 PTS DTS标志字段
            // 80
            // 05            // 后续长度
            // 21 00 01 00 01  // 后续字段 PTS DTS
            int pts_dts_flags = (pesHeader[6] >> 6) & 0x03;
            if(pts_dts_flags == 0b10 || pts_dts_flags == 0b11){
                byte[] timePTS = new byte[5];
                System.arraycopy(pesHeader, 9, timePTS, 0, 5);
                if(pts_dts_flags == 0b10){
                    // 仅 PTS
                    long b0 = timePTS[0] & 0xFF;
                    long b1 = timePTS[1] & 0xFF;
                    long b2 = timePTS[2] & 0xFF;
                    long b3 = timePTS[3] & 0xFF;
                    long b4 = timePTS[4] & 0xFF;
                    long pts = ((b0 & 0x0E) << 29) |  // 取首字节的中间3位（0x0E掩码），左移29位
                                    (b1 << 22) |            // 第2字节的8位，左移22位
                                    ((b2 & 0xFE) << 14) |  // 第3字节的高7位（0xFE掩码），左移14位
                                    (b3 << 7) |            // 第4字节的8位，左移7位
                                    (b4 >> 1);              // 第5字节的高7位（右移1位）
                    if(log.isTraceEnabled()){
                        log.trace("pesHeader pts {}", pts);
                    }
                    lastVideoPts = pts;
                }
                if(pts_dts_flags == 0b11){
                    // PTS + DTS 再提取五个字节
                }
            }

            // 媒体数据开始
            int mediaStart = 9 + attachDataLength;
            byte[] mediaData = new byte[chunk.length - mediaStart];
            System.arraycopy(chunk, mediaStart, mediaData, 0, mediaData.length);
            if(log.isTraceEnabled()){
                log.trace("mediaType {} mediaLength {}", mediaType,mediaData.length);
            }

            if(mediaType == 0xe0){
                // 重新计算时间
                if(lastVideoTime == 0){
                    lastVideoTime = message.getTimestamp();
                }else {
                    if(lastVideoTime + 90 >= message.getTimestamp()){
                        if(log.isTraceEnabled()){
                            log.trace("fixTime sequenceNumber {} lastVideoTime {} >= rtpTimestamp {}",message.getSequenceNumber(),
                                    lastVideoTime,message.getTimestamp());
                            message.setTimestamp(lastVideoTime + 90 + 90);
                        }
                    }
                }
                long cut = message.getTimestamp() - lastVideoTime;
                lastVideoTime = message.getTimestamp();
                videoTime = videoTime+ cut;

                if(log.isTraceEnabled()){
                    log.trace("rtpTimestamp {} videoTime {} videoTimets {}", message.getTimestamp(),videoTime,videoTime/90);
                }

                long timestamp = videoTime/90;
                if(modifyStamp == 0){
                    timestamp = lastVideoPts;
                }
                if(modifyStamp == 1){
                    timestamp = System.currentTimeMillis();
                }

                if(psVideoType == 0x1b){
                    flvEncoder.addH264(timestamp, mediaData);
                }else if(psVideoType == 0x24){
                    flvEncoder.addH265(timestamp, mediaData);
                }
            }
            if(mediaType == 0xc0){
                // 重新计算时间
                if(lastAudioTime == 0){
                    lastAudioTime = message.getTimestamp();
                }else {
                    if(lastVideoTime + 90 >= message.getTimestamp()){
                        if(log.isTraceEnabled()){
                            log.trace("fixTime sequenceNumber {}  lastAudioTime {} >= rtpTimestamp {}",message.getSequenceNumber(),
                                    lastAudioTime,message.getTimestamp());
                            message.setTimestamp(lastAudioTime + 90 + 90);
                        }
                    }
                }
                long cut = message.getTimestamp() - lastAudioTime;
                lastAudioTime = message.getTimestamp();
                audioTime = audioTime+ cut;

                if(log.isTraceEnabled()){
                    log.trace("rtpTimestamp {} audioTime {}",message.getTimestamp(),audioTime);
                }

                long timestamp = audioTime/90;
                if(modifyStamp == 0){
                    timestamp =  message.getTimestamp();
                }
                if(modifyStamp == 1){
                    timestamp = System.currentTimeMillis();
                }

                flvEncoder.addG711a(timestamp, mediaData);
            }
        }
    }

    private ByteBuf flvHeader = Unpooled.buffer();

    private ByteBuf firstVideoTag = Unpooled.buffer();

    private ByteBuf firstAudioTag = Unpooled.buffer();

    private ByteBuf lastVideoITag = Unpooled.buffer();

    private Consumer<ByteBuf> consumerFlvData(){
        return data -> {
            // 判断是否需要录制 TODO 改成拉流录制mp4
            // checkRecord(data);
            // 缓存第一个flvHeader
            if(flvHeader.readableBytes() == 0 && data.getCharSequence(0,3, StandardCharsets.UTF_8).equals("FLV")){
                flvHeader.writeBytes(data);
                log.info("gbt28181 stream on flvHeader ssrc {}",ssrc);
                return;
            }
//            // 缓存第一个firstVideoTag 视频基本参数信息sps和pps
            if(firstVideoTag.readableBytes() == 0 && data.getByte(0) == 9){
                firstVideoTag.writeBytes(data);
                log.info("gbt28181 stream on firstVideoTag ssrc {}",ssrc);
                return;
            }
//            // 缓存第一个firstAudioTag  只有aac类型音频才有音频基本参数信息AudioSpecificConfig TODO 有些需要先发送
//            if(firstAudioTag.readableBytes() == 0 && data.getByte(0) == 8){
//                firstAudioTag.writeBytes(data);
//                log.info("gbt28181 stream on firstAudioTag ssrc {}",ssrc);
//                return;
//            }
            // 缓存最后一个I帧 秒开 0x17 h264  0x1C h265
            if(data.getByte(0) == 9 && (
                    (psVideoType == 0x1b && data.getByte(11) == 0x17)
                    ||
                    (psVideoType == 0x24 && data.getByte(11) == 0x1C))){
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
                log.info("gbt28181 stream publish http://{}:{}/video/{}.flv",config.getPublicIp(),config.getGbt28181().getFlvPort(),ssrc);
                isStreamReady = true;
                streamReadyCallback.forEach(Runnable::run);
            }
            // 发送数据给每个播放者
            for(FlvSubscriber subscriber : subscribers){
                // 先发送flvHeader 和 firstVideoTag firstAudioTag 和最后一个I帧
                if(!subscriber.isSendHeader()){
                    subscriber.sendData(flvHeader.copy());
                    subscriber.sendData(firstVideoTag.copy());
                    subscriber.sendData(lastVideoITag.copy());
                    subscriber.setSendHeader(true);
                }
                // 重置时间戳
                if(modifyStamp == 2){
                    resetTime(data,subscriber);
                }
                subscriber.sendData(data.copy());
            }
        };
    }

    /**
     * 每个订阅者的初始时间应是0
     */
    private void resetTime(ByteBuf data,FlvSubscriber subscriber){
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
            subscriber.setVideoTime(subscriber.getVideoTime() + (cut<=0?1:cut));
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
            subscriber.setAudioTime(subscriber.getAudioTime() + (cut<=0?1:cut));
            // 重新设置时间
            data.setMedium(4,(int) (subscriber.getAudioTime() & 0x00FFFFFF));
            data.setByte(7,(int) ((subscriber.getAudioTime() >> 24) & 0xFF));
        }
        if(log.isTraceEnabled()){
            log.trace("streamTask resetTime type {} {} => {}",type,time,((long) ((data.getUnsignedByte(7) & 0xFF) << 24) |
                    data.getUnsignedMedium(4)) & 0xFFFFFFFFL);
        }
    }


    private List<ByteBuf> recordCache = new ArrayList<>();

    private boolean recordEnable;

    private String recordPath;

    private long recordSecond;

    private boolean recordSlice;

    private String recordTempPath;

    private long recordFileSecond;

    private void checkRecord(ByteBuf data) {
        if(!recordEnable){
            return;
        }
        if(recordTempPath == null){
            String a = IdUtil.fastSimpleUUID();
            recordTempPath = recordPath + "/temp/"+ a + ".flv";
            FileUtil.del(recordTempPath);
            FileUtil.touch(recordTempPath);
        }
        recordCache.add(data.copy());
        if(recordCache.size() <= 100){
            return;
        }
        doRecord();
    }

    private void doRecord(){
        try {
            if(recordCache.isEmpty()){
                return;
            }
            byte[] bytes = ByteUtil.mergeByte(recordCache.stream().map(ByteBuf::array).toList());
            FileOutputStream fos = new FileOutputStream(recordTempPath, true);
            fos.write(bytes);
            fos.close();
            Long duration = recordCache.stream().map(b -> {
                if(b.getByte(0) != 9){
                    return 0L;
                }
                int low24Bits = b.getUnsignedMedium(4);
                int high8Bits = b.getUnsignedByte(7) & 0xFF;
                return ((long) (high8Bits << 24) | low24Bits) & 0xFFFFFFFFL;
            }).max(Comparator.comparing(v -> v)).orElse(0L);
            recordFileSecond = duration / 1000;
            log.trace("gbt28181 stream record ssrc {} second {}", ssrc, recordFileSecond);
            recordCache.clear();
            if(isStop){
                String recordStoragePath = recordTempPath.replace("/temp","");
                FileUtil.del(recordStoragePath);
                FileUtil.copy(recordTempPath, recordStoragePath,true);
                FileUtil.del(recordTempPath);
                log.info("gbt28181 stream record stop {}",recordStoragePath);
            }
        }catch (Exception ex){
            log.error("doRecord ex",ex);
        }
    }
}
