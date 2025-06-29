package cn.gbtmedia.jtt808.server.flv;

import cn.gbtmedia.jtt808.server.media.stream.StreamManger;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xqs
 */
@Slf4j
public class FlvHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final AttributeKey<FlvSubscriber> SESSION_KEY = AttributeKey.valueOf("session");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest message) throws Exception {
        String uri = message.uri();
        // 播发请求返回flv
        if (uri.startsWith("/video/")){
            String mediaKey = uri.substring("/video/".length()).replace(".flv","");
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers()
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    .set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv")
                    .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
                    .set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                    .set(HttpHeaderNames.SERVER, "jtt808Media");
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("response error",future.cause());
                }else {
                    FlvSubscriber subscriber = getSubscriber(ctx);
                    subscriber.setMediaKey(mediaKey);
                    StreamManger.getInstance().subscribe(subscriber);
                }
            });
            return;
        }
        // 默认返回 401
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
        log.info("<<<<< jtt808Flv 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
        Attribute<FlvSubscriber> attribute = ctx.channel().attr(SESSION_KEY);
        FlvSubscriber subscriber = new FlvSubscriber();
        subscriber.setChannel(ctx.channel());
        attribute.set(subscriber);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("<<<<< jtt808Flv 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
        FlvSubscriber subscriber = getSubscriber(ctx);
        if(subscriber.getMediaKey() != null){
            StreamManger.getInstance().unSubscribe(getSubscriber(ctx));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("<<<<< jtt808Flv 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.warn("<<<<< jtt808Flv 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
        if (evt instanceof IdleStateEvent) {
            ctx.close();
            FlvSubscriber subscriber = getSubscriber(ctx);
            if(subscriber.getMediaKey() != null){
                StreamManger.getInstance().unSubscribe(getSubscriber(ctx));
            }
        }
    }

    public static FlvSubscriber getSubscriber(ChannelHandlerContext ctx) {
        Attribute<FlvSubscriber> attribute = ctx.channel().attr(SESSION_KEY);
        if(attribute != null){
            return attribute.get();
        }
        throw new RuntimeException("channel subscriber is null");
    }
}
