package cn.gbtmedia.gbt28181.server.media.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;

/**
 * @author xqs
 */
@Data
public class RtpMessage {

    /**
     * 1   V 2 BITS 固定为 2
     *     P 1 BIT  固定为 0
     *     X 1 BIT  RTP头是否需要扩展位，固定为 0
     *     CC 4 BITS 固定为 1
     */
    private int vpxcc = 0b10000001;

    /**
     * 2   M 1 BIT 标志位，确定是否是完整数据帧的边界
     *     PT 7 BITS 负载类型，见表 12
     *                音频  8 PCMA  25 MP3
     *                视频  98 H.264   96 ps国标
     */
    private byte mpt;

    /**
     * 4 包序号 WORD初始为 0，每发送一个 RTP 数据包，序列号 加 1
     */
    private int sequenceNumber;

    /**
     * 8 时间戳 DWORD 标识此 RTP 数据包当前帧的相对时间，单 位毫秒( ms)
     */
    private long timestamp;

    /**
     * 12  ssrc  DWORD
     */
    private long ssrc;

    /**
     * 数据包
     */
    private byte[] payload;

    /**
     * 获取负载类型
     */
    public int getPt(){
        return mpt & 0x7F;
    }

    /**
     * 获取10位ssrc字符串
     */
    public String getSsrcStr(){
        return String.format("%010d", ssrc);
    }

    /**
     * 获取整个消息
     */
    public byte[] getMessageBytes(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(vpxcc);
        byteBuf.writeByte(mpt);
        byteBuf.writeShort(sequenceNumber);
        byteBuf.writeInt((int) timestamp);
        byteBuf.writeInt((int) ssrc);
        byteBuf.writeBytes(payload);
        return ByteBufUtil.getBytes(byteBuf);
    }

    /**
     * 转成rtp消息
     */
    public static RtpMessage toRtpMessage(byte[] data){
        ByteBuf in = Unpooled.wrappedBuffer(data);
        RtpMessage message = new RtpMessage();
        message.setVpxcc(in.readByte());
        message.setMpt(in.readByte());
        message.setSequenceNumber(in.readUnsignedShort());
        message.setTimestamp(in.readUnsignedInt());
        message.setSsrc(in.readUnsignedInt());
        byte[] payload = new byte[in.readableBytes()];
        in.readBytes(payload);
        message.setPayload(payload);
        return message;
    }
}
