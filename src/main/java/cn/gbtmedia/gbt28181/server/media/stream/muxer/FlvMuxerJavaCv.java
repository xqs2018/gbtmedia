package cn.gbtmedia.gbt28181.server.media.stream.muxer;

import cn.gbtmedia.common.util.AudioUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import java.io.ByteArrayOutputStream;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
public class FlvMuxerJavaCv {

    private Consumer<ByteBuf> flvConsumer;

    public void onFlvData(Consumer<ByteBuf> consumer) {
        this.flvConsumer = consumer;
        init();
    }

    private FFmpegFrameRecorder flvRecorder;

    private ByteArrayOutputStream flvOutputStream;

    public void init() {
        try {
            // 设置日志回调
            FFmpegLogCallback.set();
            // 设置FFmpeg日志级别为DEBUG
            avutil.av_log_set_level(avutil.AV_LOG_TRACE);

            flvOutputStream = new ByteArrayOutputStream();
            flvRecorder = new FFmpegFrameRecorder(flvOutputStream, 1920, 1080);

            // 配置FFmpegFrameRecorder
            flvRecorder.setFormat("flv");

            // 视频配置
            flvRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            flvRecorder.setFrameRate(25);
            flvRecorder.setVideoBitrate(1000000); // 1Mbps
            flvRecorder.setVideoQuality(0);
            flvRecorder.setVideoOption("preset", "ultrafast");
            flvRecorder.setVideoOption("tune", "zerolatency");
            flvRecorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

            // 音频配置
            flvRecorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            flvRecorder.setAudioChannels(1);
            flvRecorder.setSampleRate(8000);
            flvRecorder.setAudioBitrate(16000);
            flvRecorder.setAudioQuality(0);

            // 创建并设置 AVFormatContext
            AVFormatContext formatContext = avformat.avformat_alloc_context();
            if (formatContext == null) {
                throw new RuntimeException("Failed to allocate format context");
            }

            // 设置输出格式为FLV
            formatContext.oformat(avformat.av_guess_format("flv", null, null));
            if (formatContext.oformat() == null) {
                throw new RuntimeException("Failed to set output format");
            }

            // 添加视频流
            AVStream video = avformat.avformat_new_stream(formatContext, null);
            if (video == null) {
                throw new RuntimeException("Failed to create video stream");
            }
            video.codecpar().codec_id(avcodec.AV_CODEC_ID_H264);
            video.codecpar().channels(0);
            video.codecpar().bit_rate(1000000); // 1Mbps
            video.time_base().num(1);
            video.time_base().den(25);

            // 添加音频流
            AVStream audioStream = avformat.avformat_new_stream(formatContext, null);
            if (audioStream == null) {
                throw new RuntimeException("Failed to create audio stream");
            }
            audioStream.codecpar().codec_id(avcodec.AV_CODEC_ID_AAC);
            audioStream.codecpar().channels(1);
            audioStream.codecpar().sample_rate(8000);
            audioStream.codecpar().bit_rate(16000);
            audioStream.time_base().num(1);
            audioStream.time_base().den(8000);

            flvRecorder.start(formatContext);

            // 发送header
            byte[] flvHeader = flvOutputStream.toByteArray();
            log.info("send flvHeader length {}",flvHeader.length);
            flvConsumer.accept(Unpooled.wrappedBuffer(flvHeader));
            flvOutputStream.reset();
        } catch (Exception ex) {
            log.error("init flvMuxerJavaCv error", ex);
            throw new RuntimeException("init flvMuxerJavaCv error", ex);
        }
    }

    private final ByteBuf h264Buf = Unpooled.buffer(1024 * 100);

    public void addH264(long timestamp, byte[] h264) {

    }

    private void recorderVideo(long timestamp, byte[] nalu) {
        try {
            AVPacket pkt = new AVPacket();
            pkt.data(new BytePointer(nalu));
            pkt.size(nalu.length);
            pkt.dts(timestamp);
            pkt.pts(timestamp);
            pkt.stream_index(0);
            pkt.flags(nalu[4] == 0x65 ? avcodec.AV_PKT_FLAG_KEY : 0); // 设置关键帧标志
            flvRecorder.recordPacket(pkt);
            // 发送视频帧
            byte[] videoTag = flvOutputStream.toByteArray();
            log.info("send nalu length {} videoTag length {}", nalu.length, videoTag.length);
            flvConsumer.accept(Unpooled.wrappedBuffer(videoTag));
            flvOutputStream.reset();
        } catch (Exception ex) {
            log.error("recorderVideo error", ex);
        }
    }

    public void addG711a(long timestamp, byte[] g711a) {
        // 先转成pcm
        byte[] pcm = AudioUtil.g711aToPcm(g711a);
        // 录制音频帧
        recorderAudio(timestamp, pcm);
    }

    private void recorderAudio(long timestamp, byte[] pcm) {
        try {
            short[] pcmData  = new short[pcm.length / 2];
            for (int i = 0; i < pcmData .length; i++) {
                pcmData [i] = (short) ((pcm[2 * i + 1] << 8) | (pcm[2 * i] & 0xFF));
            }
            Frame frame = new Frame();
            frame.sampleRate = 8000;
            frame.audioChannels = 1;
            frame.samples = new ShortBuffer[] {ShortBuffer.wrap(pcmData)};
            frame.timestamp = timestamp;
            flvRecorder.record(frame);
            // 发送音频帧
            byte[] audioTag = flvOutputStream.toByteArray();
            log.info("send pcm length {} audioTag length {}",pcm.length,audioTag.length);
            flvConsumer.accept(Unpooled.wrappedBuffer(audioTag));
            flvOutputStream.reset();
        }catch (Exception ex){
            log.error("recorderAudio error",ex);
        }
    }

    public void close() {
    }

    public void addH265(long timestamp, byte[] mediaData) {
    }
}
