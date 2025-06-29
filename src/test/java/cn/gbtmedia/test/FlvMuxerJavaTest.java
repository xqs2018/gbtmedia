package cn.gbtmedia.test;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.gbt28181.server.media.stream.muxer.FlvMuxer;
import cn.gbtmedia.gbt28181.server.media.stream.muxer.FlvMuxerJavaCv;
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
public class FlvMuxerJavaTest {


    static List<byte[]> testflvList =new ArrayList<>();
    private static void testflvList(ByteBuf data) {
        testflvList.add(ByteBufUtil.getBytes(data));
        log.info("testflvList {} ",testflvList.size());
        if (testflvList.size() == 200) {
            try {
                byte[] bytes = ByteUtil.mergeByte(testflvList);
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

    public static void main1(String[] args) throws InterruptedException {
        FlvMuxer flvEncoder= new FlvMuxer();
        flvEncoder.onFlvData(FlvMuxerJavaTest::testflvList);
        byte[] bytes = FileUtil.readBytes("/var/test-h26433417.h264");
        flvEncoder.addH264(System.currentTimeMillis(),bytes);
//        int one = bytes.length / 20;
//        for(int i =0;i<20;i++){
//            Thread.sleep(500);
//        }
    }

    public static void main2(String[] args) throws InterruptedException {
        FlvMuxer flvEncoder= new FlvMuxer();
        flvEncoder.onFlvData(FlvMuxerJavaTest::testflvList);
        byte[] bytes = FileUtil.readBytes("/var/test-g71115095.g711");
        flvEncoder.addG711a(System.currentTimeMillis(),bytes);
//        int one = bytes.length / 20;
//        for(int i =0;i<20;i++){
//            Thread.sleep(500);
//        }
    }

    public static void main(String[] args) throws InterruptedException {
        FlvMuxerJavaCv flvEncoder = new FlvMuxerJavaCv();
        flvEncoder.onFlvData(FlvMuxerJavaTest::testflvList);
        byte[] bytes = FileUtil.readBytes("/var/test-g71115095.g711");
        byte[] bytesh264 = FileUtil.readBytes("/var/test-h26433417.h264");
        //flvEncoder.addH264(System.currentTimeMillis(),bytesh264);
        // G711是8kHz采样率，每帧1024个采样点
        // G711是8位采样，所以每帧是1024字节
        int g711FrameSize = 1024; // G711每帧的字节数
        int audioFrames = bytes.length / g711FrameSize;

        // 计算音频总时长（毫秒）
        long audioDuration = (long) audioFrames * 128; // 每帧128ms (1024/8000 * 1000)

        // 计算需要的视频帧数（30fps）
        int videoFrames = (int) (audioDuration / 1000.0 * 30); // 30fps

        // 计算每帧视频数据大小，如果视频数据不够，就循环使用
        int videoFrameSize = Math.min(bytesh264.length, 1024); // 使用较小的帧大小
        int availableVideoFrames = bytesh264.length / videoFrameSize;

        log.info("音频数据: {} 字节, {} 帧, 总时长: {}ms", bytes.length, audioFrames, audioDuration);
        log.info("视频数据: {} 字节, 需要 {} 帧, 可用 {} 帧, 每帧 {} 字节",
                bytesh264.length, videoFrames, availableVideoFrames, videoFrameSize);

        // 按帧发送数据
        for (int i = 0; i < audioFrames; i++) {
            // 发送音频帧
            byte[] audioFrame = new byte[g711FrameSize];
            System.arraycopy(bytes, i * g711FrameSize, audioFrame, 0, g711FrameSize);
            flvEncoder.addG711a(System.currentTimeMillis(), audioFrame);

            // 发送视频帧（每1个音频帧发送4个视频帧，因为音频帧是128ms，视频帧是33.33ms）
            for (int j = 0; j < 4; j++) {
                int videoFrameIndex = (i * 4 + j) % availableVideoFrames;
                byte[] videoFrame = new byte[videoFrameSize];
                System.arraycopy(bytesh264, videoFrameIndex * videoFrameSize, videoFrame, 0, videoFrameSize);
                flvEncoder.addH264(System.currentTimeMillis(), videoFrame);
                Thread.sleep(5); // 短暂暂停，确保视频帧之间有间隔
            }

            // 每帧之间稍微暂停一下，模拟实时流
            Thread.sleep(20); // 20ms，因为8000Hz采样率下，1024个采样点需要128ms
        }
    }

}
