package cn.gbtmedia.test;

import cn.gbtmedia.common.util.AudioUtil;
import de.sciss.jump3r.lowlevel.LameEncoder;
import de.sciss.jump3r.mp3.Lame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;

/**
 * @author xqs
 */
@Slf4j
public class FlvEncoderJavaCv {

    private Consumer<ByteBuf> flvDataConsumer;

    /**
     * 消费flvData
     */
    public void onFlvData(Consumer<ByteBuf> consumer){
        flvDataConsumer = consumer;
    }

    /**
     * 关闭
     */
    public void close(){
        closeMp3Encoder();
        closeAacEncoder();
    }

    private ByteBuf flvHeader = null;

    /**
     * 创建FlvHeader
     */
    private void createFlvHeader(){
        flvHeader = Unpooled.buffer();
        flvHeader.writeByte((byte)'F');
        flvHeader.writeByte((byte)'L');
        flvHeader.writeByte((byte)'V');
        // 版本固定0x01
        byte version = 0x01;
        flvHeader.writeByte(version);
        boolean haveVide = true;
        boolean haveAudio = true;
        // 前5位0 第6位表示是否存在音频 第7位0 第8位表示是否存在视频  0b00000101
        flvHeader.writeByte((byte)(0x00 | (haveVide ? 0x01 : 0x00) | (haveAudio ? 0x04 : 0x00)));
        // header的长度固定0x09
        int dataOffset = 0x09;
        flvHeader.writeInt(dataOffset);
        // 前一个tag长度， 前一个为0
        flvHeader.writeInt(0);
        // 发送消费
        flvDataConsumer.accept(flvHeader);
    }

    private final ByteBuf h264Buf = Unpooled.buffer(1024 * 100);

    /**
     * h264转成flv
     */
    public void addH264(long timestamp, byte[] h264){
        //  先生成一个 flvHeader
        if(flvHeader == null){
            createFlvHeader();
        }
        h264Buf.writeBytes(h264);
        while (true) {
            byte[] nalu = null;
            for (int i = 0; i < h264Buf.readableBytes() - 3; i++) {
                int a = h264Buf.getByte(i + 0) & 0xff;
                int b = h264Buf.getByte(i + 1) & 0xff;
                int c = h264Buf.getByte(i + 2) & 0xff;
                int d = h264Buf.getByte(i + 3) & 0xff;
                if ((a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01)||(a == 0x00 && b == 0x00 && c == 0x01 && d == 0x65)) {
                    if (i == 0) {
                        continue;
                    }
                    byte[] nalu1 = new byte[i];
                    h264Buf.readBytes(nalu1);
                    h264Buf.discardReadBytes(); //重置已经读取的
                    if(nalu1.length > 3 && nalu1[3] == 0x65){
                        // 前面补 00 00 01 65 => 完整格式 00 00 00 01 65
                        byte[] newNalu = new byte[nalu1.length+1];
                        newNalu[0] = 0;
                        System.arraycopy(nalu1, 0, newNalu, 1, newNalu.length - 1);
                        nalu1 = newNalu;
                    }
                    nalu =  nalu1;
                    break;
                }
            }
            if (nalu == null) {
                break;
            }
            if (nalu.length < 4){
                continue;
            }
            // 去掉开头四个字节的标记
            int newLength = nalu.length - 4;
            byte[] newArray = new byte[newLength];
            System.arraycopy(nalu, 4, newArray, 0, newLength);
            nalu = newArray;
            // 创建VideoTag
            createVideoTag1(timestamp, nalu);
        }
    }

    //***************************************************************************
    //*******************************  flvRecorder *******************************
    PipedOutputStream pos = new PipedOutputStream();
    private FFmpegFrameRecorder flvRecorder;
    private ByteArrayOutputStream flvOutputStream;
    FFmpegFrameGrabber grabber;
    volatile boolean ischeav;
    private void cheav () {
        if(ischeav){
            return;
        }
        ischeav = true;
        new Thread(()->{
            try {
                log.info("flvRecorder ............");
                 grabber = new FFmpegFrameGrabber(new PipedInputStream(pos));
                 log.info("grabber ...........");
                grabber.start();

                flvOutputStream = new ByteArrayOutputStream();
                flvRecorder = new FFmpegFrameRecorder(flvOutputStream, 1280,720,0);
                flvRecorder.setFormat("flv");
                flvRecorder.setFrameRate(25);
                flvRecorder.setInterleaved(true);
                flvRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);


                flvRecorder.start(grabber.getFormatContext());

                byte[] flvHeader = flvOutputStream.toByteArray();
                flvDataConsumer.accept(Unpooled.wrappedBuffer(flvHeader));

                flvOutputStream.reset();


                AVPacket avPacket;

                while( (avPacket=grabber.grabPacket()) != null && avPacket.size() >0 && avPacket.data() != null){
                    avPacket.pts(mPtsQueue.pop());
                    avPacket.dts(avPacket.dts());
                    flvRecorder.recordPacket(avPacket);
                    log.info("recordPacket ...");
                    byte[] flvData = flvOutputStream.toByteArray();
                    flvOutputStream.reset();
                    flvDataConsumer.accept(Unpooled.wrappedBuffer(flvData));
                }


            } catch (Exception ex) {
                log.error("createAacEncoder ex",ex);
            }
        },"ff").start();
    }

    private ConcurrentLinkedDeque<Long> mPtsQueue = new ConcurrentLinkedDeque<>();

    private void closeFlvRecorder(){
        if(flvRecorder != null){
            try {
                flvRecorder.stop();
                flvOutputStream.close();
            } catch (Exception ex) {
                log.error("closeAacEncoder ex",ex);
            }
        }
    }

    private void createVideoTag1(long timestamp, byte[] nalu){
        cheav();
        if(grabber == null){
            return;
        }
        mPtsQueue.add(timestamp);
        try {
            pos.write(nalu);
        } catch (IOException e) {
           log.error(" pos.write e",e);
        }
        if(true){
            return;
        }
        if(flvRecorder == null){
            return;
        }

        try {
            // 包装H.264数据为AVPacket
            AVPacket packet = new AVPacket();
            av_init_packet(packet);
            packet.data(new BytePointer(nalu));
            packet.size(nalu.length);
            packet.pts(timestamp); // 计算时间戳（微秒）
            packet.dts(packet.pts());   // 此处简化为dts=pts，实际需按GOP调整
            packet.stream_index(0);
            if ((nalu[0] & 0x1f) == 0x05) {
                packet.flags(avcodec.AV_PKT_FLAG_KEY);
            }

            flvRecorder.recordPacket(packet);
        }catch (Exception ex){
            log.error("createVideoTag1 ex",ex);
        }
        byte[] flvData = flvOutputStream.toByteArray();
        if (flvData.length == 0) {
            return;
        }
        flvOutputStream.reset();
        flvDataConsumer.accept(Unpooled.wrappedBuffer(flvData));
    }

    static {
        // 设置FFmpeg日志级别和回调
        avutil.av_log_set_level(avutil.AV_LOG_DEBUG); // 日志级别：DEBUG

    }

    //****************************** h264 sps pps **************************
    private ByteBuf SPS = null;
    private ByteBuf PPS = null;
    private ByteBuf firstVideoTag = null;

    /**
     * 创建VideoTag
     */
    private void createVideoTag(long timestamp, byte[] nalu) {
        int naluType = nalu[0] & 0x1f;
        // 跳过辅助增强信息 SEI
        if (naluType == 0x06){
            return;
        }
        if (naluType == 0x01 || naluType == 0x02 || naluType == 0x03 || naluType == 0x04
                || naluType == 0x05 || naluType == 0x07 || naluType == 0x08);
        else{
            // 判断是否为I帧的算法为：（NALU类型 & 0001 1111） = 5 即 NALU类型 & 31 = 5，比如0x65 & 0x1f = 5
            // 0x67 (0 11 00111) SPS 非常重要 type = 7
            // 0x68 (0 11 01000) PPS 非常重要 type = 8
            // 0x65 (0 11 00101) IDR帧 关键帧 非常重要 type = 5
            // 0x61 (0 11 00001) I帧 重要 type=1 非IDR的I帧 不大常见
            // 0x41 (0 10 00001) P帧 重要 type = 1
            // 0x01 (0 00 00001) B帧 不重要 type = 1
            // 0x06 (0 00 00110) SEI 不重要 type = 6
            return;
        }
        // 序列参数集
        if (SPS == null && naluType == 0x07) {
            SPS = Unpooled.wrappedBuffer(nalu);
        }
        // 图像参数集
        if (PPS == null && naluType == 0x08) {
            PPS =  Unpooled.wrappedBuffer(nalu);
        }
        // 在 ISO/IEC 14496-15 中定义第一个 videoTag AVC Sequence Header 一般位于流的第一个Tag中。
        // 包含着是 H.264 解码相关比较重要的 SPS 和 PPS 信息
        if (SPS != null && PPS != null && firstVideoTag == null) {
            firstVideoTag = Unpooled.buffer();

            // 本tag data部分大小
            int dataSize = 1 + 1 + 3 + 6 + 2 + SPS.readableBytes()  + 1 + 2 + PPS.readableBytes();

            //************ tagHeader(固定11字节) ********************
            // tag类型 0x09固定视频
            firstVideoTag.writeByte(0x09);
            // 本tag data部分大小 3字节
            firstVideoTag.writeMedium(dataSize);
            // tag时间戳 3字节
            firstVideoTag.writeMedium(0);
            // tag时间戳扩展 前3个字节不够扩展这个一个字节
            firstVideoTag.writeByte(0);
            // 流id固定0 3字节
            firstVideoTag.writeMedium(0);

            //************ tagData ************************
            // 帧类型和编码id 对应的帧类型是 关键帧（H.264 的 IDR 帧），编码方式是 AVC (H.264)。
            firstVideoTag.writeByte(0x17);
            // AVC包类型 AVCPacketType  0 AVC sequence header
            firstVideoTag.writeByte(0);
            // 相对时间戳 CompositionTime 如果AVCPacketType=0x01，为相对时间戳，其它均为0；
            firstVideoTag.writeMedium(0);

            //************** AVC Sequence Header ***************
            // configurationVersion 版本固定0x01
            firstVideoTag.writeByte(0x01);
            /**
             *  SPS[0] nalu_header：
             *  SPS[1] profile_idc：
             *  SPS[2] constraint_set_flags：
             *  SPS[3] level_idc：
             */
            // AVCProfileIndication 表示 H.264 编码的 Profile（配置文件）
            firstVideoTag.writeByte(SPS.getByte(1));
            // profile_compatibility 表示 H.264 配置文件的兼容性
            firstVideoTag.writeByte(SPS.getByte(2));
            // AVCLevelIndication 表示 H.264 编码的 Level（级别）
            firstVideoTag.writeByte(SPS.getByte(3));
            // reserved(1111 11 ) + lengthSizeMinusOne ( 一般为 11 )
            firstVideoTag.writeByte((byte)0xff);
            // reserved(111 ) + numOfSequenceParameterSets ( sps个数一般为 00001 )
            firstVideoTag.writeByte((byte)0xe1);
            // sequenceParameterSetLength  spsSize(16bit)
            firstVideoTag.writeShort((short)(SPS.readableBytes()));
            // sequenceParameterSetNALUnit sps数据
            firstVideoTag.writeBytes(SPS.copy());
            // numOfPictureParameterSets (pps个数一般为 1)
            firstVideoTag.writeByte((byte)0x01);
            // pictureParameterSetLength  ppsSize(16bit)
            firstVideoTag.writeShort((short)(PPS.readableBytes()));
            // pictureParameterSetNALUnit pps数据
            firstVideoTag.writeBytes(PPS.copy());

            int prevTagSize = 11 + dataSize;
            // 前一个tag长度
            firstVideoTag.writeInt(prevTagSize);

            flvDataConsumer.accept(firstVideoTag);
        }
        if (firstVideoTag == null){
            return;
        }
        // 后续 videoTag
        ByteBuf videoTag  = Unpooled.buffer();
        if (naluType != 7 && naluType != 8) {
            // 本tag data部分大小
            int dataSize = 1 + 1 + 3 + 4 + nalu.length;

            //************ tagHeader(固定11字节) ********************
            // tag类型 0x09固定视频
            videoTag.writeByte(0x09);
            // 本tag data部分大小 3字节
            videoTag.writeMedium(dataSize);
            // tag时间戳 3字节
            videoTag.writeMedium((int) (timestamp & 0x00FFFFFF));
            // tag时间戳扩展 前3个字节不够扩展这个一个字节
            videoTag.writeByte((int) ((timestamp >> 24) & 0xFF));
            // 流id固定0 3字节
            videoTag.writeMedium(0);

            //************ tagData ********************
            // 帧类型和编码id
            if (naluType == 5) {
                // 对应的帧类型是 关键帧（H.264 的 IDR 帧），编码方式是 AVC (H.264)。
                videoTag.writeByte(0x17);
            } else{
                // 对应的帧类型是 非关键帧， 编码方式是 AVC (H.264)。
                videoTag.writeByte(0x27);
            }
            // AVC包类型 AVCPacketType  1 AVC NALU
            videoTag.writeByte(0x01);
            // 相对时间戳 CompositionTime 如果AVCPacketType=0x01，为相对时间戳，其它均为0；
            videoTag.writeMedium(0);
            //  NALU Len
            videoTag.writeInt(nalu.length);
            // NALU Data
            videoTag.writeBytes(nalu);

            int prevTagSize = 11 + dataSize;
            // 前一个tag长度
            videoTag.writeInt(prevTagSize);

            flvDataConsumer.accept(videoTag);
        }
    }

    /**
     * g711a转成flv
     */
    public void addG711a(long timestamp, byte[] g711a) {
        //  先生成一个 flvHeader
        if(flvHeader == null){
            createFlvHeader();
        }
        // 先转成pcm
        byte[] pcm = AudioUtil.g711aToPcm(g711a);
        // pmc转成flv
        createAudioTag(timestamp, pcm);
    }


    /**
     * 2 mp3  10  aac 7 g711a 1 adpmc
     */
    private static final int audio_format = 7;

    /**
     * pmc转成flv
     */
    private void createAudioTag(long timestamp, byte[] pcm){
        if(audio_format == 2){
            createAudioTagMp3(timestamp, pcm);
        }else if(audio_format == 10){
            createAudioTagAac(timestamp, pcm);
        }else if(audio_format == 7){
            createAudioTagG711a(timestamp, pcm);
        }else if(audio_format == 1){
            createAudioTagAdpmc(timestamp, pcm);
        }
    }

    //***************************************************************************
    //*******************************  mp3Encoder *******************************

    private LameEncoder mp3Encoder;
    private byte[]  mp3Buffer;
    private ByteArrayOutputStream mp3Output;
    private void createMp3Encoder(){
        AudioFormat PCM_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 1 * 2, -1, false);
        mp3Encoder =  new LameEncoder(PCM_FORMAT, 256, 3, Lame.MEDIUM, false);
        mp3Buffer = new byte[mp3Encoder.getPCMBufferSize()];
        mp3Output = new ByteArrayOutputStream(mp3Encoder.getOutputBufferSize());
    }
    private void closeMp3Encoder(){
        if(mp3Encoder != null){
            mp3Encoder.close();
        }
    }

    private void createAudioTagMp3(long timestamp, byte[] pcm){
        if(mp3Encoder == null){
            createMp3Encoder();
        }
        // 转成mp3
        if (pcm.length == 0) {
            return;
        }
        int bytesToTransfer = Math.min(mp3Encoder.getPCMBufferSize(), pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;
        mp3Output.reset();
        while (0 < (bytesWritten = mp3Encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, mp3Buffer))){
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(mp3Buffer.length, pcm.length - currentPcmPosition);
            mp3Output.write(mp3Buffer, 0, bytesWritten);
        }
        byte[] mp3Data = mp3Output.toByteArray();

        // 转成flv
        ByteBuf audioTag = Unpooled.buffer();
        //************ tagHeader(固定11字节) ******************
        // 本tag data部分大小
        int dataSize = mp3Data.length + 1;

        audioTag.writeByte(0x08);
        // 本tag data部分大小 3字节
        audioTag.writeMedium(dataSize);
        // tag时间戳 3字节
        audioTag.writeMedium((int) (timestamp & 0x00FFFFFF));
        // tag时间戳扩展 前3个字节不够扩展这个一个字节
        audioTag.writeByte((int) ((timestamp >> 24) & 0xFF));
        // 流id固定0 3字节
        audioTag.writeMedium(0);

        //*********************  tagData ********************
        byte format = 2; // 音频格式 2 = mp3
        byte rate = 0;   // 采样率 0 = 5.5-kHz
        byte size = 1;   // 采用精度 1 = snd16Bit
        byte type = 0;   // 音频声道 0 = sndMono 单声道
        byte formatAndRateAndSize = (byte) ( format  << 4 | rate << 2 | size << 1 | type );
        audioTag.writeByte(formatAndRateAndSize);
        // 音频数据
        audioTag.writeBytes(mp3Data);

        int prevTagSize = 11 + dataSize;
        // 前一个tag长度
        audioTag.writeInt(prevTagSize);

        // 先等待第一个VideoTag发出去
        if(firstVideoTag == null){
            return;
        }
        flvDataConsumer.accept(audioTag);
    }

    //***************************************************************************
    //*******************************  aacEncoder *******************************

    private FFmpegFrameRecorder pcmAacrecorder;
    private ByteArrayOutputStream aacOutputStream;
    private void createAacEncoder(){
        try {
            aacOutputStream = new ByteArrayOutputStream();
            pcmAacrecorder = new FFmpegFrameRecorder(aacOutputStream, 0);
            pcmAacrecorder.setAudioChannels(1);
            pcmAacrecorder.setSampleRate(8000);
            pcmAacrecorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            pcmAacrecorder.setAudioBitrate(16000);
            pcmAacrecorder.setFormat("adts");
            pcmAacrecorder.start();
        } catch (Exception ex) {
            log.error("createAacEncoder ex",ex);
        }
    }
    private void closeAacEncoder(){
        if(pcmAacrecorder != null){
            try {
                pcmAacrecorder.stop();
                aacOutputStream.close();
            } catch (Exception ex) {
                log.error("closeAacEncoder ex",ex);
            }
        }
    }

    private ByteBuf firstAudioTag;

    private void createAudioTagAac(long timestamp, byte[] pcm) {
        if(pcmAacrecorder == null){
            createAacEncoder();
        }
        // 转成aac
        if (pcm.length == 0) {
            return;
        }
        short[] shortArray = new short[pcm.length / 2];
        for (int i = 0; i < shortArray.length; i++) {
            shortArray[i] = (short) ((pcm[2 * i + 1] << 8) | (pcm[2 * i] & 0xFF));
        }
        Frame frame = new Frame();
        frame.samples = new ShortBuffer[] {ShortBuffer.wrap(shortArray)};
        try {
            pcmAacrecorder.record(frame);
        }catch (Exception ex){
            log.error("createAudioTagAac ex",ex);
        }
        byte[] aacData = aacOutputStream.toByteArray();
        if (aacData.length == 0) {
            return;
        }
        aacOutputStream.reset();
        if(firstAudioTag == null){
            // aacSequenceHeader
            // 音频对象类型为 2（即 AAC LC），采样率索引为 11（对应 8000Hz），声道配置为 1（单声道）
             aacData = new byte[]{(byte) 0x15, (byte) 0x81};
        }
        // 转成flv
        ByteBuf audioTag = Unpooled.buffer();
        //************ tagHeader(固定11字节) ******************
        // 本tag data部分大小
        int dataSize = aacData.length + 1 + 1; // aac多一个aac_packet_type

        audioTag.writeByte(0x08);
        // 本tag data部分大小 3字节
        audioTag.writeMedium(dataSize);
        // tag时间戳 3字节
        audioTag.writeMedium((int) (timestamp & 0x00FFFFFF));
        // tag时间戳扩展 前3个字节不够扩展这个一个字节
        audioTag.writeByte((int) ((timestamp >> 24) & 0xFF));
        // 流id固定0 3字节
        audioTag.writeMedium(0);

        // 序列头时间戳为0
        if(firstAudioTag == null){
            audioTag.setMedium(4,0);
            audioTag.setByte(7,0);
        }

        //*********************  tagData ********************
        byte format = 10; // 音频格式 10 = acc
        byte rate = 3;   // 采样率 0 = 5.5-kHz 3 = 44kHz
        byte size = 1;   // 采用精度 1 = snd16Bit
        byte type = 0;   // 音频声道 0 = sndMono 单声道
        byte formatAndRateAndSize = (byte) ( format  << 4 | rate << 2 | size << 1 | type );
        audioTag.writeByte(formatAndRateAndSize);
        if(firstAudioTag == null){
            audioTag.writeByte(0x00); // aac_packet_type = 0 (序列头)
        }else {
            audioTag.writeByte(0x01); // aac_packet_type = 1 (原始数据)
        }
        // 音频数据
        audioTag.writeBytes(aacData);

        int prevTagSize = 11 + dataSize;
        // 前一个tag长度
        audioTag.writeInt(prevTagSize);

        if(firstAudioTag == null){
            firstAudioTag = audioTag.copy();
            flvDataConsumer.accept(audioTag);
        }

        // 先等待第一个VideoTag发出去
        if(firstVideoTag == null){
            return;
        }
        flvDataConsumer.accept(audioTag);
    }

    private void createAudioTagG711a(long timestamp, byte[] pcm) {
        byte[] g711a = AudioUtil.pcmToG711a(pcm);
        // 转成g711a
        if (g711a.length == 0) {
            return;
        }
        // 转成flv
        ByteBuf audioTag = Unpooled.buffer();
        //************ tagHeader(固定11字节) ******************
        // 本tag data部分大小
        int dataSize = g711a.length + 1;

        audioTag.writeByte(0x08);
        // 本tag data部分大小 3字节
        audioTag.writeMedium(dataSize);
        // tag时间戳 3字节
        audioTag.writeMedium((int) (timestamp & 0x00FFFFFF));
        // tag时间戳扩展 前3个字节不够扩展这个一个字节
        audioTag.writeByte((int) ((timestamp >> 24) & 0xFF));
        // 流id固定0 3字节
        audioTag.writeMedium(0);

        //*********************  tagData ********************
        byte format = 7; // 音频格式 7 = g711a
        byte rate = 0;   // 采样率 0 = 5.5-kHz
        byte size = 1;   // 采用精度 1 = snd16Bit
        byte type = 0;   // 音频声道 0 = sndMono 单声道
        byte formatAndRateAndSize = (byte) ( format  << 4 | rate << 2 | size << 1 | type );
        audioTag.writeByte(formatAndRateAndSize);
        // 音频数据
        audioTag.writeBytes(g711a);

        int prevTagSize = 11 + dataSize;
        // 前一个tag长度
        audioTag.writeInt(prevTagSize);

        // 先等待第一个VideoTag发出去
        if(firstVideoTag == null){
            return;
        }
        flvDataConsumer.accept(audioTag);
    }


    private void createAudioTagAdpmc(long timestamp, byte[] pcm) {

    }

}
