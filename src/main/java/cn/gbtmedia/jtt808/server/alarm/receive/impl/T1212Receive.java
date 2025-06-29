package cn.gbtmedia.jtt808.server.alarm.receive.impl;

import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.event.T1212Event;
import cn.gbtmedia.jtt808.server.alarm.message.T1211;
import cn.gbtmedia.jtt808.server.alarm.receive.AbstractAlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

/**
 * 文件上传完成消息
 * @author xqs
 */
@Slf4j
@Component
public class T1212Receive extends AbstractAlarmFileReceive {

    @Override
    public boolean doSupport(AlarmFileMessage808 request) {
        return request.getMessageId() == 0X1212;
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

        log.info("T1212Receive clientId {} {}",clientId, t1211);

        // 应答消息 T9212
        ByteBuf payload = Unpooled.buffer();
        payload.writeByte(t1211.getNameLength());
        payload.writeBytes(t1211.getName().getBytes(StandardCharsets.UTF_8));
        payload.writeByte(0);
        payload.writeByte(0);
        payload.writeByte(0);
        int readableBytes = payload.readableBytes();
        byte[] data = new byte[readableBytes];
        payload.readBytes(data);

        // 返回 T9212
        super.dataResponse(session,request,data,0x9212);

        String platformAlarmIdO = t1211.getName().split("_")[4];
        // 上报的字数不足后面是空格
        String platformAlarmId = platformAlarmIdO.substring(0,platformAlarmIdO.indexOf(".")).trim();
        // 发布事件报警文件上传完成
        log.info("T1212Receive publishEvent clientId {} platformAlarmId {}",session.getClientId(), platformAlarmId);
        SpringUtil.publishEvent(new T1212Event(this,platformAlarmId,t1211));
    }
}
