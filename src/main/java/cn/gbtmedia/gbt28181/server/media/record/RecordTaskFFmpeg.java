package cn.gbtmedia.gbt28181.server.media.record;

import cn.gbtmedia.common.extra.FFmpegExec;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
public class RecordTaskFFmpeg extends RecordTask{

    private Process ffmpegProcess;

    private ServerSocket recordProgressSocket;

    private String recordTempDir;

    private String recordTempFilePath;

    @Override
    public void doStart() {
        String httpFlv = recordParam.getPullUrl();
        log.info("recordTask pullUrl {}",httpFlv);
        String ssrc = recordParam.getSsrc();
        String recordPath = recordParam.getRecordPath();
        boolean recordSlice = recordParam.isRecordSlice();
        if(recordSlice){
            // 分片下只能整分或者整小时
            recordParam.setRecordSecond(recordParam.getRecordSecond() == 60 ? 60 : 3600);
        }
        long recordSecond = recordParam.getRecordSecond();
        recordTempDir = recordPath + "/temp";
        FileUtil.del(recordTempDir);
        FileUtil.mkdir(recordTempDir);

        //  FFmpeg 输出进度日志端口
        int recordProgressPort;
        try {
            recordProgressSocket = new ServerSocket(0);
            recordProgressPort = recordProgressSocket.getLocalPort();
        } catch (IOException e) {
            log.error("recordProgressSocket ex",e);
            throw new RuntimeException(e);
        }
        // FFmpeg 参数
        FFmpegExec fmpegExec = FFmpegExec.getInstance();
        String ffmpeg = fmpegExec.getFfmpegPath();
        String ffmpegCmd = null;

        // 普通录制
        if(!recordSlice){
            recordTempFilePath = recordTempDir + "/" + IdUtil.fastSimpleUUID() +".mp4";
            ffmpegCmd = String.format(
                    "%s " +
                            "-loglevel warning "+
                            // "-analyzeduration 100000 " +
                            "-i %s " +
                            "-progress tcp://127.0.0.1:%d " +
                            "-c:v copy " +
                            "-c:a aac " +
                            "-movflags frag_keyframe+empty_moov "+
                            "-f mp4 " +
                            "%s",
                    ffmpeg, httpFlv, recordProgressPort, recordTempFilePath
            );
        }
        // 分片录制
        else {
            // 计算初始时间偏移，固定只能 按分钟和小时生成
            int segmentInterval = (int) recordSecond;
            long currentEpoch = System.currentTimeMillis() / 1000;
            long nextSegmentEpoch = ((currentEpoch / segmentInterval) + 1) * segmentInterval;
            long initialOffset = nextSegmentEpoch - currentEpoch;
            recordTempFilePath = recordTempDir + "/" + IdUtil.fastSimpleUUID() + "_%Y%m%d%H%M%S.mp4";
            ffmpegCmd = String.format(
                    "%s " +
                            //"-hide_banner " +
                            "-loglevel warning " +
                            "-i %s " +
                            "-ss %d " +
                            "-progress tcp://127.0.0.1:%d " +
                            "-c:v copy " +
                            "-c:a aac " +
                            "-f segment " +
                            "-segment_time %d " +
                            //"-segment_clocktime_offset 1 " +
                            "-segment_format mp4 " +
                            "-movflags faststart+empty_moov " +
                            "-reset_timestamps 1 " +
                            "-strftime 1 " +
                            //"-fflags +genpts " +
                            //"-avoid_negative_ts make_zero " +
                            //"-use_wallclock_as_timestamps 1 " +
                            "%s",
                    ffmpeg, httpFlv,initialOffset, recordProgressPort,segmentInterval, recordTempFilePath
            );
        }
        log.info("Starting FFmpeg with command: {}", ffmpegCmd);

        // 获取录制时长线程
        RecordManger.getInstance().run(() -> {
            try (Socket socket = recordProgressSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                log.info("recordProgressSocket accept {}",socket);
                recordProgressSocket.close();
                long lastLogTime = System.currentTimeMillis();
                long sliceCount = 0;
                while (!isStop) {
                    String line = reader.readLine();
                    if(log.isTraceEnabled()){
                        log.trace("recordProgress-line {}",line);
                    }
                    // 解析进度行 out_time_ms=123456
                    if(line != null && line.contains("out_time_us=")){
                        String valueStr = line.replace("out_time_us=", "");
                        int seconds = (int) (Long.parseLong(valueStr) / 1_000_000.0);
                        if(seconds <= recordSecond + 1){
                            recordFileSecond.set(seconds );
                        }else {
                            recordFileSecond.set((int) (seconds % recordSecond));
                            long countNow = seconds / recordSecond;
                            if(countNow > sliceCount){
                                sliceCount = countNow;
                                log.info("recordTask ssrc {} sliceCount {}",ssrc, sliceCount);
                            }
                        }
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastLogTime >= 10000) {
                            log.trace("recordTask ssrc {} second {}", ssrc, recordFileSecond.get());
                            lastLogTime = currentTime;
                        }
                    }
                }
            }catch (Exception ex){
                log.error("recordProgressSocket ex",ex);
            }
        });

        // 启动FFmpeg进程
        try {
            ffmpegProcess = Runtime.getRuntime().exec(ffmpegCmd);
        } catch (IOException ex) {
           log.error("start ffmpegProcess ex",ex);
           throw new RuntimeException(ex);
        }

        // getInputStream 输出的是视频数据，如果命令通过管道输出
        RecordManger.getInstance().run(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("FFmpeg stdout: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading FFmpeg stdout", e);
            }
        });

        // getErrorStream 输出日志
        RecordManger.getInstance().run(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("FFmpeg stderr: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading FFmpeg stderr", e);
            }
        });

        // ffmpge阻塞线程
        RecordManger.getInstance().run(() -> {
            try {
                int exitCode = ffmpegProcess.waitFor();
                log.info("FFmpeg process exited with code: {}", exitCode);
                // 临时文件移动到储目录
                if(FileUtil.exist(recordTempFilePath)){
                    // 已经结束了，复制文件
                    log.info("stop recordTask ssrc {} filePath {} second {}",ssrc,recordTempFilePath,recordFileSecond.get());
                    long millis = System.currentTimeMillis();
                    String recordStoragePath = recordTempFilePath.replace("/temp","");
                    FileUtil.del(recordStoragePath);
                    FileUtil.copy(recordTempFilePath, recordStoragePath,true);
                    FileUtil.del(recordTempFilePath);
                    log.info("end copy record file cost {}",System.currentTimeMillis() -millis);
                }
            } catch (Exception ex) {
                log.error("FFmpeg process ex", ex);
            }
        });

        // 分片录制扫描线程, 10秒钟没有更新过的, 重命名和复制文件
        RecordManger.getInstance().run(()->{
            while (!isStop && recordSlice){
                try {
                    Thread.sleep(1000*10);
                } catch (InterruptedException e) {
                    log.error("ex",e);
                }
                List<File> files = FileUtil.loopFiles(recordTempDir).stream()
                        .filter(v->v.length()>1024*1024)
                        .filter(v->v.lastModified() + 1000*10 < System.currentTimeMillis())
                        .toList();
                for(File file :files){
                    String[] split = file.getName().split("_");
                    if(split.length == 2){
                        try {
                            String id = split[0];
                            String startTime = split[1].replace(".mp4","");
                            String endTime = new DateTime(DateUtil.parse(startTime).getTime() + recordSecond * 1000).toString("yyyyMMddHHmmss");
                            String newName = id + "_" + startTime + "_" + endTime + ".mp4";
                            FileUtil.rename(file,newName,true);
                            long millis = System.currentTimeMillis();
                            String finalRecordTempFilePath = recordTempDir + "/" + newName;
                            String recordStoragePath = finalRecordTempFilePath.replace("/temp","");
                            FileUtil.del(recordStoragePath);
                            FileUtil.copy(finalRecordTempFilePath, recordStoragePath,true);
                            FileUtil.del(finalRecordTempFilePath);
                            log.info("end copy recordSlice file cost newName {} {}",newName,System.currentTimeMillis() -millis);
                        }catch (Exception ex){
                            log.error("copy slice length {} ex ",file.length(), ex);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void doStop() {
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
        try {
            if(!recordProgressSocket.isClosed()){
                recordProgressSocket.close();
            }
        } catch (IOException ex) {
            log.error("recordProgressSocket close ex",ex);
        }
    }

}
