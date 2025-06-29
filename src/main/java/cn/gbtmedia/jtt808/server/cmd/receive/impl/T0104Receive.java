package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.message.T0104;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.FutureContext;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询终端参数应答
 * @author xqs
 */
@Slf4j
@Component
public class T0104Receive extends AbstractJtt808Receive {

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0104;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());

        T0104 t0104 = new T0104();
        t0104.setResponseSerialNo(byteBuf.readUnsignedShort());
        t0104.setTotal(byteBuf.readUnsignedByte());
        Map<Integer, Object> parameters = new HashMap<>();
        t0104.setParameters(parameters);

        while (byteBuf.readableBytes() > 0){
            int paramId = byteBuf.readInt();
            int paramLength = byteBuf.readUnsignedByte();
            byte[] paramValue = new byte[paramLength];
            byteBuf.readBytes(paramValue);
            parameters.put(paramId,paramValue);
            readParameters(paramId,paramValue,t0104,session);
        }

        log.info("T0104Receive clientId {} {}",session.getClientId(),t0104);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 触发回调
        String callKey = clientId +"_" + t0104.getResponseSerialNo();
        log.info("T0104Receive future callBack callKey {}", callKey);
        FutureContext.callBack(callKey, t0104);
    }

    private void readParameters(int paramId,byte[] paramValue,T0104 t0104, ClientSession session){
        ByteBuf byteBuf = Unpooled.wrappedBuffer(paramValue);
        int version = session.getVersion();

        if(paramId == 0x0076){
            T0104.Param0x0076 param = new T0104.Param0x0076();
            t0104.getParameters().put(paramId,param);
            t0104.setParam0x0076(param);
            param.setAudioVideoChannels(byteBuf.readByte());
            param.setAudioChannels(byteBuf.readByte());
            param.setVideoChannels(byteBuf.readByte());
            List<T0104.Param0x0076.Item> itemList = new ArrayList<>();
            param.setItems(itemList);
            while (byteBuf.readableBytes()>0){
                T0104.Param0x0076.Item item = new T0104.Param0x0076.Item();
                itemList.add(item);
                item.setChannelId(byteBuf.readByte());
                item.setChannelNo(byteBuf.readByte());
                item.setChannelType(byteBuf.readByte());
                item.setHasPtz(byteBuf.readByte());
            }
        }
    }
}
