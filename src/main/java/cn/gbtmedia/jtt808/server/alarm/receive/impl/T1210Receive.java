package cn.gbtmedia.jtt808.server.alarm.receive.impl;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.receive.AbstractAlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import cn.gbtmedia.jtt808.server.alarm.message.T1210;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 报警附件信息消息
 * @author xqs
 */
@Slf4j
@Component
public class T1210Receive extends AbstractAlarmFileReceive {

    @Override
    public boolean doSupport(AlarmFileMessage808 request) {
        return request.getMessageId() == 0X1210;
    }

    @Override
    public void doHandle(AlarmFileSession session, AlarmFileMessage808 request) {
        String clientId = session.getClientId();
        int version = session.getVersion();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T1210 t1210 = new T1210();
        if(version == 2019){
            t1210.setDeviceId1(byteBuf.readCharSequence(30, StandardCharsets.UTF_8).toString());
        }else {
            t1210.setDeviceId1(byteBuf.readCharSequence(7, StandardCharsets.UTF_8).toString());
        }
        // 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1]
        if (version == 2019) {
            byte[] deviceIdBytes = new byte[30];
            byteBuf.readBytes(deviceIdBytes);
            t1210.setDeviceId(new String(deviceIdBytes));
        } else {
            byte[] deviceIdBytes = new byte[7];
            byteBuf.readBytes(deviceIdBytes);
            t1210.setDeviceId(new String(deviceIdBytes));
        }
        byte[] dateTimeBytes = new byte[6];
        byteBuf.readBytes(dateTimeBytes);
        t1210.setDateTime(ByteUtil.BCDToStr(dateTimeBytes));
        t1210.setSequenceNo(byteBuf.readUnsignedByte());
        t1210.setFileTotal(byteBuf.readUnsignedByte());
        if (version == 2019) {
            t1210.setReserved(byteBuf.readUnsignedShort());
        } else {
            t1210.setReserved(byteBuf.readUnsignedByte());
        }

        t1210.setPlatformAlarmId(byteBuf.readCharSequence(32, StandardCharsets.UTF_8).toString());
        t1210.setType(byteBuf.readByte());
        t1210.setTotalItem(byteBuf.readUnsignedByte());

        List<T1210.Item> itemList = new ArrayList<>();
        t1210.setItems(itemList);
        // 文件详情
        while (byteBuf.readableBytes() > 0){
            T1210.Item item = new T1210.Item();
            itemList.add(item);
            item.setNameLength(byteBuf.readUnsignedByte());
            item.setName(byteBuf.readCharSequence(item.getNameLength(),StandardCharsets.UTF_8).toString());
            item.setSize(byteBuf.readUnsignedInt());
        }

        log.info("T1210Receive clientId {} {}",clientId, t1210);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);
    }
}
