package cn.gbtmedia.test;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
public class FileDirLoopTest {

    static String path = "/var/fileloop/test";

    public static void main(String[] args) {
        //createTestDir();
        //testUse();
    }

    private static void testUse(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("遍历文件夹");
        FileUtil.ls(path);
        stopWatch.stop();

        stopWatch.start("递归文件夹");
        FileUtil.loopFiles(path);
        stopWatch.stop();

        log.info("耗时 {}",stopWatch.prettyPrint(TimeUnit.MILLISECONDS));

    }
    private static void createTestDir(){
        StopWatch stopWatch = new StopWatch();

        stopWatch.start("创建文件夹");
        for(int i = 0; i< 1000;i++){
            String dirName = "dir_abcdef" + i + "_" + IdUtil.fastSimpleUUID();
            File file = new File(path +"/" + dirName);
            file.mkdirs();
            for(int j=0;j<5;j++){
                FileUtil.touch(file.getAbsoluteFile() + "/" + "file_fghjk"+j+"_"+IdUtil.fastSimpleUUID());
            }
            log.info("createTestDir " + i);
        }
        stopWatch.stop();
        log.info("createTestDir {}",stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
    }
}
