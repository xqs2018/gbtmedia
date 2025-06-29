package cn.gbtmedia.test;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.gbt28181.server.media.stream.FlvEncoder;
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
public class H265Test {


    static List<byte[]> testFlvList =new ArrayList<>();
    private static void testFlvList(ByteBuf data) {
        testFlvList.add(ByteBufUtil.getBytes(data));
        if (testFlvList.size() == 100) {
            try {
                byte[] bytes = ByteUtil.mergeByte(testFlvList);
                FileUtil.del("/var/test-flv84666.flv");
                File file = new File("/var/test-flv84666" + ".flv");
                file.createNewFile();
                IoUtil.write(new FileOutputStream(file), true, bytes);
                log.info("************写入测试flv {} ****************", file.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FlvEncoder flvEncoder= new FlvEncoder();
        flvEncoder.onFlvData(data->{
            testFlvList(data);
        });
        byte[] bytes = FileUtil.readBytes("/var/test-h264-1130058.h265");
        flvEncoder.addH265(System.currentTimeMillis(),bytes);
//        int one = bytes.length / 20;
//        for(int i =0;i<20;i++){
//            Thread.sleep(500);
//        }
    }

}
