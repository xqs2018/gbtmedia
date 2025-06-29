package cn.gbtmedia.jtt808.server.alarm;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessageData;
import cn.gbtmedia.jtt808.server.alarm.receive.AlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSessionManager;
import cn.hutool.core.util.ObjectUtil;
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
public class AlarmFileHandler {


    public static class Tcp extends SimpleChannelInboundHandler<Object>{

        private static final AttributeKey<AlarmFileSession> SESSION_KEY = AttributeKey.valueOf("session");

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
            // 判断必须先发送808消息
            AlarmFileSession session;
            if(message instanceof AlarmFileMessageData){
                session = getSession(ctx);
                if(ObjectUtil.isEmpty(session.getClientId())){
                    log.error("<<<<< jtt808AlarmFile-tcp 必须先发送808消息 客户端地址 {}",ctx.channel().remoteAddress());
                    return;
                }
            }else {
                AlarmFileMessage808 message808 = (AlarmFileMessage808) message;
                String clientId = message808.getClientIdStr();
                session = AlarmFileSessionManager.getInstance().getSession(clientId);
                if(session == null){
                    session = getSession(ctx);
                    session.setChannel(ctx.channel());
                    session.setCreateTime(System.currentTimeMillis());
                    session.setSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());
                    session.setClientId(clientId);
                    session.setVersion(message808.getVersionFlag()==1?2019:2013);
                    log.info("<<<<< jtt808AlarmFile-tcp 开始数据接收 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
                }
            }
            session.setLastAccessedTime(System.currentTimeMillis());
            AlarmFileSessionManager.getInstance().putSession(session);
            // 处理消息
            AlarmFileReceive.handle(session, message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808AlarmFile-tcp 连接已开启 客户端地址 {} ", ctx.channel().remoteAddress());
            Attribute<AlarmFileSession> attribute = ctx.channel().attr(SESSION_KEY);
            attribute.set(new AlarmFileSession());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("<<<<< jtt808AlarmFile-tcp 连接已断开 客户端地址 {} ", ctx.channel().remoteAddress());
            offline(getSession(ctx));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            log.error("<<<<< jtt808AlarmFile-tcp 消息处理异常 客户端地址 {} ",ctx.channel().remoteAddress() , e);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            log.warn("<<<<< jtt808AlarmFile-tcp 用户事件触发 {} 客户端地址 {} ",evt.getClass().getSimpleName(), ctx.channel().remoteAddress());
            if (evt instanceof IdleStateEvent) {
                ctx.close();
                offline(getSession(ctx));
            }
        }

        private static AlarmFileSession getSession(ChannelHandlerContext ctx) {
            Attribute<AlarmFileSession> attribute = ctx.channel().attr(SESSION_KEY);
            if(attribute != null){
                return attribute.get();
            }
            throw new RuntimeException("channel session is null");
        }
    }

    public static void offline(AlarmFileSession session){
        String clientId = session.getClientId();
        if(clientId == null){
            return;
        }
        AlarmFileSessionManager.getInstance().removeSession(clientId);
    }

    public static class Udp extends SimpleChannelInboundHandler<AddressedEnvelope<Object, InetSocketAddress>> {

        static {
            Runnable checkOnline = ()->{
                AlarmFileSessionManager sessionManager = AlarmFileSessionManager.getInstance();
                List<AlarmFileSession> sessionList = sessionManager.getAlleSession();
                for(AlarmFileSession session : sessionList){
                    if(session.getLastAccessedTime() + 1000 * 60 < System.currentTimeMillis()){
                        log.info("<<<<< jtt808AlarmFile-udp 连接已断开 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
                        offline(session);
                    }
                }
            };
            // 1分钟检测一下心跳
            SchedulerTask.getInstance().startPeriod("jtt808UdpCheckOnline",checkOnline,1000 * 60);
        }


        @Override
        protected void channelRead0(ChannelHandlerContext ctx, AddressedEnvelope<Object, InetSocketAddress> lopeMsg) throws Exception {
            // TODO 如果IP端口变了就不行了
            Object message = lopeMsg.content();
            AlarmFileSession session = AlarmFileSessionManager.getInstance().getSessionBySocketAddress(lopeMsg.sender());
            // 判断必须先发送808消息
            if(session == null && message instanceof AlarmFileMessageData){
                log.error("<<<<< jtt808AlarmFile-udp 必须先发送808消息 客户端地址 {}",lopeMsg.sender());
                return;
            }else if(session == null){
                AlarmFileMessage808 message808 = (AlarmFileMessage808) message;
                String clientId = message808.getClientIdStr();
                session = new AlarmFileSession();
                session.setChannel(ctx.channel());
                session.setCreateTime(System.currentTimeMillis());
                session.setClientId(clientId);
                session.setVersion(message808.getVersionFlag()==1?2019:2013);
                log.info("<<<<< jtt808AlarmFile-udp 开始数据接收 clientId {} 客户端地址 {} ",session.getClientId(), session.getSocketAddress());
            }
            session.setSocketAddress(lopeMsg.sender());
            session.setLastAccessedTime(System.currentTimeMillis());
            AlarmFileSessionManager.getInstance().putSession(session);
            // 处理消息
            AlarmFileReceive.handle(session, message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
        }
    }

}
