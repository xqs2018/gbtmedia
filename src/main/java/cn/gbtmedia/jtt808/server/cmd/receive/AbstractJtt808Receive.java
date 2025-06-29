package cn.gbtmedia.jtt808.server.cmd.receive;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
public abstract class AbstractJtt808Receive implements IJtt808Receive{

    private final static Cache<String, List<Jtt808Message>> MESSAGE_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .removalListener((String key, List<Jtt808Message> value, RemovalCause cause) -> {
                if(cause == RemovalCause.EXPIRED){
                    log.warn("AbstractJtt808Receive 等待分包消息超时 key {} 当前包/总数据包 {}/{}",key,value.size(),value.get(0).getPackageTotal());
                }
            })
            .build();

    static {
        SchedulerTask.getInstance().startPeriod("AbstractJtt808Receive", MESSAGE_CACHE::cleanUp,100);
    }

    public abstract boolean support(Jtt808Message request);

    /**
     *先合并消息
     */
    @Override
    public void handle(ClientSession session, Jtt808Message request){
        // 如果是分包消息等待合并后再处理
        if(request.getPackageTotal() != 0){
            String key = String.format("%s_%s_%s",request.getClientIdStr(),request.getMessageId(),request.getPackageTotal());
            List<Jtt808Message> messageList = MESSAGE_CACHE.get(key,k-> new CopyOnWriteArrayList<>());
            messageList.add(request);
            log.info("clientId {} {} subPackage {}/{}",session.getClientId(),
                    this.getClass().getSimpleName() , messageList.size(),request.getPackageTotal());
            // 等待所有数据包接收完毕, 合并成一个byte数组
            if(request.getPackageTotal() != messageList.size()){
                defaultResponse(session ,request);
                return;
            }
            MESSAGE_CACHE.invalidate(key);
            List<byte[]> byteList = messageList.stream().sorted(Comparator.comparing(Jtt808Message::getPackageNo))
                    .map(Jtt808Message::getPayload).toList();
            byte[] bytes = ByteUtil.mergeByte(byteList);
            request.setPayload(bytes);
        }
        doHandle(session,request);
    }

    public abstract void doHandle(ClientSession session, Jtt808Message request);

    public void defaultResponse(ClientSession session, Jtt808Message request){
        String clientId = session.getClientId();
        int version = session.getVersion();
        int serialNo = request.getSerialNo();

        // 应答消息，平台通用应答 0x8001
        int responseSerialNo = request.getSerialNo();
        // 结果：0.成功 1.失败 2.消息有误 3.不支持 4.报警处理确认
        int resultCode = 0;
        ByteBuf payload = Unpooled.buffer();
        payload.writeShort(responseSerialNo);
        payload.writeShort(request.getMessageId());
        payload.writeByte(resultCode);
        int readableBytes = payload.readableBytes();
        byte[] data = new byte[readableBytes];
        payload.readBytes(data);

        // 消息id
        int responseMessageId = 0x8001;
        Jtt808Message response = new Jtt808Message();
        response.setMessageId((short) responseMessageId);
        response.setVersionFlag(request.getVersionFlag());
        response.setPacketFlag(0);
        response.setProtocolVersion((byte) (version==2019 ? 1:0));
        response.setClientId(request.getClientId());
        response.setSerialNo((short) session.getSerialNo().getAndIncrement());
        response.setPayload(data);
        byte checkSum = response.createCheckSum();
        response.setCheckSum(checkSum);

        session.getChannel().writeAndFlush(response).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                log.error("clientId {} {} defaultResponse error",session.getClientId(),
                        this.getClass().getSimpleName() , f.cause());
            }
        });
    }

    public void dataResponse(ClientSession session, Jtt808Message request,
                             byte[] data, int responseMessageId){
        String clientId = session.getClientId();
        int version = session.getVersion();
        // 消息id
        Jtt808Message response = new Jtt808Message();
        response.setMessageId((short) responseMessageId);
        response.setVersionFlag(request.getVersionFlag());
        response.setPacketFlag(0);
        response.setProtocolVersion((byte) (version==2019 ? 1:0));
        response.setClientId(request.getClientId());
        response.setSerialNo((short) session.getSerialNo().getAndIncrement());
        response.setPayload(data);
        byte checkSum = response.createCheckSum();
        response.setCheckSum(checkSum);

        session.getChannel().writeAndFlush(response).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                log.error("clientId {} {} dataResponse error",session.getClientId(),
                        this.getClass().getSimpleName() , f.cause());
            }
        });
    }
}
