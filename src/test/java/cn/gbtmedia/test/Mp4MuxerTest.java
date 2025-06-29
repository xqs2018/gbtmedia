package cn.gbtmedia.test;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.gbt28181.server.media.stream.muxer.Mp4Muxer;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
public class Mp4MuxerTest {


    static List<byte[]> testMp4List =new ArrayList<>();
    private static void testMp4List(ByteBuf data) {
        testMp4List.add(ByteBufUtil.getBytes(data));
        if (testMp4List.size() == 100) {
            try {
                byte[] bytes = ByteUtil.mergeByte(testMp4List);
                FileUtil.del("/var/test-mp484666.mp4");
                File file = new File("/var/test-mp484666" + ".mp4");
                file.createNewFile();
                IoUtil.write(new FileOutputStream(file), true, bytes);
                log.info("************写入测试mp4 {} ****************", file.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Mp4Muxer mp4Muxer = new Mp4Muxer();
        byte[] bytes = FileUtil.readBytes("/var/test-h26433417.h264");
        mp4Muxer.addH264(System.currentTimeMillis(),bytes);
//        int one = bytes.length / 20;
//        for(int i =0;i<20;i++){
//            Thread.sleep(500);
//        }
    }

}
