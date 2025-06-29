package cn.gbtmedia.gbt28181.server.flv;

import cn.gbtmedia.gbt28181.server.flv.transcode.TranscodeManger;
import cn.gbtmedia.gbt28181.server.media.stream.StreamManger;
import cn.hutool.core.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

/**
 * @author xqs
 */
@Slf4j
public class FlvHandler extends SimpleChannelInboundHandler<Object > {

    private static final AttributeKey<FlvSubscriber> SESSION_KEY = AttributeKey.valueOf("session");

    private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_KEY = AttributeKey.valueOf("handshaker");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        if (message instanceof CloseWebSocketFrame) {
            log.info("<<<<< gbt28181Flv 收到客户端的关闭帧 客户端地址 {} ", ctx.channel().remoteAddress());
            WebSocketServerHandshaker handshaker = ctx.channel().attr(HANDSHAKER_KEY).get();
            if(handshaker != null){
                handshaker.close(ctx,((CloseWebSocketFrame) message).retain());
                return;
            }
            ctx.channel().close();
            return;
        }
        FullHttpRequest request;
        if (message instanceof FullHttpRequest) {
            request = (FullHttpRequest) message;
        }else {
            log.warn("unknown message {}",message);
            return;
        }
        String uri = request.uri();
        log.info("gbt28181Flv player uri {} ",uri);
        if(uri.contains("?")){
            uri = uri.substring(0,uri.indexOf("?"));
            log.info("gbt28181Flv player uri {} ",uri);
        }
        // 播放地址获取ssrc和转码类型
        String ssrc = null;
        String transcode = null;
        if (uri.startsWith("/video/")){
            ssrc = uri.substring("/video/".length()).replace(".flv","");
            transcode = "";
        }
        if (uri.startsWith("/video/origin/")){
            ssrc = uri.substring("/video/origin/".length()).replace(".flv","");
            transcode = "origin";
        }
        if (uri.startsWith("/video/720p/")){
            ssrc = uri.substring("/video/720p/".length()).replace(".flv","");
            transcode = "720p";
        }
        if (uri.startsWith("/video/360p/")){
            ssrc = uri.substring("/video/360p/".length()).replace(".flv","");
            transcode = "360p";
        }
        // webSocket连接
        if(request.headers().contains(HttpHeaderNames.UPGRADE) &&
                request.headers().get(HttpHeaderNames.UPGRADE).equalsIgnoreCase("websocket")){
            log.info("webSocket flv upgrade ...");
            if(ObjectUtil.isNotEmpty(ssrc)){
                String location =  "ws://" + request.headers().get(HOST) + request.uri();
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(location, null, true,5 * 1024 * 1024);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    ctx.channel().attr(HANDSHAKER_KEY).set(handshaker);
                    // 完成握手
                    HttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    rsp.headers().set(HttpHeaderNames.SERVER, "gbt28181Media");
                    DefaultChannelPromise channelPromise = new DefaultChannelPromise(ctx.channel());
                    FlvSubscriber subscriber = getSubscriber(ctx);
                    subscriber.setSsrc(ssrc);
                    subscriber.setTranscode(transcode);
                    subscriber.setSubscriberType(2);
                    handshaker.handshake(ctx.channel(), request, rsp.headers(), channelPromise).addListener(future -> {
                        if (future.isSuccess()) {
                            subscribe(subscriber);
                        }
                    });
                }
                return;
            }
        }
        // 普通连接
        if(ObjectUtil.isNotEmpty(ssrc)){
            FlvSubscriber subscriber = getSubscriber(ctx);
            subscriber.setSsrc(ssrc);
            subscriber.setTranscode(transcode);
            subscriber.setSubscriberType(1);
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers()
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    .set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv")
                    .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
                    .set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                    .set(HttpHeaderNames.SERVER, "gbt28181Media");
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("response error",future.cause());
                }else {
                    subscribe(subscriber);
                }
            });
            return;
        }

        // 默认返回 401
        log.error("gbt28181Flv response 401");
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED,
                Unpooled.copiedBuffer("Unauthorized", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("response error",future.cause());
            }
            future.channel().close();
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("<<<<< gbt28181Flv 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
        Attribute<FlvSubscriber> attribute = ctx.channel().attr(SESSION_KEY);
        FlvSubscriber subscriber = new FlvSubscriber();
        subscriber.setChannel(ctx.channel());
        attribute.set(subscriber);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("<<<<< gbt28181Flv 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
        FlvSubscriber subscriber = getSubscriber(ctx);
        if(subscriber.getSsrc() != null){
            unSubscribe(subscriber);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("<<<<< gbt28181Flv 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.warn("<<<<< gbt28181Flv 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
        if (evt instanceof IdleStateEvent) {
            ctx.close();
            FlvSubscriber subscriber = getSubscriber(ctx);
            if(subscriber.getSsrc() != null){
                unSubscribe(subscriber);
            }
        }
    }

    private static FlvSubscriber getSubscriber(ChannelHandlerContext ctx) {
        Attribute<FlvSubscriber> attribute = ctx.channel().attr(SESSION_KEY);
        if(attribute != null){
            return attribute.get();
        }
        throw new RuntimeException("channel subscriber is null");
    }

    private static void subscribe(FlvSubscriber subscriber){
        if(ObjectUtil.isEmpty(subscriber.getTranscode())){
            StreamManger.getInstance().subscribe(subscriber);
        }else {
            TranscodeManger.getInstance().subscribe(subscriber);
        }
    }

    private static void unSubscribe(FlvSubscriber subscriber){
        if(ObjectUtil.isEmpty(subscriber.getTranscode())){
            StreamManger.getInstance().unSubscribe(subscriber);
        }else {
            TranscodeManger.getInstance().unSubscribe(subscriber);
        }
    }
}
