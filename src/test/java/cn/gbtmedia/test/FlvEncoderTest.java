package cn.gbtmedia.test;

import cn.gbtmedia.common.util.ByteUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author xqs
 */
@Slf4j
public class FlvEncoderTest {

    public static void main3(String[] args) {
        ByteBuf audioTag = Unpooled.buffer();

        {
            // 获取当前时间戳
            long timestamp = 4294967295L;
            System.out.println("设置时间 " + timestamp);
            // 创建 ByteBuf 对象

            // 写入一个字节 0x08
            audioTag.writeByte(0x08);
            // 本 tag data 部分大小 3 字节
            audioTag.writeMedium(1654);
            // tag 时间戳 3 字节
            audioTag.writeMedium((int) (timestamp & 0x00FFFFFF));
            // tag 时间戳扩展 前 3 个字节不够扩展这个一个字节
            audioTag.writeByte((int) ((timestamp >> 24) & 0xFF));

            // 读取低 24 位
            int low24Bits =  audioTag.getUnsignedMedium(4);
            // 读取高 8 位
            int high8Bits = audioTag.getUnsignedByte(7) & 0xFF;
            // 组合成 32 位整数
            int timestamp32Bits = (high8Bits << 24) | low24Bits;
            // 扩展为 64 位 long 类型
            long time =  ((long) (high8Bits << 24) | low24Bits) & 0xFFFFFFFFL;

            System.out.println("读取时间 " + time);
        }

        // long 强转无符号int
        long longValue = 4294967295L;
        ByteBuf byteBuf = Unpooled.buffer();
        int i = (int) (longValue & 0xFFFFFFFFL);
        System.out.println(""+i);
        byteBuf.writeInt(i);
        long anInt = byteBuf.readUnsignedInt();
        System.out.println(""+anInt);

        {
            // 获取当前时间戳
            long timestamp = 4294967294L;
            System.out.println("设置时间 " + timestamp);

            audioTag.setMedium(4,(int) (timestamp & 0x00FFFFFF));

            audioTag.setByte(7,(int) ((timestamp >> 24) & 0xFF));

            // 读取低 24 位
            int low24Bits =  audioTag.getUnsignedMedium(4);
            // 读取高 8 位
            int high8Bits = audioTag.getUnsignedByte(7) & 0xFF;
            // 组合成 32 位整数
            int timestamp32Bits = (high8Bits << 24) | low24Bits;
            // 扩展为 64 位 long 类型
            long time =  ((long) timestamp32Bits) & 0xFFFFFFFFL;

            System.out.println("读取时间 " + time);
        }
    }
    public static void main4(String[] args) {
        // 2147483647 int 最大有符号
        // 4294967295 int 最大无符号
        long longValue = 4294967295L;

        int intValue = (int) longValue;
        System.out.println("intValue: " + intValue);

        long unsignedLongValue = ((long) intValue) & 0xFFFFFFFFL;
        System.out.println("unsignedLongValue: " + unsignedLongValue);

        ByteBuf byteBuf1 = Unpooled.buffer();
        ByteBuf byteBuf2 = Unpooled.buffer();

        // 直接强制转换
        byteBuf1.writeInt((int) longValue);
        // 先进行位运算再强制转换
        byteBuf2.writeInt((int) (longValue & 0xFFFFFFFFL));

        // 输出结果
        System.out.println("Direct cast: " + byteBuf1.readUnsignedInt());
        System.out.println("Bitwise and then cast: " + byteBuf2.readUnsignedInt());
    }
    public static void main(String[] args) throws InterruptedException {
        ByteBufHolder byteBufHolder;

        addH264(new byte[] {
                0X0,  0X0,  0X0,  0X1,  // 标志位
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
        });
        addH264(new byte[] {
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
        });

        addH264(new byte[] {
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
        });
        addH264(new byte[] {
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
        });
        addH264(new byte[] {
                0X0,  0X0,  0X1,  0x65,
        });
        addH264(new byte[] {
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
        });
        log.info("第一次");
//
//        addH264(new byte[]{7,7,7});
//        addH264(new byte[]{0X0,  0X0,  0X1,  0x65});
//        log.info("第二次");
//
//        addH264(new byte[]{
//                4,4,4,4,4,4,4,4,4,4,4,  // 数据
//                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
//                5,5,5,5,5,5,5,5,5,5,5,  // 数据
//                5,5,5,5,5,5,5,5,5,5,5,  // 数据
//                0X0,  0X0,  0X0,  0X1,  // 标志位
//                6,6,6,6,6,6,6,6,6,6,6,  // 数据
//                6,6,6,6,6,6,6,6,6,6,6,  // 数据
//        });
//        log.info("第三次");
    }

    private static final ByteBuf h264Buf = Unpooled.buffer(1024);

    static int count;
    public static void  addH264(byte[] h264){
        h264Buf.writeBytes(h264);
        while (true) {
            byte[] naluData = null;
            // 正常nalu 以 00 00 00 01 起始标志。
            // 兼容mp4视频文件
            // 00 00 00 01 65 或 00 00 01 65
            // 00 00 00 01 或 00 00 01 为nalu的起始标志。
            // 00 00 01 在某些情况下也会使用，特别是当数据封装在某些特定的容器格式中时，
            // 为了减少起始码的数量和冗余，可能会采用三个字节的起始码。
            for (int i = 0; i < h264Buf.readableBytes() - 3; i++) {
                int a = h264Buf.getByte(i + 0) & 0xff;
                int b = h264Buf.getByte(i + 1) & 0xff;
                int c = h264Buf.getByte(i + 2) & 0xff;
                int d = h264Buf.getByte(i + 3) & 0xff;
                if ((a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01)||(a == 0x00 && b == 0x00 && c == 0x01 && d == 0x65)) {
                    if (i == 0) {
                        continue;
                    }
                    byte[] data = new byte[i];
                    h264Buf.readBytes(data);
                    //h264Buf.discardReadBytes(); //重置已经读取的
                    if(data.length > 3 && data[3] == 0x65){
                        // 前面补 00 00 01 65 => 完整格式 00 00 00 01 65
                        byte[] newData = new byte[data.length+1];
                        newData[0] = 0;
                        System.arraycopy(data, 0, newData, 1, data.length - 1);
                        data = newData;
                    }
                    naluData =  data;
                    log.info("已经完成读取 {}  data {} ", count++ , ByteBufUtil.hexDump(naluData));
                    break;
                }
            }
            if (naluData == null) {
                break;
            }
            if (naluData.length < 4) {
                continue;
            }

            // 去掉开头四个字节的标记
            byte[] nalu = new byte[naluData.length - 4];
            System.arraycopy(naluData, 4, nalu, 0, naluData.length - 4);

        }
    }

    public static void main2(String[] args) throws InterruptedException {
        byte[] data = new byte[] {
                0X0,  0X0,  0X0,  0X1,  // 标志位
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
        };

        CompletableFuture.runAsync(()->{
            ByteBuf byteBuf = Unpooled.buffer();
            //byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);
            while (true) {
                System.out.println("byteBuf大小: " + byteBuf.capacity());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < byteBuf.readableBytes() - 3; i++) {
                    int a = byteBuf.getByte(i + 0) & 0xff;
                    int b = byteBuf.getByte(i + 1) & 0xff;
                    int c = byteBuf.getByte(i + 2) & 0xff;
                    int d = byteBuf.getByte(i + 3) & 0xff;
                    if ((a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01)||(a == 0x00 && b == 0x00 && c == 0x01 && d == 0x65)) {
                        if (i == 0) {
                            continue;
                        }
                        byte[] dataBlock = new byte[i];
                        byteBuf.readBytes(dataBlock);
                        //   0X0,  0X0,  0X1,  0x65 改成    0X0,  0X0,  0X0, 0X1
                        dataBlock[2] = 0x0;
                        dataBlock[3] = 0x1;
                        System.out.println("发现一个数据块: " + ByteBufUtil.hexDump(dataBlock));
                        break;
                    }
                }
            }
        });
        Thread.sleep(10000000);
    }

    public static void main1(String[] args) throws InterruptedException {
        byte[] data = new byte[] {
                0X0,  0X0,  0X0,  0X1,  // 标志位
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                2,2,2,2,2,2,2,2,2,2,2,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                3,3,3,3,3,3,3,3,3,3,3,  // 数据
                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
                4,4,4,4,4,4,4,4,4,4,4,  // 数据
                0X0,  0X0,  0X1,  0x65, // 标志位(mp4的) I帧
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                5,5,5,5,5,5,5,5,5,5,5,  // 数据
                0X0,  0X0,  0X0,  0X1,  // 标志位
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
                6,6,6,6,6,6,6,6,6,6,6,  // 数据
        };

        // 创建 ByteBuf 来包裹数据


        // 定义两种标志位模式（4字节序列）
        byte[] flagPattern1 = { 0x00, 0x00, 0x00, 0x01 };
        byte[] flagPattern2 = { 0x00, 0x00, 0x01, 0x65 };

        CompletableFuture.runAsync(()->{
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(data);

            // 遍历 ByteBuf，读取数据并根据标志位分割
            while (true) {

                System.out.println("byteBuf大小: " + byteBuf.capacity());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 检查当前的位置是否以任意一种标志位模式开头
                boolean flagFound = false;
                if (byteBuf.readableBytes() >= flagPattern1.length) {
                    for (int i = 0; i < flagPattern1.length; i++) {
                        if (byteBuf.getByte(byteBuf.readerIndex() + i) != flagPattern1[i]) {
                            break;
                        }
                        if (i == flagPattern1.length - 1) {
                            flagFound = true;
                        }
                    }
                }

                if (!flagFound && byteBuf.readableBytes() >= flagPattern2.length) {
                    for (int i = 0; i < flagPattern2.length; i++) {
                        if (byteBuf.getByte(byteBuf.readerIndex() + i) != flagPattern2[i]) {
                            break;
                        }
                        if (i == flagPattern2.length - 1) {
                            flagFound = true;
                        }
                    }
                }

                // 如果找到了标志位，处理数据块
                if (flagFound) {
                    // 找到标志位后，跳过该标志位（4字节）
                    byteBuf.skipBytes(flagPattern1.length);

                    // 查找下一个标志位的位置
                    int nextFlagIndex = -1;

                    // 查找两种标志位的下一个位置
                    int nextFlag1 = byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.writerIndex(), flagPattern1[0]);
                    int nextFlag2 = byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.writerIndex(), flagPattern2[0]);

                    // 选择更早的标志位
                    if (nextFlag1 != -1 && (nextFlag2 == -1 || nextFlag1 < nextFlag2)) {
                        nextFlagIndex = nextFlag1;
                    } else if (nextFlag2 != -1) {
                        nextFlagIndex = nextFlag2;
                    } else {
                        continue;
                        // nextFlagIndex = byteBuf.writerIndex(); // 没有更多标志位，处理到缓冲区末尾
                    }

                    // 直接读取数据块
                    int dataLength = nextFlagIndex - byteBuf.readerIndex();
                    byte[] dataBlock = new byte[dataLength];
                    byteBuf.readBytes(dataBlock);

                    // 提取当前标志位和下一个标志位之间的数据块
                    ByteBuf segment = Unpooled.buffer();
                    segment.writeBytes(new byte[] { 0x00, 0x00, 0x00, 0x01 });
                    segment.writeBytes(dataBlock);

                    // 处理这个数据块（这里示例是打印出来）

                    System.out.println("发现一个数据块: " + ByteBufUtil.hexDump(segment));

                    // 更新读取位置，跳到下一个标志位位置
                    byteBuf.readerIndex(nextFlagIndex);

                    // 清除已读取的字节，释放内存
                    byteBuf.discardReadBytes();

                } else {
                    // 如果没有找到标志位，跳出循环
                    //break;
                }
                byteBuf.discardReadBytes();

                int readableBytes = byteBuf.readableBytes();
                byte[] last = new byte[readableBytes];
                byteBuf.readBytes(last);
                byteBuf = Unpooled.buffer();
                byteBuf.writeBytes(last);
            }
        });
        Thread.sleep(10000000);
    }

    static List<byte[]> testFlvList =new ArrayList<>();
    private static void testFlvList(ByteBuf data){
        testFlvList.add(ByteBufUtil.getBytes(data));
        if(testFlvList.size()==500){
            try {
                byte[] bytes = ByteUtil.mergeByte(testFlvList);
                File file = new File("/var/test-flv"+ RandomUtil.randomNumbers(5) +".flv");
                file.createNewFile();
                IoUtil.write(new FileOutputStream(file), true,bytes);
                log.info("************写入测试flv {} ****************",file.getAbsolutePath());
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
