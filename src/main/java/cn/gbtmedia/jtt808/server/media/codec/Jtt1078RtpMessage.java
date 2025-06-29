package cn.gbtmedia.jtt808.server.media.codec;

import cn.gbtmedia.common.util.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;

/**
 * 报文类型：码流数据报文。 实时音视频流数据的传输参考 RTP 协议，使用 UDP 或 TCP 承载。
 * 负载包格式在 IETF RFC 3550 RTP 定义的基础上补充了消息流水号、SIM 卡号、音视频通道号等字段，
 * 其负载包格式定义见表 19。 表 中定义的 bit 位按照大端模式(big-endian)进行填写。
 * @author xqs
 */
@Data
public class Jtt1078RtpMessage {

    /**
     * 0 帧头标识 DWORD 固定为 0x30 0x31 0x63 0x64
      */
    private int begin = 0x30316364;

    /**
     * 4   V 2 BITS 固定为 2
     *     P 1 BIT  固定为 0
     *     X 1 BIT  RTP头是否需要扩展位，固定为 0
     *     CC 4 BITS 固定为 1
     */
    private int vpxcc = 0b10000001;

    /**
     * 5   M 1 BIT 标志位，确定是否是完整数据帧的边界
     *     PT 7 BITS 负载类型，见表 12
     *                音频  6 G.711A  25 MP3 26 ADPMC 19 ACC
     *                视频  98 H.264
     */
    private byte mpt;

    /**
     *  6 包序号 WORD初始为 0，每发送一个 RTP 数据包，序列号 加 1
     */
    private int sequenceNumber;

    /**
     *  8 SIM 卡号 BCD[6] 终端设备 SIM 卡号   2019版本是BCD[10]
     */
    private byte[] clientId;

    /**
     *  14 逻辑通道号 BYTE 按照 JT/ T 1076—2016 中的表 2
     */
    private int channelNo;

    /**
     * 15  数据类型 4 BITS
     *     0000：视频 I 帧；
     *     0001：视频 P 帧；
     *     0010：视频 B 帧；
     *     0011：音频帧；
     *     0100：透传数据
     *     分包处理标记 4 BITS
     *     0000：原子包，不可被拆分；
     *     0001：分包处理时的第一个包；
     *     0010：分包处理时的最后一个包；
     *     0011：分包处理时的中间包
     */
    private byte dataAndPackType;

    /**
     * 16 时间戳 BYTE[8]标识此 RTP 数据包当前帧的相对时间，单 位毫秒( ms)。 当数据类型为 0100 时，则没有该字段
     */
    private long timestamp;

    /**
     * 24 Last I Frame Interval WORD 该帧与上一个关键帧之间的时间间隔，单 位毫秒(ms)，当数据类型为非视频帧时，则 没有该字段
     */
    private int lastIFrameInterval;

    /**
     * 26 Last Frame Interval WORD该帧与上一帧之间的时间间隔，单位毫秒(ms)，当数据类型为非视频帧时，则没有该 字段
     */
    private int lastFrameInterval;

    /**
     * 28 数据体长度 WORD 后续数据体长度，不含此字段
     */
    private int bodyLength;

    /**
     * 30 数据体 BYTE[n]音视频数据或透传数据，长度不超过950byte
     */
    private byte[] payload;

    /**
     * 获取负载类型
     */
    public int getPt(){
        return mpt & 0x7f;
    }

    /**
     * 设置负载类型
     */
    public void setPt(int pt) {
        mpt = (byte) ((mpt & 0x80) | (pt & 0x7f));
    }

    /**
     * 获取数据类型
     */
    public int getDataType(){
        return  (dataAndPackType >> 4) & 0x0f;
    }

    /**
     * 设置数据类型
     */
    public void setDataType(int dataType) {
        dataAndPackType = (byte) ((dataAndPackType & 0x0f) | (dataType << 4));
    }

    /**
     * 获取分包类型
     */
    public int getPackType(){
        return dataAndPackType & 0x0f;
    }

    /**
     * 设置分包类型
     */
    public void setPackType(int packType) {
        dataAndPackType = (byte) ((dataAndPackType & 0xf0) | packType);
    }

    /**
     * 获取clientId字符串
     */
    public String getClientIdStr(){
        return ByteUtil.BCDToStr(clientId);
    }

    /**
     * 获取整个消息
     */
    public byte[] getMessageBytes() {
        return ByteBufUtil.getBytes(getMessageByteBuf());
    }

    /**
     * 获取整个消息
     */
    public ByteBuf getMessageByteBuf() {
        ByteBuf out = Unpooled.buffer();
        out.writeInt(this.getBegin());
        out.writeByte(this.getVpxcc());
        out.writeByte(this.getMpt());
        out.writeShort(this.getSequenceNumber());
        out.writeBytes(this.getClientId());
        out.writeByte(this.getChannelNo());
        out.writeByte(this.getDataAndPackType());
        int dataType = this.getDataType();
        if(dataType != 0b0100){
            out.writeLong(this.getTimestamp());
        }
        if(dataType != 0b0011 && dataType != 0b0100){
            out.writeShort(this.getLastIFrameInterval());
            out.writeShort(this.getLastFrameInterval());
        }
        out.writeShort(this.getBodyLength());
        out.writeBytes(this.getPayload());
        return out;
    }

    /**
     * 转成Jtt1078RtpMessage
     */
    public static Jtt1078RtpMessage toJtt1078RtpMessage(ByteBuf in) {
        Jtt1078RtpMessage message = new Jtt1078RtpMessage();
        message.setBegin(in.readInt());
        message.setVpxcc(in.readByte());
        message.setMpt(in.readByte());
        message.setSequenceNumber(in.readUnsignedShort());
        byte[] clientId = new byte[6];
        in.readBytes(clientId);

        message.setClientId(clientId);
        message.setChannelNo(in.readUnsignedByte());
        message.setDataAndPackType(in.readByte());
        int dataType = message.getDataType();
        if(dataType != 0b0100){
            message.setTimestamp(in.readLong());
        }
        if(dataType != 0b0011 && dataType != 0b0100){
            message.setLastIFrameInterval(in.readUnsignedShort());
            message.setLastFrameInterval(in.readUnsignedShort());
        }
        message.setBodyLength(in.readUnsignedShort());
        byte[] payload = new byte[message.getBodyLength()];
        in.readBytes(payload);
        message.setPayload(payload);
        return message;
    }

    /**
     * 转成Jtt1078RtpMessage
     */
    public static Jtt1078RtpMessage toJtt1078RtpMessage(byte[] data) {
        ByteBuf in = Unpooled.wrappedBuffer(data);
        return toJtt1078RtpMessage(in);
    }

}
