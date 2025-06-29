package cn.gbtmedia.jtt808.server.cmd.send;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T1003;
import cn.gbtmedia.jtt808.server.cmd.message.T1205;
import cn.gbtmedia.jtt808.server.cmd.message.T9003;
import cn.gbtmedia.jtt808.server.cmd.message.T9101;
import cn.gbtmedia.jtt808.server.cmd.message.T9102;
import cn.gbtmedia.jtt808.server.cmd.message.T9201;
import cn.gbtmedia.jtt808.server.cmd.message.T9202;
import cn.gbtmedia.jtt808.server.cmd.message.T9205;
import cn.gbtmedia.jtt808.server.cmd.message.T9206;
import cn.gbtmedia.jtt808.server.cmd.message.T9207;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import cn.hutool.core.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.Future;

/**
 * @author xqs
 */
@Slf4j
@Component
public class Jtt1078SendImpl implements Jtt1078Send {

    @Override
    public CmdResult<T0001> sendT9101(T9101 t9101){
        log.info("sendT9101 clientId {} params {}",t9101.getClientId(),t9101);
        try {
            String clientId = t9101.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9101.getIpLength());
            payload.writeBytes(t9101.getIp().getBytes());
            payload.writeShort(t9101.getTcpPort());
            payload.writeShort(t9101.getUdpPort());
            payload.writeByte(t9101.getChannelNo());
            payload.writeByte(t9101.getMediaType());
            payload.writeByte(t9101.getStreamType());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9101;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9101 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9101 error",f.cause());
                }
            });

            T0001 t1001 = (T0001) future.get();
            if(t1001 == null){
                throw new RuntimeException("sendT9101 timeOut");
            }
            return CmdResult.success(t1001);
        }catch (Exception ex){
            log.error("sendT9101 ex params {}",t9101,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT9102(T9102 t9102) {
        log.info("sendT9102 clientId {} params {}",t9102.getClientId(),t9102);
        try {
            String clientId = t9102.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9102.getChannelNo());
            payload.writeByte(t9102.getCommand());
            payload.writeByte(t9102.getCloseType());
            payload.writeByte(t9102.getStreamType());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9102;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9102 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9102 error",f.cause());
                }
            });

            T0001 t1001 = (T0001) future.get();
            if(t1001 == null){
                throw new RuntimeException("sendT9102 timeOut");
            }
            return CmdResult.success(t1001);
        }catch (Exception ex){
            log.error("sendT9102 ex params {}",t9102,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T1003> sendT9003(T9003 t9003) {
        log.info("sendT9003 clientId {} params {}",t9003.getClientId(),t9003);
        try {
            String clientId = t9003.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            // 消息id
            int requestMessageId = 0x9003;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调 ，这个返回没有序列号
            String callKey = clientId +"_" + 0x1003;
            log.info("sendT9003 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9003 error",f.cause());
                }
            });
            T1003 t1003 = (T1003) future.get();
            if(t1003 == null){
                throw new RuntimeException("sendT9003 timeOut");
            }
            return CmdResult.success(t1003);
        }catch (Exception ex){
            log.error("sendT9003 ex params {}",t9003,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T1205> sendT9205(T9205 t9205) {
        log.info("sendT9205 clientId {} params {}",t9205.getClientId(),t9205);
        try {
            String clientId = t9205.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9205.getChannelNo());
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9205.getStartTime(),"000000000000")));
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9205.getEndTime(),"000000000000")));
            payload.writeInt(t9205.getWarnBit1());
            payload.writeInt(t9205.getWarnBit2());
            payload.writeByte(t9205.getMediaType());
            payload.writeByte(t9205.getStreamType());
            payload.writeByte(t9205.getStorageType());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9205;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9205 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9205 error",f.cause());
                }
            });

            T1205 t1205 = (T1205) future.get();
            if(t1205 == null){
                throw new RuntimeException("sendT9205 timeOut");
            }
            return CmdResult.success(t1205);
        }catch (Exception ex){
            log.error("sendT9205 ex params {}",t9205,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T1205> sendT9201(T9201 t9201) {
        log.info("sendT9201 clientId {} params {}",t9201.getClientId(),t9201);
        try {
            String clientId = t9201.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9201.getIpLength());
            payload.writeBytes(t9201.getIp().getBytes());
            payload.writeShort(t9201.getTcpPort());
            payload.writeShort(t9201.getUdpPort());
            payload.writeByte(t9201.getChannelNo());
            payload.writeByte(t9201.getMediaType());
            payload.writeByte(t9201.getStreamType());
            payload.writeByte(t9201.getStorageType());
            payload.writeByte(t9201.getPlaybackMode());
            payload.writeByte(t9201.getPlaybackSpeed());
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9201.getStartTime(),"000000000000")));
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9201.getEndTime(),"000000000000")));
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9201;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9201 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9201 error",f.cause());
                }
            });

            T1205 t1205 = (T1205) future.get();
            if(t1205 == null){
                throw new RuntimeException("sendT9201 timeOut");
            }
            return CmdResult.success(t1205);
        }catch (Exception ex){
            log.error("sendT9201 ex params {}",t9201,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT9202(T9202 t9202) {
        log.info("sendT9202 clientId {} params {}",t9202.getClientId(),t9202);
        try {
            String clientId = t9202.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9202.getChannelNo());
            payload.writeByte(t9202.getPlaybackMode());
            payload.writeByte(t9202.getPlaybackSpeed());
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9202.getPlaybackTime(),"000000000000")));
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9201;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9202 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9202 error",f.cause());
                }
            });

            T0001 t0001 = (T0001) future.get();
            if(t0001 == null){
                throw new RuntimeException("sendT9202 timeOut");
            }
            return CmdResult.success(t0001);
        }catch (Exception ex){
            log.error("sendT9202 ex params {}",t9202,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT9206(T9206 t9206) {
        log.info("sendT9206 clientId {} params {}",t9206.getClientId(),t9206);
        try {
            String clientId = t9206.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9206.getIpLength());
            payload.writeBytes(t9206.getIp().getBytes());
            payload.writeShort(t9206.getPort());
            payload.writeByte(t9206.getUsernameLength());
            payload.writeBytes(t9206.getUsername().getBytes());
            payload.writeByte(t9206.getPasswordLength());
            payload.writeBytes(t9206.getPassword().getBytes());
            payload.writeByte(t9206.getPathLength());
            payload.writeBytes(t9206.getPath().getBytes());
            payload.writeByte(t9206.getChannelNo());
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9206.getStartTime(),"000000000000")));
            payload.writeBytes(ByteUtil.strToBCD(ObjectUtil.defaultIfEmpty(t9206.getEndTime(),"000000000000")));
            payload.writeInt(t9206.getWarnBit1());
            payload.writeInt(t9206.getWarnBit2());
            payload.writeByte(t9206.getMediaType());
            payload.writeByte(t9206.getStreamType());
            payload.writeByte(t9206.getStorageType());
            payload.writeByte(t9206.getCondition());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9206;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9206 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9206 error",f.cause());
                }
            });

            T0001 t0001 = (T0001) future.get();
            if(t0001 == null){
                throw new RuntimeException("sendT9206 timeOut");
            }
            return CmdResult.success(t0001);
        }catch (Exception ex){
            log.error("sendT9206 ex params {}",t9206,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT9207(T9207 t9207) {
        log.info("sendT9207 sendT9207 {} params {}",t9207.getClientId(),t9207);
        try {
            String clientId = t9207.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeShort(t9207.getResponseSerialNo());
            payload.writeByte(t9207.getCommand());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9207;
            Jtt808Message request = new Jtt808Message();
            request.setMessageId((short) requestMessageId);
            request.setVersionFlag((version==2019 ? 1:0));
            request.setPacketFlag(0);
            request.setProtocolVersion((byte) (version==2019 ? 1:0));
            request.setClientId(ByteUtil.strToBCD(clientId));
            request.setSerialNo((short) session.getSerialNo().getAndIncrement());
            request.setPayload(data);
            byte checkSum = request.createCheckSum();
            request.setCheckSum(checkSum);

            // 注册回调
            String callKey =  clientId + "_" + request.getSerialNo();
            log.info("sendT9207 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9207 error",f.cause());
                }
            });

            T0001 t0001 = (T0001) future.get();
            if(t0001 == null){
                throw new RuntimeException("sendT9207 timeOut");
            }
            return CmdResult.success(t0001);
        }catch (Exception ex){
            log.error("sendT9207 ex params {}",t9207,ex);
            return CmdResult.error(ex.getMessage());
        }
    }
}
