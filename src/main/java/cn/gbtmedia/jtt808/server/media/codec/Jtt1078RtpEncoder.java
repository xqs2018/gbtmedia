package cn.gbtmedia.jtt808.server.media.codec;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import java.util.List;


/**
 * @author xqs
 */
@Slf4j
public class Jtt1078RtpEncoder {

    public static class Tcp extends MessageToByteEncoder<Jtt1078RtpMessage>{

        @Override
        protected void encode(ChannelHandlerContext ctx, Jtt1078RtpMessage message, ByteBuf out) throws Exception {
            ByteBuf byteBuf = message.getMessageByteBuf();
            out.writeBytes(byteBuf);
        }
    }

    public static class Udp extends MessageToMessageEncoder<Jtt1078RtpMessage> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Jtt1078RtpMessage message, List<Object> out) throws Exception {

        }
    }

}
