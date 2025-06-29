package cn.gbtmedia.jtt808.server.cmd.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
public class Jtt808Decoder{

    public static class Tcp extends ByteToMessageDecoder{

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            Jtt808Message message = toJtt808Message(in);
            out.add(message);
        }
    }

    public static class Udp extends MessageToMessageDecoder<DatagramPacket> {

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket in, List<Object> out) {
            ByteBuf byteBuf = in.content();
            byteBuf.readByte(); //去掉开头标记
            Jtt808Message message = toJtt808Message(byteBuf);
            InetSocketAddress sender = in.sender();
            out.add(new DefaultAddressedEnvelope<>(message, sender));
        }
    }

    private static Jtt808Message toJtt808Message(ByteBuf in){
        int readableBytes = in.readableBytes();
        byte[] data = new byte[readableBytes];
        in.readBytes(data);

        ByteBuf byteBuf = unescape(data);
        // 生成完整Jtt808消息
        Jtt808Message message = new Jtt808Message();
        message.setMessageId(byteBuf.readShort());
        message.setProperties(byteBuf.readShort());
        // 2019版本
        if(message.getVersionFlag() == 1){
            message.setProtocolVersion(byteBuf.readByte());
            byte[] clientId = new byte[10];
            byteBuf.readBytes(clientId);
            message.setClientId(clientId);
        }else if (message.getVersionFlag() == 0){
            //2013版本
            byte[] clientId = new byte[6];
            byteBuf.readBytes(clientId);
            message.setClientId(clientId);
        }
        message.setSerialNo(byteBuf.readShort());
        if(message.getPacketFlag() != 0){
            message.setPackageTotal(byteBuf.readShort());
            message.setPackageNo(byteBuf.readShort());
        }
        // 读取消息体
        int bodyLength = message.getBodyLength();
        byte[] body = new byte[bodyLength];
        byteBuf.readBytes(body);
        message.setPayload(body);
        // 校验码
        message.setCheckSum(byteBuf.readByte());
        // 验证校验码
        byte checkSum = message.createCheckSum();
        if(message.getCheckSum() != checkSum){
            throw new RuntimeException("消息校验码不正确，消息报文 " + message.toHexString());
        }
        if(log.isDebugEnabled()){
            String clientId = message.getClientIdStr();
            log.debug("<<<<< jtt808 收到消息 clientId: {} \n hexDump[{}]",clientId,message.toHexString());
        }
        return message;
    }


    /**
     * 反转义 规则定义如下
     * 先对 0x7d 0x01 进行转义,转换为固定一字节数据: 0x7d
     * 再对 0x7d 0x02 进行转义,转换为固定两字节数据: 0x7e
     */
    private static ByteBuf unescape(byte[] data) {
        int len = data.length;
        ByteBuf byteBuf = Unpooled.buffer();
        for (int i = 0; i < len; i++) {
            if(data[i] == 0x7d && (i == len - 1)){
                byteBuf.writeByte(data[i]);
                throw new RuntimeException("消息转义最后一位出现 0x7d 消息报文 " + ByteBufUtil.hexDump(data));
            }
            if (data[i] == 0x7d && data[i + 1] == 0x01) {
                byteBuf.writeByte(0x7d);
                i++;
            } else if (data[i] == 0x7d && data[i + 1] == 0x02) {
                byteBuf.writeByte(0x7e);
                i++;
            } else {
                byteBuf.writeByte(data[i]);
            }
        }
        return byteBuf;
    }
}
