package cn.gbtmedia.jtt808.server.cmd;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.event.ClientEvent;
import cn.gbtmedia.jtt808.server.cmd.receive.Jtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
public class Jtt808Handler{

    public static class Tcp extends SimpleChannelInboundHandler<Jtt808Message>{

        private static final AttributeKey<ClientSession> SESSION_KEY = AttributeKey.valueOf("session");

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Jtt808Message message) throws Exception {
            String clientId = message.getClientIdStr();
            // 获取当前关联的会话
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                session = getSession(ctx);
                session.setChannel(ctx.channel());
                session.setCreateTime(System.currentTimeMillis());
                session.setSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());
                session.setClientId(clientId);
                session.setVersion((message.getVersionFlag()==1?2019:2013));
                log.info("<<<<< jtt808-tcp 开始数据接收 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
            }
            session.setLastAccessedTime(System.currentTimeMillis());
            Jtt808SessionManager.getInstance().putClientSession(session);
            // 处理消息
            Jtt808Receive.handle(session, message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808-tcp 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
            Attribute<ClientSession> attribute = ctx.channel().attr(SESSION_KEY);
            attribute.set(new ClientSession());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808-tcp 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
            offline(getSession(ctx));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            log.error("<<<<< jtt808-tcp 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            log.warn("<<<<< jtt808-tcp 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
            if (evt instanceof IdleStateEvent) {
                ctx.close();
                offline(getSession(ctx));
            }
        }

        private static ClientSession getSession(ChannelHandlerContext ctx) {
            Attribute<ClientSession> attribute = ctx.channel().attr(SESSION_KEY);
            if(attribute != null){
                return attribute.get();
            }
            throw new RuntimeException("channel session is null");
        }

    }

    public static void offline(ClientSession session){
        String clientId = session.getClientId();
        if(clientId == null){
            return;
        }
        SpringUtil.publishEvent(new ClientEvent(Jtt808Handler.class,clientId,0));
    }


    public static class Udp extends SimpleChannelInboundHandler<AddressedEnvelope<Jtt808Message, InetSocketAddress>> {

        static {
            Runnable checkOnline = ()->{
                Jtt808SessionManager sessionManager = Jtt808SessionManager.getInstance();
                List<ClientSession> sessionList = sessionManager.getAllClientSession();
                for(ClientSession session : sessionList){
                    if(session.getLastAccessedTime() + 1000 * 60 < System.currentTimeMillis()){
                        log.info("<<<<< jtt808-udp 连接已断开 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
                        offline(session);
                    }
                }
            };
            // 1分钟检测一下心跳
            SchedulerTask.getInstance().startPeriod("jtt808UdpCheckOnline",checkOnline,1000 * 60);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, AddressedEnvelope<Jtt808Message, InetSocketAddress> lopeMsg) throws Exception {
            Jtt808Message message = lopeMsg.content();
            String clientId = message.getClientIdStr();
            // 获取当前关联的会话
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                session = new ClientSession();
                session.setChannel(ctx.channel());
                session.setCreateTime(System.currentTimeMillis());
                session.setClientId(clientId);
                session.setVersion(message.getVersionFlag()==1?2019:2013);
                log.info("<<<<< jtt808-udp 开始数据接收 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
            }
            session.setSocketAddress(lopeMsg.sender());
            session.setLastAccessedTime(System.currentTimeMillis());
            Jtt808SessionManager.getInstance().putClientSession(session);
            // 处理消息
            Jtt808Receive.handle(session, message);
    }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
        }
    }
}
