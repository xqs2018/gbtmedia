package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.FutureContext;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.message.T1003;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 终端上传音视频属性
 * @author xqs
 */
@Slf4j
@Component
public class T1003Receive extends AbstractJtt808Receive {

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x1003;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T1003 t1003 = new T1003();
        t1003.setAudioFormat(byteBuf.readUnsignedByte());
        t1003.setAudioChannels(byteBuf.readUnsignedByte());
        t1003.setAudioSamplingRate(byteBuf.readUnsignedByte());
        t1003.setAudioBitDepth(byteBuf.readUnsignedByte());
        t1003.setAudioFrameLength(byteBuf.readUnsignedShort());
        t1003.setAudioSupport(byteBuf.readUnsignedByte());
        t1003.setVideoFormat(byteBuf.readUnsignedByte());
        t1003.setMaxAudioChannels(byteBuf.readUnsignedByte());
        t1003.setMaxVideoChannels(byteBuf.readUnsignedByte());

        log.info("T1003Receive clientId {} {}",clientId,t1003);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 触发回调
        String callKey = clientId +"_" + 0x1003;
        log.info("T1003Receive future callBack callKey {}", callKey);
        FutureContext.callBack(callKey, t1003);
    }
}
