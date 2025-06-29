package cn.gbtmedia.jtt808.server.alarm.receive;

import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
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
public abstract class AbstractAlarmFileReceive implements IAlarmFileReceive {

    private final static Cache<String, List<AlarmFileMessage808>> MESSAGE_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .removalListener((String key, List<AlarmFileMessage808> value, RemovalCause  cause) -> {
                if(cause == RemovalCause.EXPIRED){
                    log.warn("AbstractAlarmFileReceive 等待分包消息超时 key {} 当前包/总数据包 {}/{}",key,value.size(),value.get(0).getPackageTotal());
                }
            })
            .build();

    static {
        SchedulerTask.getInstance().startPeriod("AbstractAlarmFileReceive", MESSAGE_CACHE::cleanUp,100);
    }


    @Override
    public  boolean support(Object request){
        if(!(request instanceof AlarmFileMessage808)){
            return false;
        }
        return doSupport((AlarmFileMessage808) request);
    }

    public abstract boolean doSupport(AlarmFileMessage808 request);

    /**
     *先合并消息
     */
    @Override
    public void handle(AlarmFileSession session, Object req){
        AlarmFileMessage808 request = (AlarmFileMessage808) req;
        // 如果是分包消息等待合并后再处理
        if(request.getPackageTotal() != 0){
            String key = String.format("%s_%s_%s",request.getClientIdStr(),request.getMessageId(),request.getPackageTotal());
            List<AlarmFileMessage808> messageList = MESSAGE_CACHE.get(key, k -> new CopyOnWriteArrayList<>());
            messageList.add(request);
            log.info("clientId {} {} subPackage {}/{}",session.getClientId(),
                    this.getClass().getSimpleName() , messageList.size(),request.getPackageTotal());
            // 等待所有数据包接收完毕, 合并成一个byte数组
            if(request.getPackageTotal() != messageList.size()){
                defaultResponse(session ,request);
                return;
            }
            MESSAGE_CACHE.invalidate(key);
            List<byte[]> byteList = messageList.stream().sorted(Comparator.comparing(AlarmFileMessage808::getPackageNo))
                    .map(AlarmFileMessage808::getPayload).toList();
            byte[] bytes = ByteUtil.mergeByte(byteList);
            request.setPayload(bytes);
        }
        doHandle(session,request);
    }

    public abstract void doHandle(AlarmFileSession session, AlarmFileMessage808 request);

    public void defaultResponse(AlarmFileSession session, AlarmFileMessage808 request){
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
        AlarmFileMessage808 response = new AlarmFileMessage808();
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

    public void dataResponse(AlarmFileSession session, AlarmFileMessage808 request,
                                byte[] data,int responseMessageId){
        String clientId = session.getClientId();
        int version = session.getVersion();
        // 消息id
        AlarmFileMessage808 response = new AlarmFileMessage808();
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
