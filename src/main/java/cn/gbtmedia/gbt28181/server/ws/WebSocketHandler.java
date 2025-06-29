package cn.gbtmedia.gbt28181.server.ws;

import cn.gbtmedia.common.util.AudioUtil;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        // 文本消息
        if (message instanceof TextWebSocketFrame text) {
            log.info("<<<<< gbt28181Ws text message not support {}",text.text());
            return;
        }
        // 二进制消息
        if (message instanceof BinaryWebSocketFrame binary) {
            WsContext wsContext = getWsContext(ctx);
            if(wsContext == null){
                return;
            }
            ServerInvite talk = wsContext.getServerInvite();
            ClientInvite broadcast = wsContext.getClientInvite();

            byte[] pcm = ByteBufUtil.getBytes(binary.content());
            byte[] pcma = AudioUtil.pcmToPcma(pcm);

            int sequenceNumber = wsContext.getSequenceNumber().getAndIncrement();
            RtpMessage rtpMessage = new RtpMessage();
            rtpMessage.setMpt((byte) 8);
            rtpMessage.setSequenceNumber(sequenceNumber);
            rtpMessage.setTimestamp(System.currentTimeMillis());
            rtpMessage.setPayload(pcma);

            // 发送语音到设备
            if(talk != null){
                if(sequenceNumber == 1){
                    log.info(">>>>> gbt28181Ws start send talk deviceId {} channelId {} mediaTransport {}",
                            talk.getDeviceId(), talk.getChannelId(),talk.getMediaTransport());
                }
                rtpMessage.setSsrc(Integer.parseInt(talk.getSsrc()));
                talk.getMediaServer().sendRtpMessage(rtpMessage);
                return;
            }

            // 发送语音到设备
            if(broadcast != null){
                if(sequenceNumber == 1){
                    log.info(">>>>> gbt28181Ws start send broadcast deviceId {} channelId {} mediaTransport {}",
                            broadcast.getDeviceId(), broadcast.getChannelId(),broadcast.getMediaTransport());
                }
                rtpMessage.setSsrc(Integer.parseInt(broadcast.getSsrc()));
                broadcast.getMediaClient().sendRtpMessage(rtpMessage);
                return;
            }

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("<<<<< gbt28181Ws 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        log.info("<<<<< gbt28181Ws 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
        // 防止直接关闭了，不是通过手动关的
        WsContext wsContext = getWsContext(ctx);
        if(wsContext == null){
            return;
        }
        ServerInvite talk = wsContext.getServerInvite();
        ClientInvite broadcast = wsContext.getClientInvite();
        if(talk != null){
            // 对讲会有语音上来， 移除对应的监听
            if(wsContext.getListenerKey() != null){
                talk.getMediaServer().removeRtpMessageListener(wsContext.getListenerKey());
            }
            talk.getMediaServer().stop();
        }
        if(broadcast != null){
            // 广播可会有语音上来， 移除对应的监听
            if(wsContext.getListenerKey() != null){
                broadcast.getMediaClient().removeRtpMessageListener(wsContext.getListenerKey());
            }
            broadcast.getMediaClient().stop();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("<<<<< gbt28181Ws 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.warn("<<<<< gbt28181Ws 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
        if (evt instanceof IdleStateEvent) {
            ctx.close();
        }
        // 链接握手事件
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete complete) {
            String uri = complete.requestUri();
            log.info("<<<<< gbt28181Ws 握手成功 uri {}",uri);
            WsContext wsContext = new WsContext();
            Attribute<WsContext> attribute = ctx.channel().attr(WS_CONTEXT);
            attribute.set(wsContext);
            //判断连接类型
            String[] param = uri.substring("/chat/".length()).split("_");
            String type = param[0];
            String key = param[1];
            if(type.equals("talk")){
                ServerInvite invite = SipSessionManger.getInstance().getServerInviteByCallId(key);
                if(invite == null){
                    log.warn("no active talk  callId {} ", key);
                    return;
                }
                wsContext.setServerInvite(invite);
                // 对讲会有语音上来，设置监听
                MediaServer mediaServer = invite.getMediaServer();
                String listenerKey = "chat_listener_" + ctx.channel().id().asLongText();
                wsContext.setListenerKey(listenerKey);
                mediaServer.addRtpMessageListener(listenerKey, sendToWsClient(ctx.channel()));
            }
            if(type.equals("broadcast")){
                ClientInvite invite = SipSessionManger.getInstance().getClientInvite(key);
                if(invite == null){
                    log.warn("no active broadcast callId {} ", key);
                    return;
                }
                // 广播可能会有语音上来，设置监听
                MediaClient mediaClient = invite.getMediaClient();
                String listenerKey = "chat_listener_" + ctx.channel().id().asLongText();
                wsContext.setListenerKey(listenerKey);
                mediaClient.addRtpMessageListener(listenerKey, sendToWsClient(ctx.channel()));
                wsContext.setClientInvite(invite);
            }
        }
    }

    // 发送语音到ws客户端
    private Consumer<RtpMessage> sendToWsClient(Channel channel){
        return rtpMessage -> {
            try {
                if(rtpMessage.getPt() != 8){
                    return;
                }
                byte[] payload = rtpMessage.getPayload();
                byte[] pcm = AudioUtil.adpcmToPcm(payload);
                BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(pcm));
                channel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.error("sendToWsClient error",future.cause());
                    }
                });
            }catch (Exception ex){
                log.error("sendToWsClient ex",ex);
            }
        };
    }

    @Data
    public static class WsContext{
        private ServerInvite serverInvite;
        private ClientInvite clientInvite;
        private AtomicInteger sequenceNumber = new AtomicInteger(0);
        private String listenerKey;
    }

    private static final AttributeKey<WsContext> WS_CONTEXT = AttributeKey.valueOf("WsContext");

    public static WsContext getWsContext(ChannelHandlerContext ctx) {
        Attribute<WsContext> attribute = ctx.channel().attr(WS_CONTEXT);
        if(attribute != null){
            return attribute.get();
        }
        return null;
    }

}
