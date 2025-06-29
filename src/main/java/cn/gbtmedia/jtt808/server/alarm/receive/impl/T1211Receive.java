package cn.gbtmedia.jtt808.server.alarm.receive.impl;

import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.message.T1211;
import cn.gbtmedia.jtt808.server.alarm.receive.AbstractAlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 文件信息上传
 * @author xqs
 */
@Slf4j
@Component
public class T1211Receive extends AbstractAlarmFileReceive {

    @Override
    public boolean doSupport(AlarmFileMessage808 request) {
        return request.getMessageId() == 0X1211;
    }

    @Override
    public void doHandle(AlarmFileSession session, AlarmFileMessage808 request) {
        String clientId = session.getClientId();
        int version = session.getVersion();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T1211 t1211 = new T1211();
        t1211.setNameLength(byteBuf.readUnsignedByte());
        t1211.setName(byteBuf.readCharSequence(t1211.getNameLength(), StandardCharsets.UTF_8).toString());
        t1211.setSize(byteBuf.readUnsignedInt());

        log.info("T1211Receive clientId {} {}",clientId, t1211);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);
    }
}
