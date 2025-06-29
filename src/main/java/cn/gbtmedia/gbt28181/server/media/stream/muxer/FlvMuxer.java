package cn.gbtmedia.gbt28181.server.media.stream.muxer;

import cn.gbtmedia.common.util.AudioUtil;
import cn.gbtmedia.common.util.ByteUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.global.avformat;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_open;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avformat.avio_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avio_open_dyn_buf;
import static org.bytedeco.ffmpeg.global.avformat.avio_close_dyn_buf;

/**
 * @author xqs
 */
@Slf4j
public class FlvMuxer {
    private final ByteBuf h264Buf = Unpooled.buffer(1024 * 100);
    private AVFormatContext fmt_ctx;
    private AVStream stream;
    private ByteBuf SPS = null;
    private ByteBuf PPS = null;
    private int count = 0;
    private boolean isInitialized = false;
    private long lastDts = -1;
    private Consumer<ByteBuf> flvDataConsumer;
    private ByteBuf flvHeader = null;
    private ByteBuf configPacket = null;
    private AVStream audioStream;
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNELS = 1;
    private static final int AAC_BITRATE = 16000;
    private static final int AUDIO_FRAME_SIZE = 1024;
    private AVCodecContext audioCodecCtx;
    private AVFrame audioFrame;

    static {
        // 设置FFmpeg日志回调
        avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
    }

    /**
     * 设置FLV数据消费者
     */
    public void onFlvData(Consumer<ByteBuf> consumer) {
        this.flvDataConsumer = consumer;
    }

    /**
     * 关闭编码器
     */
    public void close() {
       log.trace("关闭编码器...");
        if (fmt_ctx != null) {
            try {
                if (isInitialized) {
                    av_write_trailer(fmt_ctx);
                }
                if (fmt_ctx.pb() != null) {
                    avio_closep(fmt_ctx.pb());
                }
                avformat_close_input(fmt_ctx);
                avformat_free_context(fmt_ctx);
                fmt_ctx = null;
                lastDts = -1;
                isInitialized = false;
            } catch (Exception e) {
                log.error("关闭编码器失败", e);
            }
        }
        if (SPS != null) {
            SPS.release();
            SPS = null;
        }
        if (PPS != null) {
            PPS.release();
            PPS = null;
        }
        if (flvHeader != null) {
            flvHeader.release();
            flvHeader = null;
        }
        if (configPacket != null) {
            configPacket.release();
            configPacket = null;
        }
        if (audioCodecCtx != null) {
            avcodec.avcodec_free_context(audioCodecCtx);
            audioCodecCtx = null;
        }
        if (audioFrame != null) {
            avutil.av_frame_free(audioFrame);
            audioFrame = null;
        }
        h264Buf.release();
    }

    /**
     * h264转成mp4
     */
    public void addH264(long timestamp, byte[] h264) {
        if (h264 == null || h264.length == 0) {
            return;
        }

        try {
            h264Buf.writeBytes(h264);
            while (true) {
                byte[] nalu = null;
                for (int i = 0; i < h264Buf.readableBytes() - 3; i++) {
                    int a = h264Buf.getByte(i + 0) & 0xff;
                    int b = h264Buf.getByte(i + 1) & 0xff;
                    int c = h264Buf.getByte(i + 2) & 0xff;
                    int d = h264Buf.getByte(i + 3) & 0xff;
                    if ((a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01) || (a == 0x00 && b == 0x00 && c == 0x01 && d == 0x65)) {
                        if (i == 0) {
                            continue;
                        }
                        byte[] nalu1 = new byte[i];
                        h264Buf.readBytes(nalu1);
                        h264Buf.discardReadBytes();
                        if (nalu1.length > 3 && nalu1[3] == 0x65) {
                            byte[] newNalu = new byte[nalu1.length + 1];
                            newNalu[0] = 0;
                            System.arraycopy(nalu1, 0, newNalu, 1, newNalu.length - 1);
                            nalu1 = newNalu;
                        }
                        nalu = nalu1;
                        break;
                    }
                }
                if (nalu == null) {
                    break;
                }
                if (nalu.length < 4) {
                    continue;
                }
                // 去掉开头四个字节的标记
//                int newLength = nalu.length - 4;
//                byte[] newArray = new byte[newLength];
//                System.arraycopy(nalu, 4, newArray, 0, newLength);
//                nalu = newArray;
                // 创建VideoTag
                createVideoTag(timestamp, nalu);
            }
        } catch (Exception e) {
            log.error("处理H264数据失败", e);
        }
    }

    /**
     * 创建VideoTag
     */
    private void createVideoTag(long timestamp, byte[] nalu) {
        if (nalu == null ) {
            return;
        }

        try {
            int naluType = nalu[4] & 0x1f;
            // 跳过辅助增强信息 SEI
            if (naluType == 0x06) {
                return;
            }

            // 序列参数集
            if (SPS == null && naluType == 0x07) {
                SPS = Unpooled.wrappedBuffer(nalu);
            }
            // 图像参数集
            if (PPS == null && naluType == 0x08) {
                PPS = Unpooled.wrappedBuffer(nalu);
            }

            if (!isInitialized && SPS != null && PPS != null) {
                initializeEncoder();
            }

            if (!isInitialized || fmt_ctx == null) {
                return;
            }
           if (naluType != 7 && naluType != 8) {
               writeFrame(timestamp, nalu, naluType);
            }
        } catch (Exception e) {
            log.error("创建VideoTag失败", e);
        }
    }

    private void initializeEncoder() {
        try {
            fmt_ctx = avformat_alloc_context();
            if (fmt_ctx == null) {
                log.error("无法分配FormatContext");
                return;
            }

            avformat_network_init();

            // 创建输出上下文，使用FLV格式
            int ret = avformat_alloc_output_context2(fmt_ctx, null, "flv", null);
            if (ret < 0) {
                log.error("无法创建输出上下文, 错误码: {}", ret);
                return;
            }

            if (fmt_ctx.isNull()) {
                log.error("FormatContext为空");
                return;
            }

            // 创建视频流
            stream = avformat_new_stream(fmt_ctx, null);
            if (stream == null) {
                log.error("无法创建视频流");
                return;
            }

            // 设置视频编码器参数
            AVCodecParameters vcodecpar = stream.codecpar();
            vcodecpar.codec_type(AVMEDIA_TYPE_VIDEO);
            vcodecpar.codec_id(avcodec.AV_CODEC_ID_H264);
            vcodecpar.width(640);
            vcodecpar.height(480);

            // 设置SPS和PPS
            if (SPS != null && PPS != null) {
                CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
                compositeByteBuf.addComponents(true, SPS, PPS);
                byte[] bytes = ByteBufUtil.getBytes(compositeByteBuf);
                vcodecpar.extradata_size(bytes.length);
                vcodecpar.extradata(new BytePointer(bytes));
            }

            // 创建音频流
            audioStream = avformat_new_stream(fmt_ctx, null);
            if (audioStream == null) {
                log.error("无法创建音频流");
                return;
            }

            // 设置音频编码器参数
            AVCodecParameters acodecpar = audioStream.codecpar();
            acodecpar.codec_type(avutil.AVMEDIA_TYPE_AUDIO);
            acodecpar.codec_id(avcodec.AV_CODEC_ID_AAC);
            acodecpar.sample_rate(SAMPLE_RATE);
            acodecpar.channels(CHANNELS);
            acodecpar.bit_rate(AAC_BITRATE);
            acodecpar.format(avutil.AV_SAMPLE_FMT_FLTP);  // 设置采样格式为FLTP
            acodecpar.channel_layout(avutil.AV_CH_LAYOUT_MONO);  // 设置声道布局为单声道

            // 创建动态缓冲区
            PointerPointer<AVIOContext> pb = new PointerPointer<>(1);
            ret = avio_open_dyn_buf(pb);
            if (ret < 0) {
                log.error("无法创建动态缓冲区, 错误码: {}", ret);
                return;
            }
            fmt_ctx.pb(pb.get(AVIOContext.class));

            // 写入文件头
           log.trace("开始写入FLV头...");
            av_dump_format(fmt_ctx, 0, "flv", 1);
            ret = avformat_write_header(fmt_ctx, (PointerPointer<Pointer>) null);
            if (ret < 0) {
                log.error("写入文件头失败, 错误码: {}", ret);
                avio_closep(fmt_ctx.pb());
                return;
            }
           log.trace("FLV头写入完成，返回值: {}", ret);

            // 获取FLV头
            if (flvDataConsumer != null) {
                BytePointer buffer = new BytePointer();
                int size = avio_close_dyn_buf(fmt_ctx.pb(), buffer);
                if (size > 0) {
                    byte[] data = buffer.limit(size).getStringBytes();
                   log.trace("hexDump {}" , ByteBufUtil.hexDump(data));
                    flvDataConsumer.accept(Unpooled.wrappedBuffer(data));
                } else {
                    log.warn("FLV头大小为0，可能未正确生成");
                }
            }

            isInitialized = true;
           log.trace("编码器初始化成功");
        } catch (Exception e) {
            log.error("初始化编码器失败", e);
            close();
        }
    }

    private void writeFrame(long timestamp, byte[] nalu, int naluType) {
        AVPacket pkt = null;
        try {
           log.trace("[Video] 开始处理视频帧 - 类型: {}, 时间戳: {}", naluType, timestamp);

            // 重新创建动态缓冲区
            PointerPointer<AVIOContext> pb = new PointerPointer<>(1);
            int ret = avio_open_dyn_buf(pb);
            if (ret < 0) {
                log.error("[Video] 创建动态缓冲区失败, 错误码: {}", ret);
                return;
            }
            fmt_ctx.pb(pb.get(AVIOContext.class));

            pkt = av_packet_alloc();
            if (pkt.isNull()) {
                log.error("[Video] 分配Packet失败");
                return;
            }

            pkt.data(new BytePointer(nalu));
            pkt.size(nalu.length);
            pkt.stream_index(stream.index());

            // 处理时间戳
            long dts = timestamp;
            if (lastDts >= 0 && dts <= lastDts) {
                dts = lastDts + 1;  // 确保DTS单调递增
            }
            lastDts = dts;

            // 设置PTS和DTS
            pkt.pts(dts);
            pkt.dts(dts);

            if (naluType == 5) {
                pkt.flags(pkt.flags() | avcodec.AV_PKT_FLAG_KEY);
               log.trace("[Video] 关键帧处理完成");
            }

            // 使用交错写入
            ret = avformat.av_interleaved_write_frame(fmt_ctx, pkt);
            if (ret < 0) {
                log.error("[Video] 写入帧失败, 错误码: {}", ret);
            } else {
                // 获取写入的数据
                if (flvDataConsumer != null && fmt_ctx.pb() != null) {
                    BytePointer buffer = new BytePointer();
                    int size = avio_close_dyn_buf(fmt_ctx.pb(), buffer);
                    if (size > 0) {
                        ByteBuf data = Unpooled.wrappedBuffer(buffer.limit(size).getStringBytes());
                        flvDataConsumer.accept(data);
                       log.trace("[Video] 帧数据发送成功 - 大小: {}, 帧序号: {}, PTS: {}, DTS: {}", size, count, dts, dts);
                    }
                }
                count++;
            }
        } catch (Exception e) {
            log.error("[Video] 处理视频帧时发生异常", e);
        } finally {
            if (pkt != null) {
                av_packet_free(pkt);
            }
        }
    }

    public void addH265(long timestamp, byte[] h265) {

    }

    public void addG711a(long timestamp, byte[] g711a) {
        if (g711a == null || g711a.length == 0) {
            return;
        }

        try {
            if (!isInitialized) {
                initializeEncoder();
            }

            if (!isInitialized || fmt_ctx == null || audioStream == null) {
                log.error("编码器未正确初始化");
                return;
            }

            // 将G711A转换为PCM
            byte[] pcm = AudioUtil.g711aToPcm(g711a);
            testPcmList(Unpooled.wrappedBuffer(pcm));

            // 将PCM转换为AAC
            byte[] aac = pcmToAac(pcm);
            if(aac != null){
                testAccList(Unpooled.wrappedBuffer(aac));
            }

            if (aac != null && aac.length > 0) {
                writeAudioFrame(timestamp, aac);
            }
        } catch (Exception e) {
            log.error("处理G711A数据失败", e);
        }
    }

    static List<byte[]> testPcmList =new ArrayList<>();
    private static void testPcmList(ByteBuf data) {
        testPcmList.add(ByteBufUtil.getBytes(data));
        if (testPcmList.size() == 100) {
            try {
                byte[] bytes = ByteUtil.mergeByte(testPcmList);
                FileUtil.del("/var/test-pcm484666.pcm");
                File file = new File("/var/test-pcm484666" + ".pcm");
                file.createNewFile();
                IoUtil.write(new FileOutputStream(file), true, bytes);
                log.trace("************写入测试pcm {} ****************", file.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    static List<byte[]> testAccList = new ArrayList<>();
    private static void testAccList(ByteBuf data) {
        // 为独立的AAC文件添加ADTS头
        byte[] aacData = ByteBufUtil.getBytes(data);
        byte[] adtsHeader = new byte[7];
        // 同步字 0xFFF
        adtsHeader[0] = (byte) 0xFF;
        adtsHeader[1] = (byte) 0xF0;  // 修改为F0，表示ADTS

        // 采样率索引 (8000Hz = 11)
        // 声道配置 (1 = 单声道)
        adtsHeader[2] = (byte) 0x4C;  // 0100 1100: 采样率索引(11) + 声道配置(1)

        // 帧长度 (13位)
        int frameLength = aacData.length + 7;  // 加上ADTS头的长度
        adtsHeader[3] = (byte) ((frameLength >> 11) & 0x03);
        adtsHeader[4] = (byte) ((frameLength >> 3) & 0xFF);
        adtsHeader[5] = (byte) ((frameLength << 5) & 0xE0);

        // 帧数
        adtsHeader[5] |= 0x1C;  // 设置帧数

        // CRC校验
        adtsHeader[6] = 0x00;

        // 合并ADTS头和AAC数据
        byte[] result = new byte[adtsHeader.length + aacData.length];
        System.arraycopy(adtsHeader, 0, result, 0, adtsHeader.length);
        System.arraycopy(aacData, 0, result, adtsHeader.length, aacData.length);

        testAccList.add(result);
        if (testAccList.size() == 100) {
            try {
                byte[] bytes = ByteUtil.mergeByte(testAccList);
                FileUtil.del("/var/test-aac484666.aac");
                File file = new File("/var/test-aac484666.aac");
                file.createNewFile();
                IoUtil.write(new FileOutputStream(file), true, bytes);
                log.trace("************写入测试aac {} ****************", file.getAbsolutePath());
            } catch (Exception ex) {
                log.error("写入AAC文件失败", ex);
            }
        }
    }

    private byte[] pcmToAac(byte[] pcm) {
        try {
            log.trace("[Audio] 开始PCM转AAC处理 - 输入数据大小: {}", pcm.length);

            if (audioCodecCtx == null) {
                log.trace("[Audio] 初始化AAC编码器...");
                // 获取AAC编码器
                AVCodec codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_AAC);
                if (codec == null) {
                    log.error("[Audio] 找不到AAC编码器");
                    return null;
                }
                log.trace("[Audio] 找到AAC编码器: {}", codec.name().getString());

                // 创建编码器上下文
                audioCodecCtx = avcodec.avcodec_alloc_context3(codec);
                if (audioCodecCtx == null) {
                    log.error("[Audio] 分配编码器上下文失败");
                    return null;
                }

                // 设置基本参数
                log.trace("[Audio] 配置编码器参数 - 采样率: {}, 声道数: {}, 比特率: {}", SAMPLE_RATE, CHANNELS, AAC_BITRATE);
                audioCodecCtx.sample_fmt(avutil.AV_SAMPLE_FMT_FLTP);
                audioCodecCtx.channels(CHANNELS);
                audioCodecCtx.sample_rate(SAMPLE_RATE);
                audioCodecCtx.bit_rate(AAC_BITRATE);
                audioCodecCtx.channel_layout(avutil.AV_CH_LAYOUT_MONO);

                // 设置AAC编码器特定参数
                audioCodecCtx.flags(audioCodecCtx.flags() | avcodec.AV_CODEC_FLAG_GLOBAL_HEADER);
                audioCodecCtx.frame_size(AUDIO_FRAME_SIZE);

                // 打开编码器
                int ret = avcodec.avcodec_open2(audioCodecCtx, codec, (PointerPointer<Pointer>) null);
                if (ret < 0) {
                    log.error("[Audio] 打开AAC编码器失败, 错误码: {}", ret);
                    return null;
                }
                log.trace("[Audio] AAC编码器初始化完成");
            }

            // 创建输入帧
            log.trace("[Audio] 创建音频输入帧...");
            AVFrame frame = avutil.av_frame_alloc();
            if (frame == null) {
                log.error("[Audio] 分配输入帧失败");
                return null;
            }

            try {
                // 设置帧参数
                frame.nb_samples(AUDIO_FRAME_SIZE);
                frame.channels(CHANNELS);
                frame.format(avutil.AV_SAMPLE_FMT_FLTP);
                frame.sample_rate(SAMPLE_RATE);
                frame.channel_layout(avutil.AV_CH_LAYOUT_MONO);

                // 分配帧缓冲区
                int ret = avutil.av_frame_get_buffer(frame, 0);
                if (ret < 0) {
                    log.error("[Audio] 分配输入帧缓冲区失败, 错误码: {}", ret);
                    return null;
                }

                // 创建重采样上下文
                SwrContext swr = swresample.swr_alloc();
                if (swr == null) {
                    log.error("[Audio] 分配重采样上下文失败");
                    return null;
                }

                try {
                    // 设置重采样参数
                    swresample.swr_alloc_set_opts(swr,
                            avutil.AV_CH_LAYOUT_MONO,
                            avutil.AV_SAMPLE_FMT_FLTP,
                            SAMPLE_RATE,
                            avutil.AV_CH_LAYOUT_MONO,
                            avutil.AV_SAMPLE_FMT_S16,
                            SAMPLE_RATE,
                            0, null);

                    // 初始化重采样
                    ret = swresample.swr_init(swr);
                    if (ret < 0) {
                        log.error("[Audio] 初始化重采样失败, 错误码: {}", ret);
                        return null;
                    }

                    // 重采样
                    log.trace("[Audio] 开始音频重采样...");
                    PointerPointer<BytePointer> srcData = new PointerPointer<>(1);
                    srcData.put(0, new BytePointer(pcm));
                    PointerPointer<BytePointer> dstData = new PointerPointer<>(frame.data());
                    int[] srcLinesize = new int[1];
                    srcLinesize[0] = pcm.length;

                    ret = swresample.swr_convert(swr, dstData, AUDIO_FRAME_SIZE, srcData, pcm.length / 2);
                    if (ret < 0) {
                        log.error("[Audio] 重采样失败, 错误码: {}", ret);
                        return null;
                    }
                    log.trace("[Audio] 重采样完成 - 输出样本数: {}", ret);

                    // 分配输出包
                    AVPacket pkt = avcodec.av_packet_alloc();
                    if (pkt == null) {
                        log.error("[Audio] 分配输出包失败");
                        return null;
                    }

                    try {
                        // 发送帧
                        ret = avcodec.avcodec_send_frame(audioCodecCtx, frame);
                        if (ret < 0) {
                            log.error("[Audio] 发送音频帧失败, 错误码: {}", ret);
                            return null;
                        }

                        // 接收编码包
                        ret = avcodec.avcodec_receive_packet(audioCodecCtx, pkt);
                        if (ret < 0) {
                            log.error("[Audio] 接收编码包失败, 错误码: {}", ret);
                            return null;
                        }

                        // 复制编码后的数据
                        if (pkt.size() > 0 && pkt.data() != null && !pkt.data().isNull()) {
                            byte[] aacData = new byte[pkt.size()];
                            pkt.data().get(aacData);
                            log.trace("[Audio] PCM转AAC完成 - 输出数据大小: {}", aacData.length);
                            return aacData;
                        } else {
                            log.error("[Audio] 编码后的数据无效");
                            return null;
                        }
                    } finally {
                        avcodec.av_packet_free(pkt);
                    }
                } finally {
                    swresample.swr_free(swr);
                }
            } finally {
                avutil.av_frame_free(frame);
            }
        } catch (Exception e) {
            log.error("[Audio] PCM转AAC过程中发生异常", e);
        }
        return null;
    }

    private void writeAudioFrame(long timestamp, byte[] audioData) {
        AVPacket pkt = null;
        try {
           log.trace("[Audio] 开始写入音频帧 - 时间戳: {}, 数据大小: {}", timestamp, audioData.length);

            // 重新创建动态缓冲区
            PointerPointer<AVIOContext> pb = new PointerPointer<>(1);
            int ret = avio_open_dyn_buf(pb);
            if (ret < 0) {
                log.error("[Audio] 创建动态缓冲区失败, 错误码: {}", ret);
                return;
            }
            fmt_ctx.pb(pb.get(AVIOContext.class));

            pkt = av_packet_alloc();
            if (pkt.isNull()) {
                log.error("[Audio] 分配Packet失败");
                return;
            }

            // 设置音频包参数
            pkt.data(new BytePointer(audioData));
            pkt.size(audioData.length);
            pkt.stream_index(audioStream.index());
            pkt.pts(timestamp);
            pkt.dts(timestamp);
            pkt.duration(0);
            pkt.flags(0);

            // 使用交错写入
            ret = avformat.av_interleaved_write_frame(fmt_ctx, pkt);
            if (ret < 0) {
                log.error("[Audio] 写入音频帧失败, 错误码: {}", ret);
            } else {
                // 获取写入的数据
                if (flvDataConsumer != null && fmt_ctx.pb() != null) {
                    BytePointer buffer = new BytePointer();
                    int size = avio_close_dyn_buf(fmt_ctx.pb(), buffer);
                    if (size > 0) {
                        ByteBuf data = Unpooled.wrappedBuffer(buffer.limit(size).getStringBytes());
                        flvDataConsumer.accept(data);
                        log.trace("[Audio] 音频帧数据发送成功 - 大小: {}, 时间戳: {}", size, timestamp);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Audio] 写入音频帧时发生异常", e);
        } finally {
            if (pkt != null) {
                av_packet_free(pkt);
            }
        }
    }
}
