package cn.gbtmedia.gbt28181.server.media.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * @author xqs
 */
public class RtpEncoder{

    public static class Tcp extends MessageToByteEncoder<RtpMessage>{

        @Override
        protected void encode(ChannelHandlerContext ctx, RtpMessage message, ByteBuf out) throws Exception {
            byte[] bytes = message.getMessageBytes();
            out.writeShort(bytes.length);
            out.writeBytes(bytes);
        }
    }

    public static class Udp extends MessageToMessageEncoder<RtpMessage> {

        @Override
        protected void encode(ChannelHandlerContext ctx, RtpMessage message, List<Object> out) throws Exception {

        }
    }
}
