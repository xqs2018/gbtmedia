package cn.gbtmedia.jtt808.server.alarm.codec;

import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
public class AlarmFileEncoder {

    public static class Tcp extends MessageToByteEncoder<AlarmFileMessage808>{

        @Override
        protected void encode(ChannelHandlerContext ctx, AlarmFileMessage808 message, ByteBuf out) throws Exception {
            ByteBuf byteBuf = toByteBuf(message);
            out.writeBytes(byteBuf);
        }

    }

    public static class Udp extends MessageToMessageEncoder<AlarmFileMessage808> {

        @Override
        protected void encode(ChannelHandlerContext ctx, AlarmFileMessage808 message, List<Object> out) throws Exception {
            ByteBuf byteBuf = toByteBuf(message);
            // 获取客户端ip和端口
            String clientId = message.getClientIdStr();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            out.add(new DatagramPacket(byteBuf, (InetSocketAddress) session.getSocketAddress()));
        }
    }

    private static ByteBuf toByteBuf(AlarmFileMessage808 message){
        ByteBuf buffer = Unpooled.buffer();
        byte[] messageBytes = message.getMessageBytes();
        int length = messageBytes.length - 2;
        byte[] data = new byte[length];
        // 转义 不含开始和结束
        System.arraycopy(messageBytes, 1, data, 0, length);
        ByteBuf escape = escape(data);
        buffer.writeByte(message.getBegin());
        buffer.writeBytes(escape);
        buffer.writeByte(message.getEnd());
        if(log.isDebugEnabled()){
            String clientId = message.getClientIdStr();
            log.debug(">>>>> jtt808AlarmFile 发送消息 clientId: {} \n hexDump[{}]",clientId, ByteBufUtil.hexDump(buffer));
        }
        return buffer;
    }

    /**
     * 转义 规则定义如下
     * 先对 0x7d 进行转义,转换为固定两字节数据:0x7d 0x01
     * 再对 0x7e 进行转义,转换为固定两字节数据:0x7d 0x02
     */
    private static ByteBuf escape(byte[] data) {
        ByteBuf byteBuf = Unpooled.buffer();
        for (byte b : data) {
            if (b == 0x7e) {
                byteBuf.writeByte(0x7d);
                byteBuf.writeByte(0x02);
            } else if (b == 0x7d) {
                byteBuf.writeByte(0x7d);
                byteBuf.writeByte(0x01);
            } else {
                byteBuf.writeByte(b);
            }
        }
        return byteBuf;
    }
}
