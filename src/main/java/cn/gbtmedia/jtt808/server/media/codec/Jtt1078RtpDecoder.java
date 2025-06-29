package cn.gbtmedia.jtt808.server.media.codec;

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
public class Jtt1078RtpDecoder {

    public static class Tcp extends ByteToMessageDecoder{

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
            in.markReaderIndex();
            if(in.readableBytes() < 17 ){
                return;
            }
            Jtt1078RtpMessage message = new Jtt1078RtpMessage();
            int begin = in.readInt();
            if(begin != message.getBegin()) {
                throw new RuntimeException("Jtt1078RtpMessage begin is " + Integer.toHexString(begin));
            }
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
            int bodyLength = message.getBodyLength();
            if( bodyLength != 0 && bodyLength > in.readableBytes()){
                in.resetReaderIndex();
                return;
            }
            byte[] payload = new byte[message.getBodyLength()];
            in.readBytes(payload);
            message.setPayload(payload);
            out.add(message);
        }
    }

    public static class Udp extends MessageToMessageDecoder<DatagramPacket> {

        @Override
        protected void decode(ChannelHandlerContext context, DatagramPacket in, List<Object> out) throws Exception {
            ByteBuf byteBuf = in.content();
            Jtt1078RtpMessage message = Jtt1078RtpMessage.toJtt1078RtpMessage(byteBuf);
            out.add(message);
        }
    }


}
