package cn.gbtmedia.gbt28181.server.media.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xqs
 */
@Slf4j
public class RtpDecoder{

    public static class Tcp extends ByteToMessageDecoder{

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out){
            RtpMessage message = new RtpMessage();
            if(in.readableBytes() <= 12){
                return;
            }
            message.setVpxcc(in.readByte());
            message.setMpt(in.readByte());
            message.setSequenceNumber(in.readUnsignedShort());
            message.setTimestamp(in.readUnsignedInt());
            message.setSsrc(in.readUnsignedInt());
            byte[] payload = new byte[in.readableBytes()];
            in.readBytes(payload);
            message.setPayload(payload);
            out.add(message);
        }
    }


    public static class Udp extends MessageToMessageDecoder<DatagramPacket>{

        @Override
        protected void decode(ChannelHandlerContext context, DatagramPacket packet, List<Object> out) {
            RtpMessage message = new RtpMessage();
            ByteBuf in =  packet.content();
            if(in.readableBytes() <= 12){
                return;
            }
            message.setVpxcc(in.readByte());
            message.setMpt(in.readByte());
            message.setSequenceNumber(in.readUnsignedShort());
            message.setTimestamp(in.readUnsignedInt());
            message.setSsrc(in.readUnsignedInt());
            byte[] payload = new byte[in.readableBytes()];
            in.readBytes(payload);
            message.setPayload(payload);
            out.add(message);
        }
    }
}
