package cn.gbtmedia.jtt808.server.cmd.send;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T0104;
import cn.gbtmedia.jtt808.server.cmd.message.T0107;
import cn.gbtmedia.jtt808.server.cmd.message.T8104;
import cn.gbtmedia.jtt808.server.cmd.message.T8107;
import cn.gbtmedia.jtt808.server.cmd.message.T8300;
import cn.gbtmedia.jtt808.server.cmd.message.T9208;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.Future;


/**
 * @author xqs
 */
@Slf4j
@Service
public class Jtt808SendImpl implements Jtt808Send {

    @Override
    public CmdResult<T0104> sendT8104(T8104 t8104) {
        log.info("sendT8104 clientId {} params {}",t8104.getClientId(),t8104);
        try {
            String clientId = t8104.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x8104;
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
            String callKey =  clientId +  "_" + request.getSerialNo();
            log.info("sendT8104 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT8104 error",f.cause());
                }
            });

            T0104 t0104 = (T0104) future.get();
            if(t0104 == null){
                throw new RuntimeException("sendT8104 timeOut");
            }
            return CmdResult.success(t0104);
        }catch (Exception ex){
            log.error("sendT8104 ex params {}",t8104,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0107> sendT8107(T8107 t8107) {
        log.info("sendT8107 clientId {} params {}",t8107.getClientId(),t8107);
        try {
            String clientId = t8107.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x8107;
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
            String callKey =  clientId +  "_" + 0x0107;
            log.info("sendT8107 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT8107 error",f.cause());
                }
            });

            T0107 t0107 = (T0107) future.get();
            if(t0107 == null){
                throw new RuntimeException("sendT8107 timeOut");
            }
            return CmdResult.success(t0107);
        }catch (Exception ex){
            log.error("sendT8107 ex params {}",t8107,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT8300(T8300 t8300) {
        log.info("sendT8300 clientId {} params {}",t8300.getClientId(),t8300);
        try {
            String clientId = t8300.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t8300.getSign());
            payload.writeByte(t8300.getType());
            payload.writeBytes(t8300.getContent().getBytes());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x8107;
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
            String callKey =  clientId +  "_" + request.getSerialNo();
            log.info("sendT8300 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT8300 error",f.cause());
                }
            });

            T0001 t0001 = (T0001) future.get();
            if(t0001 == null){
                throw new RuntimeException("sendT8300 timeOut");
            }
            return CmdResult.success(t0001);
        }catch (Exception ex){
            log.error("sendT8300 ex params {}",t8300,ex);
            return CmdResult.error(ex.getMessage());
        }
    }

    @Override
    public CmdResult<T0001> sendT9208(T9208 t9208) {
        log.info("sendT9208 clientId {} params {}",t9208.getClientId(),t9208);
        try {
            String clientId = t9208.getClientId();
            ClientSession session = Jtt808SessionManager.getInstance().getClientSession(clientId);
            if(session == null){
                throw new RuntimeException("session is null clientId " + clientId);
            }
            int version = session.getVersion();

            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(t9208.getIpLength());
            payload.writeBytes(t9208.getIp().getBytes());
            payload.writeShort(t9208.getTcpPort());
            payload.writeShort(t9208.getUdpPort());
            if(version == 2019){
                byte[] bytes = t9208.getDeviceId().getBytes();
                payload.writeBytes(ByteUtil.padCheckBytes(bytes,30));
            }else {
                byte[] bytes = t9208.getDeviceId().getBytes();
                payload.writeBytes(ByteUtil.padCheckBytes(bytes,7));
            }
            payload.writeBytes(ByteUtil.strToBCD(t9208.getDateTime()));
            payload.writeByte(t9208.getSequenceNo());
            payload.writeByte(t9208.getFileTotal());
            if(version == 2019){
                payload.writeShort(t9208.getReserved());
            }else {
                payload.writeByte(t9208.getReserved());
            }
            payload.writeBytes(t9208.getPlatformAlarmId().getBytes());
            payload.writeBytes(t9208.getReserves());
            int readableBytes = payload.readableBytes();
            byte[] data = new byte[readableBytes];
            payload.readBytes(data);

            // 消息id
            int requestMessageId = 0x9208;
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
            String callKey =  clientId +  "_" + request.getSerialNo();
            log.info("sendT9208 future regist callKey {} ",callKey);
            Future<Object> future = FutureContext.regist(callKey);
            // 发送消息
            session.getChannel().writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("sendT9208 error",f.cause());
                }
            });

            T0001 t0001 = (T0001) future.get();
            if(t0001 == null){
                throw new RuntimeException("sendT9208 timeOut");
            }
            return CmdResult.success(t0001);
        }catch (Exception ex){
            log.error("sendT9208 ex params {}",t9208,ex);
            return CmdResult.error(ex.getMessage());
        }
    }
}
