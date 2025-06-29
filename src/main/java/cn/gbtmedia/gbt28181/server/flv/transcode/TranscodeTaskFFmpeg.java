package cn.gbtmedia.gbt28181.server.flv.transcode;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.core.io.FileUtil;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import cn.gbtmedia.common.extra.FFmpegExec;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TranscodeTaskFFmpeg extends TranscodeTask{

    private Process ffmpegProcess;

    private ServerSocket serverSocket;

    @Override
    protected synchronized void doStop() {
        if(ffmpegProcess != null){
            ffmpegProcess.destroy();
        }
        if(serverSocket != null){
            try {
                serverSocket.close();
            } catch (Exception e) {
                log.error("close serverSocket ex",e);
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        log.info("start transcodeFFmpeg pullFlvUrl {} transcode {}",pullFlvUrl,transcode);
        // https://github.com/kokorin/Jaffree 可参考
        FFmpegResult ffmpegResult = FFmpeg.atPath()
                .addInput(UrlInput.fromUrl(pullFlvUrl))
                .addOutput(PipeOutput.pumpTo(flvData))
                .execute();
        String ffmpeg = FFmpegExec.getInstance().getFfmpegPath();
        // ffmpeg流输出到本地socket 输出到管道
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        String pushUlr = "tcp://127.0.0.1:" + serverSocket.getLocalPort();
        // 字体路径
        String fontPath = ServerConfig.getInstance().getFontPath();
        // 拼接ffmpeg命令
        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        // 日志等级
//        command.add("-v");
//        command.add("warning");
        command.add("-i");
        command.add(pullFlvUrl);
        command.add("-acodec");
        command.add("copy");
        command.add("-vf");
        StringBuilder filter = new StringBuilder();
        // 分辨率在水印前面
        if (imageWidth > 0 && imageHeight > 0) {
            filter.append("scale=").append(imageWidth).append(":").append(imageHeight);
            filter.append(",");
        }
        if(FileUtil.isFile(fontPath)){
            filter.append("drawtext=text='").append(drawText).append("':x=w-tw-10:y=h-th-10:");
            filter.append("fontsize=24:fontcolor=white:shadowy=2:fontfile=").append(fontPath);
        }else {
            log.warn("fontfile is null {}",fontPath);
        }
        command.add(filter.toString());
        command.add("-f");
        command.add("flv");
        command.add(pushUlr);

        // 启动新线程运行ffmpeg子进程
        new Thread(()->{
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                log.info("exec ffmpeg command {}",processBuilder.command());
                ffmpegProcess = processBuilder.start();
                // 分别打印进程输入和输出
                new Thread(() -> {
                    BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
                    try {
                        while (!isStop) {
                            String line = in.readLine();
                            if (line != null) {
                                log.trace(line);
                            }
                        }
                    } catch (Exception ex) {
                       log.error("ffmpeg ex",ex);
                    }
                },"ffmpeg-" + ssrc + "-" + transcode + "-input").start();
                new Thread(() -> {
                    BufferedReader err = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));
                    try {
                        while (!isStop) {
                            String line = err.readLine();
                            if (line!=null) {
                                log.trace(line);
                            }
                        }
                    } catch (Exception ex) {
                        log.error("ffmpeg ex",ex);
                    }
                },"ffmpeg-" + ssrc + "-" + transcode + "-error").start();
                ffmpegProcess.waitFor();
            } catch (Exception ex) {
                log.error("ffmpeg process ex",ex);
            }
            stop();
            log.info("ffmpeg process thread exit");
        },"ffmpeg-" + ssrc + "-" + transcode).start();

        // 获取转码后的数据
        try (Socket client = serverSocket.accept();
             DataInputStream input = new DataInputStream(client.getInputStream())) {
             log.info("accept ffmpeg process connection {}", client.getInetAddress());
             byte[] buffer = new byte[1024];
             int len = 0;
             while (!isStop) {
                 synchronized (this){
                     len = input.read(buffer);
                     if (len == -1) {
                         break;
                     }
                     flvData.write(buffer, 0, len);
                     if (flvHeader == null) {
                         flvHeader = flvData.toByteArray();
                         flvData.reset();
                         continue;
                     }
                     // 帧数据
                     byte[] data = flvData.toByteArray();
                     flvData.reset();
                     receiveFlvData(data);
                 }
            }
        } catch (Exception ex) {
            if("Socket closed".equals(ex.getMessage())){
                log.info("Socket closed");
            }else {
                log.error("read ffmpeg process data ex",ex);
            }
        }finally {
            stop();
        }
    }
}
