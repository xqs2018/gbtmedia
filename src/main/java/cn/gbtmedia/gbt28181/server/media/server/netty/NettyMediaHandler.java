package cn.gbtmedia.gbt28181.server.media.server.netty;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xqs
 */
@Slf4j
public class NettyMediaHandler {

    public static class Tcp extends SimpleChannelInboundHandler<RtpMessage>{

        private static final AttributeKey<SinglePortMediaServer> SERVER_KEY = AttributeKey.valueOf("SERVER_KEY");

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RtpMessage message){
            String ssrc = message.getSsrcStr();
            SinglePortMediaServer mediaServer = SinglePortMediaServer.getBySsrc(ssrc);
            if(mediaServer == null){
                return;
            }
            Attribute<SinglePortMediaServer> attribute = ctx.channel().attr(SERVER_KEY);
            if(attribute.get() == null){
                attribute.set(mediaServer);
            }
            mediaServer.setChannel(ctx.channel());
            mediaServer.receiveRtpMessage(message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("<<<<< gbt28181Media-tcp 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("<<<<< gbt28181Media-tcp 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
            stop(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            if (e.getMessage() !=null && e.getMessage().toLowerCase().contains("connection".toLowerCase())) {
                log.error("<<<<< gbt28181Media-tcp 消息处理异常 {} 客户端地址 {} ",e.getMessage(),ctx.channel().remoteAddress());
            }else {
                log.error("<<<<< gbt28181Media-tcp 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            log.warn("<<<<< gbt28181Media-tcp 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
            if (evt instanceof IdleStateEvent) {
                ctx.close();
                stop(ctx);
            }
        }

        private static void stop(ChannelHandlerContext ctx){
            Attribute<SinglePortMediaServer> attribute = ctx.channel().attr(SERVER_KEY);
            if(attribute.get() == null){
                return;
            }
            mediaStop(attribute.get());
        }
    }

    public static void mediaStop(MediaServer mediaServer){
        log.info("gbt28181Media mediaStop ssrc {}",mediaServer.getSsrc());
        mediaServer.stop();
    }

    public static class Udp extends SimpleChannelInboundHandler<RtpMessage>{

        private static final ConcurrentHashMap<String,Long> EXPIRE_MAP = new ConcurrentHashMap<>();

        static {
            Runnable checkOnline = ()->{
                List<String> ssrcList = EXPIRE_MAP.keySet().stream().toList();
                for(String ssrc : ssrcList){
                    if(EXPIRE_MAP.get(ssrc) + 1000 * 10 < System.currentTimeMillis()){
                        EXPIRE_MAP.remove(ssrc);
                        log.info("<<<<< gbt28181Media-udp 连接已断开 ssrc {} ",ssrc);
                        SinglePortMediaServer mediaServer = SinglePortMediaServer.getBySsrc(ssrc);
                        if(mediaServer != null){
                            mediaStop(mediaServer);
                        }
                    }
                }
            };
            // 10秒中监测流还在推送不
            SchedulerTask.getInstance().startPeriod("gbt28181MediaUdpCheckOnline",checkOnline,1000 * 10);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RtpMessage message) throws Exception {
            String ssrc = message.getSsrcStr();
            if(!EXPIRE_MAP.containsKey(ssrc)){
                log.info("<<<<< gbt28181Media-udp 连接已开启 ssrc {} 客户端地址 {} ",ssrc, ctx.channel().remoteAddress());
            }
            EXPIRE_MAP.put(ssrc, System.currentTimeMillis());
            SinglePortMediaServer mediaServer = SinglePortMediaServer.getBySsrc(ssrc);
            if(mediaServer == null){
                return;
            }
            mediaServer.receiveRtpMessage(message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

        }
    }
}
