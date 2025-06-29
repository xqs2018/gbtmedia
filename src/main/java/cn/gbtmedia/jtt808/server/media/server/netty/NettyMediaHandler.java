package cn.gbtmedia.jtt808.server.media.server.netty;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import cn.gbtmedia.jtt808.server.media.server.MediaServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xqs
 */
@Slf4j
public class NettyMediaHandler{

    public static class Tcp extends SimpleChannelInboundHandler<Jtt1078RtpMessage>{

        private static final AttributeKey<SinglePortMediaServer> SERVER_KEY = AttributeKey.valueOf("SERVER_KEY");

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Jtt1078RtpMessage message){
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            int port = socketAddress.getPort();
            String mediaType = port == ServerConfig.getInstance().getJtt808().getMediaSinglePlayPort() ? "play":"playback";
            String clientId = message.getClientIdStr();
            int channelNo = message.getChannelNo();
            String mediaKey = String.format("%s_%s_%s",mediaType,clientId,channelNo);
            SinglePortMediaServer mediaServer = SinglePortMediaServer.getByMediaKey(mediaKey);
            if(mediaServer == null){
                return;
            }
            Attribute<SinglePortMediaServer> attribute = ctx.channel().attr(SERVER_KEY);
            if(attribute.get() == null){
                attribute.set(mediaServer);
            }
            mediaServer.setChannel(ctx.channel());
            mediaServer.receiveJtt1078RtpMessage(message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808Media-tcp 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808Media-tcp 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
            stop(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            if (e.getMessage() !=null && e.getMessage().toLowerCase().contains("connection".toLowerCase())) {
                log.error("<<<<< jtt808Media-tcp 消息处理异常 {} 客户端地址 {} ",e.getMessage(),ctx.channel().remoteAddress());
            }else {
                log.error("<<<<< jtt808Media-tcp 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            log.warn("<<<<< jtt808Media-tcp 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
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
        log.info("jtt808Media mediaStop mediaKey {}",mediaServer.getMediaKey());
        mediaServer.stop();
    }

    public static class Udp extends SimpleChannelInboundHandler<Jtt1078RtpMessage>{

        private static final ConcurrentHashMap<String,Long> EXPIRE_MAP = new ConcurrentHashMap<>();

        static {
            Runnable checkOnline = ()->{
                List<String> mediaKeyList = EXPIRE_MAP.keySet().stream().toList();
                for(String mediaKey : mediaKeyList){
                    if(EXPIRE_MAP.get(mediaKey) + 1000 * 10 < System.currentTimeMillis()){
                        EXPIRE_MAP.remove(mediaKey);
                        log.info("<<<<< jtt808Media-udp 连接已断开 mediaKey {} ",mediaKey);
                        SinglePortMediaServer mediaServer = SinglePortMediaServer.getByMediaKey(mediaKey);
                        if(mediaServer != null){
                            mediaStop(mediaServer);
                        }
                    }
                }
            };
            // 10秒中监测流还在推送不
            SchedulerTask.getInstance().startPeriod("jtt808MediaUdpCheckOnline",checkOnline,1000 * 10);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Jtt1078RtpMessage message) throws Exception {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            int port = socketAddress.getPort();
            String mediaType = port == ServerConfig.getInstance().getJtt808().getMediaSinglePlayPort() ? "play":"playback";
            String clientId = message.getClientIdStr();
            int channelNo = message.getChannelNo();
            String mediaKey = String.format("%s_%s_%s",mediaType,clientId,channelNo);
            if(!EXPIRE_MAP.containsKey(mediaKey)){
                log.info("<<<<< jtt808Media-udp 连接已开启 mediaKey {} 客户端地址 {} ",mediaKey, ctx.channel().remoteAddress());
            }
            EXPIRE_MAP.put(mediaKey, System.currentTimeMillis());
            SinglePortMediaServer mediaServer = SinglePortMediaServer.getByMediaKey(mediaKey);
            if(mediaServer == null){
                return;
            }
            mediaServer.receiveJtt1078RtpMessage(message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {

        }
    }

}
