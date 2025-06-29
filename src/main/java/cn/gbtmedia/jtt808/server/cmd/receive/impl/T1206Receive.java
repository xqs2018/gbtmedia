package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.event.T1206Event;
import cn.gbtmedia.jtt808.server.cmd.message.T1206;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 音视频录像上传完成通知
 * @author xqs
 */
@Slf4j
@Component

public class T1206Receive extends AbstractJtt808Receive {
    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x1206;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T1206 t1206 = new T1206();
        t1206.setResponseSerialNo(byteBuf.readUnsignedShort());
        t1206.setResult(byteBuf.readByte());

        log.info("T1206Receive clientId {} {}",clientId,t1206);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 发布已完成事件
        log.info("T1206Receive publishEvent clientId {} serialNo {}", clientId, t1206.getResponseSerialNo());
        SpringUtil.publishEvent(new T1206Event(this,clientId,t1206));
    }
}
