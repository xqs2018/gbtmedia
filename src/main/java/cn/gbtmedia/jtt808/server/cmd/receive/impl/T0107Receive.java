package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.message.T0107;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.FutureContext;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 查询终端属性应答
 * @author xqs
 */
@Slf4j
@Component
public class T0107Receive extends AbstractJtt808Receive {

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0107;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T0107 t0107 = new T0107();
        t0107.setDeviceType(byteBuf.readUnsignedShort());

        log.info("T0107Receive clientId {} {}",session.getClientId(),t0107);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 触发回调
        String callKey = clientId +"_" + 0x0107;
        log.info("T0107Receive future callBack callKey {}", callKey);
        FutureContext.callBack(callKey, t0107);
    }
}
