package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.ByteUtil;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.event.T0200Event;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T0200;
import cn.gbtmedia.jtt808.server.cmd.message.T9208;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.Jtt808Send;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.entity.ClientT0200;
import cn.gbtmedia.jtt808.repository.ClientT0200Repository;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 终端上报位置信息
 * @author xqs
 */
@Slf4j
@Component
public class T0200Receive extends AbstractJtt808Receive {

    private static final ExecutorService JTT808_T0200_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-jtt808-t0200-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("jtt808 t0200 pool ex t {}", t, e))
                            .factory());

    @Resource
    private ClientT0200Repository clientT0200Repository;
    @Resource
    private Jtt808Send jtt808Send;

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0200;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();

        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());
        T0200 t0200 = new T0200();

        // 基本信息
        t0200.setWarnBit(byteBuf.readInt());
        t0200.setStatusBit(byteBuf.readInt());
        t0200.setLatitude(byteBuf.readInt());
        t0200.setLongitude(byteBuf.readInt());
        t0200.setAltitude(byteBuf.readUnsignedShort());
        t0200.setSpeed(byteBuf.readUnsignedShort());
        t0200.setDirection(byteBuf.readUnsignedShort());
        byte[] deviceTime = new byte[6];
        byteBuf.readBytes(deviceTime);
        t0200.setDeviceTime(ByteUtil.BCDToStr(deviceTime));

        // 位置附加信息 格式 [附加信息ID  附加信息长度 附加信息]
        while (byteBuf.readableBytes() > 0){
            int id = byteBuf.readUnsignedByte();
            int length = byteBuf.readUnsignedByte();
            byte[] content = new byte[length];
            byteBuf.readBytes(content);
            readAttributes(id,content,t0200,session);
        }

        log.debug("T0200Receive clientId {} {}",clientId, t0200);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        // 发布事件
        SpringUtil.publishEvent(new T0200Event(this, clientId,t0200));

        // 保存原始数据
        ClientT0200 clientT0200 = new ClientT0200();
        clientT0200.setClientId(session.getClientId());
        clientT0200.setT0200json(JSON.toJSONString(t0200));
        clientT0200Repository.saveBatch(List.of(clientT0200));

        // 检查需要上传报警附件
        checkAlarmUpload(t0200,session);
    }

    private void readAttributes(int id, byte[] content,T0200 t0200,ClientSession session){
        ByteBuf byteBuf = Unpooled.wrappedBuffer(content);
        int version = session.getVersion();

        //  0x01 累计行驶里程(1/10km)[4]
        if(id == 0x01){
            t0200.setAttr0x01(byteBuf.readUnsignedInt());
            return;
        }

        // 0x02 油量(1/10L)[2]
       if(id == 0x02){
           t0200.setAttr0x02(byteBuf.readUnsignedShort());
           return;
        }

        //  0x03 记录仪速度(1/10km)[2]
       if(id == 0x03){
           t0200.setAttr0x03(byteBuf.readUnsignedShort());
           return;
        }

        // 0x04 需人工确认报警事件ID [2]
       if(id == 0x04){
           t0200.setAttr0x04(byteBuf.readUnsignedShort());
           return;
        }

        // 0x06 车厢温度 [2]
       if(id == 0x06){
           t0200.setAttr0x06(byteBuf.readUnsignedShort());
           return;
        }

        // 0x25 车辆信号状态 [4] 定义参考表31
       if(id == 0x25){
           t0200.setAttr0x25(byteBuf.readInt());
           return;
        }

        // 0x2A IO状态位[2] 定义参考表32
       if(id == 0x2A){
           t0200.setAttr0x2A(byteBuf.readShort());
           return;
        }

        // 0x2B 模拟量[4]
       if(id == 0x2B){
           t0200.setAttr0x2B(byteBuf.readInt());
           return;
        }

        // 0x30 网络信号[1]
       if(id == 0x30){
           t0200.setAttr0x30(byteBuf.readByte());
           return;
        }

        // 0x31 GNSS卫星数[1]
       if(id == 0x31){
           t0200.setAttr0x31(byteBuf.readByte());
           return;
        }

        // 0x11 超速附加报警信息
       if(id == 0x11){
           T0200.Attr0x11 attr0x11 =new T0200.Attr0x11();
           t0200.setAttr0x11(attr0x11);

           attr0x11.setAreaType(byteBuf.readByte());
           if(attr0x11.getAreaType() != 0){
               attr0x11.setAreaId(byteBuf.readInt());
           }
           return;
        }

        // 0x12 进出区域/路线附加报警信息
       if(id == 0x12){
           T0200.Attr0x12 attr0x12 =new T0200.Attr0x12();
           t0200.setAttr0x12(attr0x12);

           attr0x12.setAreaType(byteBuf.readByte());
           attr0x12.setAreaId(byteBuf.readInt());
           attr0x12.setDirection(byteBuf.readByte());
           return;
        }

        // 0x13 路线行驶时间不足/过长附加报警信息
       if(id == 0x13){
           T0200.Attr0x13 attr0x13 =new T0200.Attr0x13();
           t0200.setAttr0x13(attr0x13);

           attr0x13.setAreaId(byteBuf.readInt());
           attr0x13.setDriveTime(byteBuf.readUnsignedShort());
           attr0x13.setResult(byteBuf.readByte());
           return;
        }

        // 0x64 (苏标) 高级驾驶辅助系统报警 ADAS
       if(id == 0x64){
           T0200.Attr0x64 attr0x64 =new T0200.Attr0x64();
           t0200.setAttr0x64(attr0x64);

           attr0x64.setId(byteBuf.readUnsignedInt());
           attr0x64.setState(byteBuf.readUnsignedByte());
           attr0x64.setType(byteBuf.readUnsignedByte());
           attr0x64.setLevel(byteBuf.readUnsignedByte());
           attr0x64.setFrontSpeed(byteBuf.readUnsignedByte());
           attr0x64.setFrontDistance(byteBuf.readUnsignedByte());
           attr0x64.setDeviateType(byteBuf.readUnsignedByte());
           attr0x64.setRoadSign(byteBuf.readUnsignedByte());
           attr0x64.setRoadSignValue(byteBuf.readUnsignedByte());
           attr0x64.setSpeed(byteBuf.readUnsignedByte());
           attr0x64.setAltitude(byteBuf.readUnsignedShort());
           attr0x64.setLatitude(byteBuf.readInt());
           attr0x64.setLongitude(byteBuf.readInt());
           byte[] alarmTimeBytes = new byte[6];
           byteBuf.readBytes(alarmTimeBytes);
           attr0x64.setAlarmTime(ByteUtil.BCDToStr(alarmTimeBytes));
           attr0x64.setStatusBit(byteBuf.readUnsignedShort());
           if (version == 2019) {
               byte[] deviceIdBytes = new byte[30];
               byteBuf.readBytes(deviceIdBytes);
               attr0x64.setDeviceId(new String(deviceIdBytes));
           } else {
               byte[] deviceIdBytes = new byte[7];
               byteBuf.readBytes(deviceIdBytes);
               attr0x64.setDeviceId(new String(deviceIdBytes));
           }
           byte[] dateTimeBytes = new byte[6];
           byteBuf.readBytes(dateTimeBytes);
           attr0x64.setDateTime(ByteUtil.BCDToStr(dateTimeBytes));
           attr0x64.setSequenceNo(byteBuf.readUnsignedByte());
           attr0x64.setFileTotal(byteBuf.readUnsignedByte());
           // 读取预留
           if (version == 2019) {
               attr0x64.setReserved(byteBuf.readUnsignedShort());
           } else {
               attr0x64.setReserved(byteBuf.readUnsignedByte());
           }
           // 平台报警编号[32] 平台产生
           attr0x64.setPlatformAlarmId(IdUtil.fastSimpleUUID());
           return;
        }

        // 0x65 (苏标) 驾驶员行为监测报警 DSM
       if(id == 0x65){
           T0200.Attr0x65 attr0x65 =new T0200.Attr0x65();
           t0200.setAttr0x65(attr0x65);

           attr0x65.setId(byteBuf.readUnsignedInt());
           attr0x65.setState(byteBuf.readUnsignedByte());
           attr0x65.setType(byteBuf.readUnsignedByte());
           attr0x65.setLevel(byteBuf.readUnsignedByte());
           attr0x65.setFatigueDegree(byteBuf.readUnsignedByte());
           attr0x65.setReserves(byteBuf.readInt());
           attr0x65.setSpeed(byteBuf.readUnsignedByte());
           attr0x65.setAltitude(byteBuf.readUnsignedShort());
           attr0x65.setLatitude(byteBuf.readInt());
           attr0x65.setLongitude(byteBuf.readInt());
           byte[] alarmTimeBytes = new byte[6];
           byteBuf.readBytes(alarmTimeBytes);
           attr0x65.setAlarmTime(ByteUtil.BCDToStr(alarmTimeBytes));
           attr0x65.setStatusBit(byteBuf.readUnsignedShort());
           if (version == 2019) {
               byte[] deviceIdBytes = new byte[30];
               byteBuf.readBytes(deviceIdBytes);
               attr0x65.setDeviceId(new String(deviceIdBytes));
           } else {
               byte[] deviceIdBytes = new byte[7];
               byteBuf.readBytes(deviceIdBytes);
               attr0x65.setDeviceId(new String(deviceIdBytes));
           }
           byte[] dateTimeBytes = new byte[6];
           byteBuf.readBytes(dateTimeBytes);
           attr0x65.setDateTime(ByteUtil.BCDToStr(dateTimeBytes));
           attr0x65.setSequenceNo(byteBuf.readUnsignedByte());
           attr0x65.setFileTotal(byteBuf.readUnsignedByte());
           if (version == 2019) {
               attr0x65.setReserved(byteBuf.readUnsignedShort());
           } else {
               attr0x65.setReserved(byteBuf.readUnsignedByte());
           }
           // 平台报警编号[32] 平台产生
           attr0x65.setPlatformAlarmId(IdUtil.fastSimpleUUID());
           return;
        }

        // 0x66 (苏标) 轮胎状态监测报警 TPMS
       if(id == 0x66){
           T0200.Attr0x66 attr0x66 =new T0200.Attr0x66();
           t0200.setAttr0x66(attr0x66);

           attr0x66.setId(byteBuf.readUnsignedInt());
           attr0x66.setState(byteBuf.readUnsignedByte());
           attr0x66.setSpeed(byteBuf.readUnsignedByte());
           attr0x66.setAltitude(byteBuf.readUnsignedShort());
           attr0x66.setLatitude(byteBuf.readInt());
           attr0x66.setLongitude(byteBuf.readInt());
           byte[] alarmTimeBytes = new byte[6];
           byteBuf.readBytes(alarmTimeBytes);
           attr0x66.setAlarmTime(ByteUtil.BCDToStr(alarmTimeBytes));
           attr0x66.setStatusBit(byteBuf.readUnsignedShort());
           if (version == 2019) {
               byte[] deviceIdBytes = new byte[30];
               byteBuf.readBytes(deviceIdBytes);
               attr0x66.setDeviceId(new String(deviceIdBytes));
           } else {
               byte[] deviceIdBytes = new byte[7];
               byteBuf.readBytes(deviceIdBytes);
               attr0x66.setDeviceId(new String(deviceIdBytes));
           }
           byte[] dateTimeBytes = new byte[6];
           byteBuf.readBytes(dateTimeBytes);
           attr0x66.setDateTime(ByteUtil.BCDToStr(dateTimeBytes));
           attr0x66.setSequenceNo(byteBuf.readUnsignedByte());
           attr0x66.setFileTotal(byteBuf.readUnsignedByte());
           if (version == 2019) {
               attr0x66.setReserved(byteBuf.readUnsignedShort());
           } else {
               attr0x66.setReserved(byteBuf.readUnsignedByte());
           }
           attr0x66.setTotalItem(byteBuf.readUnsignedByte());
           List<T0200.Attr0x66.Item> itemList = new ArrayList<>();
           attr0x66.setItems(itemList);
           while (byteBuf.readableBytes()>0){
               T0200.Attr0x66.Item item = new T0200.Attr0x66.Item();
               itemList.add(item);
               item.setPosition(byteBuf.readUnsignedByte());
               item.setType(byteBuf.readUnsignedShort());
               item.setPressure(byteBuf.readUnsignedShort());
               item.setTemperature(byteBuf.readUnsignedShort());
               item.setBatteryLevel(byteBuf.readUnsignedShort());
           }
           // 平台报警编号[32] 平台产生
           attr0x66.setPlatformAlarmId(IdUtil.fastSimpleUUID());
           return;
        }

        // 0x67 (苏标) 盲区监测系统报警 BSD
       if(id == 0x67){
           T0200.Attr0x67 attr0x67 =new T0200.Attr0x67();
           t0200.setAttr0x67(attr0x67);

           attr0x67.setId(byteBuf.readUnsignedInt());
           attr0x67.setState(byteBuf.readUnsignedByte());
           attr0x67.setType(byteBuf.readUnsignedByte());
           attr0x67.setSpeed(byteBuf.readUnsignedByte());
           attr0x67.setAltitude(byteBuf.readUnsignedShort());
           attr0x67.setLatitude(byteBuf.readInt());
           attr0x67.setLongitude(byteBuf.readInt());
           byte[] alarmTimeBytes = new byte[6];
           byteBuf.readBytes(alarmTimeBytes);
           attr0x67.setAlarmTime(ByteUtil.BCDToStr(alarmTimeBytes));
           attr0x67.setStatusBit(byteBuf.readUnsignedShort());
           if (version == 2019) {
               byte[] deviceIdBytes = new byte[30];
               byteBuf.readBytes(deviceIdBytes);
               attr0x67.setDeviceId(new String(deviceIdBytes));
           } else {
               byte[] deviceIdBytes = new byte[7];
               byteBuf.readBytes(deviceIdBytes);
               attr0x67.setDeviceId(new String(deviceIdBytes));
           }
           byte[] dateTimeBytes = new byte[6];
           byteBuf.readBytes(dateTimeBytes);
           attr0x67.setDateTime(ByteUtil.BCDToStr(dateTimeBytes));
           attr0x67.setSequenceNo(byteBuf.readUnsignedByte());
           attr0x67.setFileTotal(byteBuf.readUnsignedByte());
           if (version == 2019) {
               attr0x67.setReserved(byteBuf.readUnsignedShort());
           } else {
               attr0x67.setReserved(byteBuf.readUnsignedByte());
           }
           // 平台报警编号[32] 平台产生
           attr0x67.setPlatformAlarmId(IdUtil.fastSimpleUUID());
           return;
        }

        log.debug("T0200Receive no AttributesReader for clientId {} attrId 0x{}",session.getClientId(),
                Integer.toHexString(id));
    }

    private void checkAlarmUpload(T0200 t0200,ClientSession session) {
        ServerConfig conf = ServerConfig.getInstance();
        String alarmFilePath = conf.getJtt808().getAlarmFilePath();
        Runnable task = () -> {
            T0200.Attr0x64 attr0x64 = t0200.getAttr0x64();
            T0200.Attr0x65 attr0x65 = t0200.getAttr0x65();
            T0200.Attr0x66 attr0x66 = t0200.getAttr0x66();
            T0200.Attr0x67 attr0x67 = t0200.getAttr0x67();

            T9208 t9208 = new T9208();
            t9208.setClientId(session.getClientId());
            t9208.setIpLength(conf.getAccessIp().length());
            t9208.setIp(conf.getAccessIp());
            t9208.setTcpPort(conf.getJtt808().getAlarmFilePort());
            t9208.setUdpPort(conf.getJtt808().getAlarmFilePort());

            if (attr0x64 != null) {
                t9208.setDeviceId(attr0x64.getDeviceId());
                t9208.setDateTime(attr0x64.getDateTime());
                t9208.setSequenceNo(attr0x64.getSequenceNo());
                t9208.setFileTotal(attr0x64.getFileTotal());
                t9208.setReserved(attr0x64.getReserved());

                // 创建本地目录
                String platformAlarmId = attr0x64.getPlatformAlarmId();
                t9208.setPlatformAlarmId(platformAlarmId);
                String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
                File file = new File(savePath);
                boolean b = file.mkdirs();
                log.info("T0200_attr0x64 sendT9208 savePath {}", savePath);
                CmdResult<T0001> result = jtt808Send.sendT9208(t9208);
            }

            if (attr0x65 != null) {
                t9208.setDeviceId(attr0x65.getDeviceId());
                t9208.setDateTime(attr0x65.getDateTime());
                t9208.setSequenceNo(attr0x65.getSequenceNo());
                t9208.setFileTotal(attr0x65.getFileTotal());
                t9208.setReserved(attr0x65.getReserved());

                // 创建本地目录
                String platformAlarmId = attr0x65.getPlatformAlarmId();
                t9208.setPlatformAlarmId(platformAlarmId);
                String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
                File file = new File(savePath);
                boolean b = file.mkdirs();
                log.info("T0200_attr0x65 sendT9208 savePath {}", savePath);
                CmdResult<T0001> result = jtt808Send.sendT9208(t9208);
            }

            if (attr0x66 != null) {
                t9208.setDeviceId(attr0x66.getDeviceId());
                t9208.setDateTime(attr0x66.getDateTime());
                t9208.setSequenceNo(attr0x66.getSequenceNo());
                t9208.setFileTotal(attr0x66.getFileTotal());
                t9208.setReserved(attr0x66.getReserved());

                // 创建本地目录
                String platformAlarmId = attr0x66.getPlatformAlarmId();
                t9208.setPlatformAlarmId(platformAlarmId);
                String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
                File file = new File(savePath);
                boolean b = file.mkdirs();
                log.info("T0200_attr0x66 sendT9208 savePath {}", savePath);
                CmdResult<T0001> result = jtt808Send.sendT9208(t9208);
            }

            if (attr0x67 != null) {
                t9208.setDeviceId(attr0x67.getDeviceId());
                t9208.setDateTime(attr0x67.getDateTime());
                t9208.setSequenceNo(attr0x67.getSequenceNo());
                t9208.setFileTotal(attr0x67.getFileTotal());
                t9208.setReserved(attr0x67.getReserved());

                // 创建本地目录
                String platformAlarmId = attr0x67.getPlatformAlarmId();
                t9208.setPlatformAlarmId(platformAlarmId);
                String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
                File file = new File(savePath);
                boolean b = file.mkdirs();
                log.info("T0200_attr0x67 sendT9208 savePath {}", savePath);
                CmdResult<T0001> result = jtt808Send.sendT9208(t9208);
            }
        };
        JTT808_T0200_POOL.execute(task);
    }
}
