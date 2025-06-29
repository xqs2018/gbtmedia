package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.FutureContext;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 终端通用应答
 * @author xqs
 */
@Slf4j
@Component
public class T0001Receive extends AbstractJtt808Receive {

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x001;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());
        T0001 t0001 = new T0001();
        t0001.setResponseSerialNo(byteBuf.readUnsignedShort());
        t0001.setResponseMessageId(byteBuf.readUnsignedShort());
        t0001.setResultCode(byteBuf.readUnsignedByte());

        // 触发回调
        String callKey = clientId +"_" + t0001.getResponseSerialNo();
        log.info("T0001Receive future callBack callKey {}", callKey);
        FutureContext.callBack(callKey,t0001);
    }
}
