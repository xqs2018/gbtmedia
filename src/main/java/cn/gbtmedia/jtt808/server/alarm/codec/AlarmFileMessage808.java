package cn.gbtmedia.jtt808.server.alarm.codec;

import cn.gbtmedia.common.util.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;

/**
 * 消息格式
 * -------------------------------------------------
 * [标识位]    [消息头]   [消息体]   [校验码]   [标识位]
 * -------------------------------------------------
 * [标识位]  0x7e
 * [消息头]  消息id 消息体属性 协议版本号 终端手机号 消息流水号 消息包总数 包序号
 * [消息体]  *****************
 * [校验码]  *****************
 * [标识位]  0x7e
 *-------------------------------------------------
 * 2013 版本消息头为 12 字节或 16 字节，
 * 2019 版本多了 5 个字节，1 个字节的协议版本号，终端手机号多了 4 字节的 BCD 码。
 * @author xqs
 */
@Data
public class AlarmFileMessage808 {

    /**
     * 开始标志位
     */
    private final byte begin = 0x7e;

    /**
     * 0 消息 ID WORD
     */
    private int messageId;

    /**
     * 2 消息体属性 WORD
     *  15    14                 13     12  11  10    9-0
     *  保留  2019版本表示固定1   分包     加密方式     消息体长度
     *        2013版本固定0保留字段
     */
    private short properties;

    /**
     *
     * 协议版本号 BYTE 2019版本才有的字段  协议版本,每次关键修订递增,初始版本为1
     */
    private byte protocolVersion;

    /**
     * 终端手机号 BCD 2013版本6字节  2019版本10字节
     * 手机号不足位的,则在前补充数字 0
     */
    private byte[] clientId;

    /**
     * 消息流水号 WORD 按发送顺序从 0 开始循环累加
     */
    private int serialNo;

    /**
     * 消息总包数 WORD 该消息分包后的总包数
     * properties 13 确定分包有才有此字段
     */
    private int packageTotal;

    /**
     * 包序号 WORD 从 1 开始
     *  properties 13 确定分包有才有此字段
     */
    private int packageNo;

    /**
     * 消息体
     */
    private byte[] payload;

    /**
     * 校验码的计算规则应从消息头首字节开始,同后一字节进行异或操纵,
     * 直到消息体末字节结束;校验码长度为一字节
     */
    private byte checkSum;

    /**
     * 结束标志位
     */
    private final byte end = 0x7e;

    /**
     * 获取clientId字符串
     */
    public String getClientIdStr(){
        return ByteUtil.BCDToStr(clientId);
    }

    /**
     * 获取整个消息
     */
    public byte[] getMessageBytes(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(begin);
        byteBuf.writeShort(messageId);
        byteBuf.writeShort(properties);
        if(protocolVersion != 0){
            byteBuf.writeByte(protocolVersion);
        }
        byteBuf.writeBytes(clientId);
        byteBuf.writeShort(serialNo);
        if (packageTotal != 0) {
            byteBuf.writeShort(packageTotal);
        }
        if (packageNo != 0) {
            byteBuf.writeShort(packageNo);
        }
        if (payload != null) {
            byteBuf.writeBytes(payload);
        }
        byteBuf.writeByte(checkSum);
        byteBuf.writeByte(end);
        int readableBytes = byteBuf.readableBytes();
        byte[] data = new byte[readableBytes];
        byteBuf.readBytes(data);
        return data ;
    }

    /**
     * 打印整个消息16进制字符串
     */
    public String toHexString() {
        return ByteBufUtil.hexDump(getMessageBytes());
    }

    /**
     * 生成校验码
     * 校验码的计算规则应从消息头首字节开始,同后一字节进行异或操纵,
     * 直到消息体末字节结束;校验码长度为一字节
     */
    public byte createCheckSum(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShort(messageId);
        byteBuf.writeShort(properties);
        if(protocolVersion != 0){
            byteBuf.writeByte(protocolVersion);
        }
        byteBuf.writeBytes(clientId);
        byteBuf.writeShort(serialNo);
        if (packageTotal != 0) {
            byteBuf.writeShort(packageTotal);
        }
        if (packageNo != 0) {
            byteBuf.writeShort(packageNo);
        }
        if (payload != null) {
            byteBuf.writeBytes(payload);
        }
        byte checksum = byteBuf.getByte(byteBuf.readerIndex());
        for (int i = byteBuf.readerIndex() + 1; i < byteBuf.writerIndex(); i++) {
            checksum = (byte) (checksum ^ byteBuf.getByte(i));
        }
        return checksum;
    }

    /**
     * 获取版本标记 2019版本表示固定1 2013 2011 版本固定0
     */
    public int getVersionFlag() {
        return (properties >> 14) & 1;
    }

    /**
     * 获取分包标记
     */
    public int getPacketFlag() {
        return (properties >> 13) & 1;
    }

    /**
     * 获取消息体长度
     */
    public int getBodyLength() {
        return properties & 0x03FF;
    }

    /**
     * 设置版本标记 2019版本表示固定1 2013 2011版本固定0
     */
    public void setVersionFlag(int flag) {
        if (flag == 1) {
            properties |= (short) 0xC000;
        } else {
            properties &= (short) ~0xC000;
        }
    }

    /**
     * 设置分包标记
     */
    public void setPacketFlag(int flag) {
        if (flag == 1) {
            properties |= 0x2000;
        } else {
            properties &= ~0x2000;
        }
    }

    /**
     * 消息体长度最大（9-0位）最大值1023
     */
    public void setMessageBodyLength(int length) {
        properties = (short) ((properties & ~0x03FF) | (length & 0x03FF));
    }
}
