package cn.gbtmedia.gbt28181.server.media.stream.muxer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.File;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
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

/**
 * @author xqs
 */
@Slf4j
public class Mp4Muxer {
    private final ByteBuf h264Buf = Unpooled.buffer(1024 * 100);
    private AVFormatContext fmt_ctx;
    private AVStream stream;
    private ByteBuf SPS = null;
    private ByteBuf PPS = null;
    private int count = 0;
    private boolean isInitialized = false;
    private long lastDts = -1;

    static {
        // 设置FFmpeg日志回调
        avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
    }

    /**
     * 关闭编码器
     */
    public void close() {
        log.info("关闭编码器...");
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
        if (nalu == null || nalu.length < 5) {
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

            if (count++ < 95) {
                writeFrame(timestamp, nalu, naluType);
            } else {
                close();
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

            String outputPath =  "/var/cc.mp4";
            File outputFile = new File(outputPath);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            if (outputFile.exists()) {
                outputFile.delete();
            }

            // 创建输出上下文
            int ret = avformat_alloc_output_context2(fmt_ctx, null, "mp4", outputPath);
            if (ret < 0) {
                log.error("无法创建输出上下文, 错误码: {}", ret);
                return;
            }

            if (fmt_ctx.isNull()) {
                log.error("FormatContext为空");
                return;
            }

            // 创建新的流
            stream = avformat_new_stream(fmt_ctx, null);
            if (stream == null) {
                log.error("无法创建新的流");
                return;
            }

            // 设置编码器参数
            AVCodecParameters ccodecpar = stream.codecpar();
            ccodecpar.codec_type(AVMEDIA_TYPE_VIDEO);
            ccodecpar.codec_id(avcodec.AV_CODEC_ID_H264);
            ccodecpar.width(640);
            ccodecpar.height(480);

            // 设置SPS和PPS
            CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
            compositeByteBuf.addComponents(true, SPS, PPS);
            byte[] bytes = ByteBufUtil.getBytes(compositeByteBuf);
            ccodecpar.extradata_size(bytes.length);
            ccodecpar.extradata(new BytePointer(bytes));

            // 打开输出文件
            String absolutePath = outputFile.getAbsolutePath();
            AVIOContext pb = new AVIOContext();
            ret = avio_open(pb, absolutePath, AVIO_FLAG_WRITE);
            if (ret < 0) {
                log.error("无法打开输出文件: {}, 错误码: {}", absolutePath, ret);
                return;
            }
            fmt_ctx.pb(pb);

            // 写入文件头
            ret = avformat_write_header(fmt_ctx, (PointerPointer<Pointer>) null);
            if (ret < 0) {
                log.error("写入文件头失败, 错误码: {}", ret);
                avio_closep(fmt_ctx.pb());
                return;
            }

            isInitialized = true;
            log.info("编码器初始化成功");
        } catch (Exception e) {
            log.error("初始化编码器失败", e);
            close();
        }
    }

    private void writeFrame(long timestamp, byte[] nalu, int naluType) {
        AVPacket pkt = null;
        try {
            pkt = av_packet_alloc();
            if (pkt.isNull()) {
                log.error("无法分配Packet");
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
            }

            int ret = av_write_frame(fmt_ctx, pkt);
            if (ret < 0) {
                log.error("写入帧失败, 错误码: {}", ret);
            } else {
                log.info("写入帧 {}, pts: {}, dts: {}", count, dts, dts);
            }
        } catch (Exception e) {
            log.error("写入帧失败", e);
        } finally {
            if (pkt != null) {
                av_packet_free(pkt);
            }
        }
    }
}
