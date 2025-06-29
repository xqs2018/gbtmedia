package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.message.T1205;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.FutureContext;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

/**
 * 终端上传录像资源
 * @author xqs
 */
@Slf4j
@Component
public class T1205Receive extends AbstractJtt808Receive {

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x1205;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T1205 t1205 = new T1205();
        t1205.setResponseSerialNo(byteBuf.readUnsignedShort());
        t1205.setTotal(byteBuf.readInt());
        t1205.setItems(new ArrayList<>());
        // 读取列表
        while (byteBuf.readableBytes() > 0){
            T1205.Item item = new T1205.Item();
            item.setChannelNo(byteBuf.readUnsignedByte());
            byte[] startTime = new byte[6];
            byteBuf.readBytes(startTime);
            item.setStartTime(ByteUtil.BCDToStr(startTime));
            byte[] endTime = new byte[6];
            byteBuf.readBytes(endTime);
            item.setEndTime(ByteUtil.BCDToStr(endTime));
            item.setWarnBit1(byteBuf.readInt());
            item.setWarnBit2(byteBuf.readInt());
            item.setMediaType(byteBuf.readUnsignedByte());
            item.setStreamType(byteBuf.readUnsignedByte());
            item.setStorageType(byteBuf.readUnsignedByte());
            item.setSize(byteBuf.readUnsignedInt());
            t1205.getItems().add(item);
        }

        log.info("T1205Receive clientId {} {}",clientId,t1205);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 触发回调
        String callKey = clientId +"_" + t1205.getResponseSerialNo();
        log.info("T1205Receive future callBack callKey {}", callKey);
        FutureContext.callBack(callKey, t1205);
    }
}
