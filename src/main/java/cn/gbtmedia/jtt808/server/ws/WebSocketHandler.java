package cn.gbtmedia.jtt808.server.ws;

import cn.gbtmedia.common.util.AudioUtil;
import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.jtt808.server.cmd.session.ClientMedia;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xqs
 */
@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private static final AttributeKey<ClientMedia> CLIENT_MEDIA = AttributeKey.valueOf("clientMedia");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        // 文本消息
        if (message instanceof TextWebSocketFrame text) {
            log.info("<<<<< jtt808Ws text message not support {}",text.text());
            return;
        }
        // 二进制消息
        if (message instanceof BinaryWebSocketFrame binary) {
            ClientMedia clientMedia = getClientMedia(ctx);

            // 发送语音到设备
            if(clientMedia != null){
                byte[] pcm = ByteBufUtil.getBytes(binary.content());
                byte[] g711a = AudioUtil.pcmToG711a(pcm);
                int sequenceNumber = clientMedia.getSequenceNumber().getAndIncrement();
                if(sequenceNumber == 1){
                    log.info(">>>>> jtt808Ws start send g711a to {}_{}", clientMedia.getClientId(), clientMedia.getChannelNo());
                }
                Jtt1078RtpMessage jtt1078RtpMessage = new Jtt1078RtpMessage();
                jtt1078RtpMessage.setMpt((byte) 6);
                jtt1078RtpMessage.setSequenceNumber(sequenceNumber);
                jtt1078RtpMessage.setClientId(ByteUtil.strToBCD(clientMedia.getClientId()));
                jtt1078RtpMessage.setChannelNo(clientMedia.getChannelNo());
                jtt1078RtpMessage.setDataAndPackType((byte) ((3 << 4)));
                jtt1078RtpMessage.setTimestamp(System.currentTimeMillis());
                jtt1078RtpMessage.setBodyLength(g711a.length);
                jtt1078RtpMessage.setPayload(g711a);
                clientMedia.getMediaServer().sendJtt1078RtpMessage(jtt1078RtpMessage);
                return;
            }
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("<<<<< jtt808Ws 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("<<<<< jtt808Ws 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("<<<<< jtt808Ws 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.warn("<<<<< jtt808Ws 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
        if (evt instanceof IdleStateEvent) {
            ctx.close();
        }
        // 链接握手事件
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete complete) {
            String uri = complete.requestUri();
            log.info("<<<<< jtt808Ws 握手成功 uri {}",uri);
            String mediaKey  = uri.substring("/chat/".length());
            ClientMedia clientMedia = Jtt808SessionManager.getInstance().getClientMedia(mediaKey);
            if(clientMedia != null){
                Attribute<ClientMedia> attribute = ctx.channel().attr(CLIENT_MEDIA);
                attribute.set(clientMedia);
            }
            if(clientMedia == null){
                log.warn("no active clientMedia mediaKey {} ",mediaKey);
            }
        }
    }

    public static ClientMedia getClientMedia(ChannelHandlerContext ctx) {
        Attribute<ClientMedia> attribute = ctx.channel().attr(CLIENT_MEDIA);
        if(attribute != null){
            return attribute.get();
        }
        return null;
    }
}
