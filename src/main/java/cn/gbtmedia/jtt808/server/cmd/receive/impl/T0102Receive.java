package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 终端鉴权处理
 * @author xqs
 */
@Slf4j
@Component
public class T0102Receive extends AbstractJtt808Receive {

    @Resource
    private ClientRepository clientRepository;

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0102;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();

        String token;
        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());
        if(version == 2019){
            // 鉴权码长度
            byte n = byteBuf.readByte();
            // 鉴权码
            byte[] tokenBytes = new byte[n];
            byteBuf.readBytes(tokenBytes);
            token = new String(tokenBytes);
            // 终端IMEI
            byte[] imei = new byte[15];
            byteBuf.readBytes(imei);
            // 软件版本号
            byte[] softwareVersion = new byte[20];
            byteBuf.readBytes(softwareVersion);
        }else {
            // 鉴权码
            int last = byteBuf.readableBytes();
            byte[] tokenBytes = new byte[last];
            byteBuf.readBytes(tokenBytes);
            token = new String(tokenBytes);
        }

        log.info("T0102Receive clientId {} token {}",clientId,token);

        int resultCode = 0;
        Client client = clientRepository.findByClientId(clientId);
        if(client == null){
            log.error("T0102Receive client is null clientId {}",clientId);
            resultCode = 1;
        }else if(!token.equals(client.getToken())){
            log.error("T0102Receive client token error clientId {} clientToken {} serverToken {}",clientId,token,client.getToken());
            resultCode = 1;
        }

        // 应答消息，平台通用应答 0x8001
        int responseSerialNo = request.getSerialNo();
        // 结果：0.成功 1.失败 2.消息有误 3.不支持 4.报警处理确认
        ByteBuf payload = Unpooled.buffer();
        payload.writeShort(responseSerialNo);
        payload.writeShort(0x0102);
        payload.writeByte(resultCode);
        int readableBytes = payload.readableBytes();
        byte[] data = new byte[readableBytes];
        payload.readBytes(data);

        // 返回 0x8001
        super.dataResponse(session,request,data,0x8001);
    }
}
