package cn.gbtmedia.gbt28181.server.media.record;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * @author xqs
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class RecordTaskJavaCv extends RecordTask{

    @Override
    public void doStart() {
        startRecord();
    }

    @Override
    public void doStop() {
        stopRecord();
    }

    private Thread recordThread;

    private AtomicInteger recordSliceCount = new AtomicInteger(0);

    private void startRecord(){
        String name = "gbt28181-record-" + recordParam.getSsrc();
        if(recordParam.isRecordSlice()){
            name += "-" + recordSliceCount.incrementAndGet();
        }
        // recordThread = Thread.ofPlatform().name(name).start(recordTask());
        // 直接录制copy 无需转码，不耗费cpu
        isStop = false;
        recordThread = Thread.ofVirtual().name("vt-"+name).start(recordTask());
    }

    private void stopRecord(){
        if(recordThread == null){
            return;
        }
        isStop = true;
        recordThread.interrupt();
    }

    private Runnable recordTask(){
        return () -> {
            FFmpegFrameGrabber grabber = null;
            FFmpegFrameRecorder recorder = null;
            try {
                String httpFlv = recordParam.getPullUrl();
                log.info("recordTask pullUrl {}",httpFlv);
                String ssrc = recordParam.getSsrc();
                String recordPath = recordParam.getRecordPath();
                long recordSecond = recordParam.getRecordSecond();
                boolean recordSlice = recordParam.isRecordSlice();
                String recordTempPath = recordPath + "/temp/"+ IdUtil.fastSimpleUUID()+".mp4";
                // 文件开头是时间，当前小时开头到结束
                AtomicBoolean recordSliceStop = new AtomicBoolean(false);
                if(recordSlice){
                    // 默认一个小时一个文件
                    String a = new DateTime().toString("yyyyMMddHH0000");
                    String b = new DateTime(new Date().getTime()+3600*1000).toString("yyyyMMddHH0000");
                    // 1分钟一个文件
                    if(recordSecond == 60){
                        a = new DateTime().toString("yyyyMMddHHmm00");
                        b = new DateTime(new Date().getTime()+60*1000).toString("yyyyMMddHHmm00");
                    }
                    long time = new DateTime(b).getTime() - new DateTime().getTime();
                    recordTempPath = recordPath + "/temp/"+ a +"_"+ b +"_"+recordSliceCount.get()+"_"+IdUtil.fastSimpleUUID()+".mp4";
                    SchedulerTask instance = SchedulerTask.getInstance();
                    instance.startDelay("recordSliceStart-" + recordPath + "-" + recordSliceCount.get()+1,()->{
                        log.info("recordSliceStart ssrc {} count {}", ssrc, recordSliceCount.get()+1);
                        recordSliceStop.set(true);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            log.error("ex",e);
                        }
                        startRecord();
                    },time);
                }
                log.info("recordTask filePath {}",recordTempPath);
                // 初始化FFmpeg拉流器
                grabber = new FFmpegFrameGrabber(httpFlv);
                grabber.setOption("timeout", "5000000"); // 5秒超时 适用RTSP
                grabber.setOption("rw_timeout", "10000000"); // 10秒读写超时 适用RTMP FLV
                grabber.start();
                // 初始化FFmpeg记录器
                FileUtil.del(recordTempPath);
                FileUtil.touch(recordTempPath);
                // TODO 先不录制音频 mp4不支持g711a
                recorder = new FFmpegFrameRecorder(recordTempPath, grabber.getImageWidth(), grabber.getImageHeight(),0);
                recorder.setFormat("mp4");
                recorder.start(grabber.getFormatContext());
                //开始录制
                long startTime = System.currentTimeMillis();
                long lastLogTime = System.currentTimeMillis();
                long packetCount = 0;
                long lastDts = 0;
                long incrVideoDts = 0;
                long videoTs = 0;
                while (!isStop) {
                    // 录制分片停止
                    if(recordSlice && recordSliceStop.get()){
                        break;
                    }
                    AVPacket packet = grabber.grabPacket();
                    if(packet != null){
                        // TODO 时间戳优化
                        long timestamp = System.currentTimeMillis() - startTime;
                        //recorder.setTimestamp(timestamp); // 设置时间戳 AVPacket模式下手动维护
                        videoTs = 1000 * (System.currentTimeMillis() - startTime);
                        if (videoTs > recorder.getTimestamp()) {
                            if(log.isDebugEnabled()){
                                log.warn("videoTs {} timestamp {} ",videoTs,timestamp);
                            }
                            // recorder.setTimestamp((videoTs));
                        }
                        int stream_index = packet.stream_index();
                        long pts = packet.pts();
                        long dts = packet.dts();
                        if(log.isTraceEnabled()){
                            log.trace("stream_index {} pts {} dts {}",stream_index,pts,dts);
                        }
                        // 检查并校正时间戳，先直接跳过不管
                        if (dts <= lastDts) {
                            if(log.isDebugEnabled()){
                                log.warn("invalid dts {} last dts {}", dts, lastDts);
                            }
                            // 0是开头配置帧和首帧
                            if(dts != 0){
                                continue;
                            }
                        }
                        // 录制当前数据
                        recorder.recordPacket(packet);
                        // 记录实际已经录制了多少秒 总帧数 / FPS TODO 实际计算可能不准
                        if(packet.stream_index() == grabber.getVideoStream()){
                            long second = (packetCount++) / (int) recorder.getFrameRate();
                            recordFileSecond.set((int) second);

                            // 通过 dts 计算时间 ,dts可能不是0开始
                            if(incrVideoDts == 0){
                                incrVideoDts = lastDts;
                            }
                            long cut = dts - lastDts;
                            incrVideoDts = incrVideoDts + cut;
                            AVStream videoStream = grabber.getFormatContext().streams(grabber.getVideoStream());
                            double ptsTime = incrVideoDts * av_q2d(videoStream.time_base()); // 默认 1000
                            recordFileSecond.set((int) ptsTime);

                            // 10秒打印一次日志，录制多少秒了
                            if(log.isDebugEnabled()){
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastLogTime >= 10000 && !recordSlice) {
                                    log.trace("recordTask ssrc {} second {}", ssrc, recordFileSecond.get());
                                    lastLogTime = currentTime;
                                }
                            }
                        }
                        // 更新最后dts
                        lastDts = dts;
                    }else {
                        break;
                    }
                }
                // 先关闭不然占用文件句柄
                try {
                    log.info("stop grabber");
                    grabber.close();
                    grabber = null;
                } catch (Exception e) {
                    log.error("stop grabber ex", e);
                }
                try {
                    log.info("stop recorder");
                    recorder.close();
                    recorder = null;
                } catch (Exception e) {
                    log.error("stop recorder ex",e);
                }
                log.info("stop recordTask ssrc {} filePath {} second {}",ssrc,recordTempPath,recordFileSecond.get());
                long millis = System.currentTimeMillis();
                // 移动到储目录
                String recordStoragePath = recordTempPath.replace("/temp","");
                FileUtil.del(recordStoragePath);
                FileUtil.copy(recordTempPath, recordStoragePath,true);
                FileUtil.del(recordTempPath);
                log.info("end copy record file cost {}",System.currentTimeMillis() -millis);
            }catch (Exception ex){
                log.error("recordTask ex",ex);
            }finally {
                try {
                    if(grabber !=null){
                        log.info("stop grabber");
                        grabber.close();
                    }
                } catch (Exception e) {
                    log.error("stop grabber ex", e);
                }
                try {
                    if(recorder !=null){
                        log.info("stop recorder");
                        recorder.close();
                    }
                } catch (Exception e) {
                    log.error("stop recorder ex",e);
                }
            }
        };
    }
}
