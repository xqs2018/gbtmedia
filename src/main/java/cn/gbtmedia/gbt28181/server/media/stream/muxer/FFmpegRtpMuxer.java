package cn.gbtmedia.gbt28181.server.media.stream.muxer;

import cn.gbtmedia.common.extra.FFmpegExec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
public class FFmpegRtpMuxer {

    private static final ExecutorService FFMPEG_MUXER_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-ffmpeg-muxer-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("ffmpeg muxer pool ex t {}", t, e))
                            .factory());

    private Consumer<ByteBuf> flvConsumer;
    public void onFlvData(Consumer<ByteBuf> consumer) {
        this.flvConsumer = consumer;
    }

    private ServerSocket rtpServer;
    private Socket rptClient;
    private OutputStream rtpOutputStream;

    private Process ffmpegProcess;
    private volatile boolean isRunning = true;
    private volatile boolean isInitialized;

    private synchronized void init() {
        if (isInitialized) {
            return;
        }
        try {
            // 写入rtp流
            rtpServer = new ServerSocket(0);
            rtpServer.setSoTimeout(30000);
            int rtpPort = rtpServer.getLocalPort();
            FFMPEG_MUXER_POOL.execute(()->{
                try {
                    rptClient = rtpServer.accept();
                    rtpOutputStream = rptClient.getOutputStream();
                    log.info("rtpServer client connected from: {}", rptClient.getInetAddress());
                    rtpServer.close();
                } catch (Exception ex) {
                    log.error("Failed to accept videoServer connection", ex);
                }
            });

            // FFmpeg 参数
            FFmpegExec fmpegExec = FFmpegExec.getInstance();
            String ffmpeg = fmpegExec.getFfmpegPath();
            String ffmpegCmd = String.format(
                "%s " +
                "-loglevel trace "+
                "-i tcp://127.0.0.1:%d " +
                "-c:v copy " +
                "-c:a aac " +
                "-f flv " +
                "-flvflags no_duration_filesize " +
                "-",
                ffmpeg, rtpPort
            );

            log.info("Starting FFmpeg with command: {}", ffmpegCmd);
            ffmpegProcess = Runtime.getRuntime().exec(ffmpegCmd);

            // 读取合并流
            FFMPEG_MUXER_POOL.execute(() -> {
                try (InputStream inputStream = ffmpegProcess.getInputStream()) {
                     readMuxerInput(inputStream);
                } catch (Exception ex) {
                    log.error("Error reading FFmpeg stdout", ex);
                    close();
                }
            });

            // 读取日志
            FFMPEG_MUXER_POOL.execute(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("FFmpeg stderr: {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading FFmpeg stderr", e);
                }
            });

            // 启动ffmpge进程
            FFMPEG_MUXER_POOL.execute(() -> {
                try {
                    int exitCode = ffmpegProcess.waitFor();
                    log.info("FFmpeg process exited with code: {}", exitCode);
                    if (exitCode != 0) {
                        close();
                    }
                } catch (InterruptedException e) {
                    log.error("FFmpeg process interrupted", e);
                    close();
                }
            });

            isInitialized = true;
        } catch (Exception ex) {
            log.error("Failed to initialize FFmpeg muxer", ex);
            close();
        }
    }

    private void readMuxerInput(InputStream inputStream) {
        try {
            // 读取flv头 9 + 4(前一个tag大小)
            byte[] header = new byte[ 9 + 4];
            int headerRead = 0;
            while (headerRead < 9 + 4) {
                int read = inputStream.read(header, headerRead, 9 + 4 - headerRead);
                if (read == -1) {
                    log.error("Failed to read FLV header");
                    return;
                }
                headerRead += read;
            }
            // 验证flv信息
            if (header[0] != 'F' || header[1] != 'L' || header[2] != 'V' || header[3] != 1) {
                log.error("Invalid FLV header: {}", java.util.Arrays.toString(header));
                return;
            }
            int version = header[3] & 0xFF;
            int flags = header[4] & 0xFF;
            boolean hasAudio = (flags & 0x04) != 0;
            boolean hasVideo = (flags & 0x01) != 0;
            log.info("FLV Header: version={}, hasAudio={}, hasVideo={}", version, hasAudio, hasVideo);
            // 消费头
            flvConsumer.accept(Unpooled.wrappedBuffer(header));
            // 循环读取tag
            while (isRunning) {
                // tag  11 + 数据体
                byte[] tagHeader = new byte[11];
                int headerBytesRead = 0;
                while (headerBytesRead < 11) {
                    int read = inputStream.read(tagHeader, headerBytesRead, 11 - headerBytesRead);
                    if (read == -1) {
                        log.info("End of FLV stream");
                        return;
                    }
                    headerBytesRead += read;
                }
                // tag信息
                int tagType = tagHeader[0] & 0xFF;
                int dataSize = ((tagHeader[1] & 0xFF) << 16) |
                             ((tagHeader[2] & 0xFF) << 8) |
                             (tagHeader[3] & 0xFF);
                int timestamp = ((tagHeader[4] & 0xFF) << 16) |
                              ((tagHeader[5] & 0xFF) << 8) |
                              (tagHeader[6] & 0xFF) |
                              ((tagHeader[7] & 0xFF) << 24);
                log.trace("FLV Tag type={}, size={} bytes, timestamp={}", tagType, dataSize, timestamp);

                // tag数据 + 4(前一个头大小)
                byte[] tagData = new byte[dataSize +4];
                int dataBytesRead = 0;
                while (dataBytesRead < dataSize +4) {
                    int read = inputStream.read(tagData, dataBytesRead, dataSize +4 - dataBytesRead);
                    if (read == -1) {
                        log.info("End of FLV Tag");
                        return;
                    }
                    dataBytesRead += read;
                }
                ByteBuf tagBuffer = Unpooled.wrappedBuffer(tagHeader,tagData);
                // 消费tag
                flvConsumer.accept(tagBuffer);
            }
        } catch (Exception e) {
            log.error("Failed to accept FLV connection", e);
        }
    }

    public void close() {
        isRunning = false;
        if (rtpOutputStream != null) {
            try {
                rtpOutputStream.close();
            } catch (IOException e) {
                log.error("rtpOutputStream close", e);
            }
        }
        if (rptClient != null && !rptClient.isClosed()) {
            try {
                rptClient.close();
            } catch (IOException e) {
                log.error("rptClient close", e);
            }
        }
        if (rtpServer != null && !rtpServer.isClosed()) {
            try {
                rtpServer.close();
            } catch (IOException e) {
                log.error("videoServer close", e);
            }
        }
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            try (OutputStream stdin = ffmpegProcess.getOutputStream()) {
                stdin.write('q');
                stdin.flush();
                if (!ffmpegProcess.waitFor(5, TimeUnit.SECONDS)) {
                    ffmpegProcess.destroyForcibly();
                    log.warn("FFmpeg destroyForcibly");
                }
            } catch (Exception ex) {
                log.error("close ffmpegProcess ex", ex);
            }
        }
        log.info("FFmpeg muxer closed");
    }

    public void addRtp(long timestamp, byte[] rtp) {
        try {
            init();
            while (rtpOutputStream == null){
                Thread.sleep(100);
            }
            rtpOutputStream.write((short)rtp.length);
            rtpOutputStream.write(rtp);
            rtpOutputStream.flush();
        } catch (Exception e) {
            log.error("Failed to write rtp data", e);
        }
    }

    public void addH264(long timestamp, byte[] h264) {

    }

    public void addG711a(long timestamp, byte[] g711a) {

    }

    public void addH265(long timestamp, byte[] h265) {

    }
}
