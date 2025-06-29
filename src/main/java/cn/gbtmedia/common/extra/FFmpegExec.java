package cn.gbtmedia.common.extra;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.progress.Progress;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author xqs
 */
@Getter
@Slf4j
@Component
public class FFmpegExec{

    private String ffmpegPath ;

    private String ffprobePath;

    public static FFmpegExec getInstance(){
        return SpringUtil.getBean(FFmpegExec.class);
    }

    @Resource
    private ServerConfig serverConfig;

    static {
        // 统一修改日志等级
        FFmpegLogCallback.set();
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
    }

    @PostConstruct
    void init() throws Exception {
        System.setProperty("org.bytedeco.javacpp.pathsFirst", "true");
        log.info("init ffmpeg ffprobe ...");
        try {
            ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            ffprobePath = Loader.load(org.bytedeco.ffmpeg.ffprobe.class);
        }catch (Throwable ex){
            log.info("javacpp load ffmpeg ex",ex);
            ffmpegPath = serverConfig.getFfmpegPath();
            ffprobePath = serverConfig.getFfprobePath();
        }
        log.info("ffmpeg path {}",ffmpegPath);
        log.info("ffprobe path {}",ffprobePath);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,"-version");
            Process start = processBuilder.inheritIO().start();
            int waitFor = start.waitFor();
            if(waitFor != 0){
                throw new RuntimeException("waitFor exit code " + waitFor);
            }
        }catch (Exception ex){
            log.error("check ffmpeg ex",ex);
        }
    }

    public void merge(List<File> inputFiles, BiConsumer<Progress,File> callBack) throws Exception {

        // 创建 FFmpegExecutor
        FFmpegExecutor executor = new FFmpegExecutor(new FFmpeg(ffmpegPath), new FFprobe(ffprobePath));

        // 合并的临时目录
        File tempDir = new File("/var/temp/ffmpeg");
        boolean b1 = tempDir.mkdirs();

        // 输入文件，多个文件写入一个txt
        File inputFileNames = new File("/var/temp/ffmpeg/"+IdUtil.fastSimpleUUID()+".txt");
        boolean b2 = inputFileNames.createNewFile();
        List<String> lines = inputFiles.stream().map(v -> "file '" + v.getAbsolutePath() + "'").collect(Collectors.toList());
        FileUtil.writeLines(lines,inputFileNames, StandardCharsets.UTF_8);

        // 输出文件
        File outputFile = new File("/var/temp/ffmpeg/"+ IdUtil.fastSimpleUUID()+"."+FileUtil.getSuffix(inputFiles.get(0)));

        // 构建FFmpegBuilder
        FFmpegBuilder builder = new FFmpegBuilder()
                .setFormat("concat")
                .overrideOutputFiles(true)
                .setInput(inputFileNames.getAbsolutePath())
                .addExtraArgs("-safe", "0")
                .addExtraArgs("-threads", "2")
                .addOutput(outputFile.getAbsolutePath())
                .setVideoCodec("copy")
                .setAudioCodec("copy")
                .done();

        // 执行合并任务
        FFmpegJob job = executor.createJob(builder, (Progress progress) -> {
            if (progress.status.equals(Progress.Status.END)){
                callBack.accept(progress,outputFile);
                // 删除临时txt文件
                FileUtil.del(inputFileNames);
            }else {
                callBack.accept(progress,outputFile);
            }
        });
        job.run();
    }

}
