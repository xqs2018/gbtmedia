package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.server.cmd.message.T0104;
import cn.gbtmedia.jtt808.server.cmd.message.T1003;
import cn.gbtmedia.jtt808.server.cmd.message.T8104;
import cn.gbtmedia.jtt808.server.cmd.message.T9003;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.send.Jtt1078Send;
import cn.gbtmedia.jtt808.server.cmd.send.Jtt808Send;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import cn.hutool.core.util.IdUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 终端注册处理
 * @author xqs
 */
@Slf4j
@Component
public class T0100Receive extends AbstractJtt808Receive {

    private static final ExecutorService JTT808_T0100_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-jtt808-t0100-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("jtt808 t0100 pool ex t {}", t, e))
                            .factory());

    @Resource
    private ClientRepository clientRepository;

    @Resource
    private Jtt1078Send jt1078Send;

    @Resource
    private Jtt808Send jt808Send;

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0100;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        // 获取真实版本标记，通过消息体长度兼容2011  2019  2013 2011
        int version;
        int bodyLength = request.getBodyLength();
        if (bodyLength > 0 && bodyLength < 37){
            version  = 2011;
        }else {
            version = request.getVersionFlag() == 1 ? 2019: 2013;
        }
        String clientId = session.getClientId();
        session.setVersion(version);
        // 读取消息体数据
        ByteBuf byteBuf = Unpooled.wrappedBuffer(request.getPayload());
        // 省域ID
        short provinceId = byteBuf.readShort();
        // 市县域ID
        short cityId = byteBuf.readShort();
        // 制造商ID
        byte[] makerIdBytes = new byte[version == 2019 ? 11 : 5];
        byteBuf.readBytes(makerIdBytes);
        String makerId = new String(makerIdBytes);
        // 终端型号
        byte[] deviceModelBytes = new byte[version == 2011 ? 8 : version == 2013 ? 20 : 30];
        byteBuf.readBytes(deviceModelBytes);
        String deviceModel = new String(deviceModelBytes, Charset.forName("gbk")).trim();
        // 终端ID
        byte[]  deviceIdBytes =  new byte[version == 2019 ? 30 : 7];
        byteBuf.readBytes(deviceIdBytes);
        String deviceId = new String(deviceIdBytes, Charset.forName("gbk")).trim();
        // 车牌颜色：0.未上车牌 1.蓝色 2.黄色 3.黑色 4.白色 9.其他
        byte plateColor = byteBuf.readByte();
        // 车辆标识
        int last = byteBuf.readableBytes();
        byte[] plateNoBytes = new byte[last];
        byteBuf.readBytes(plateNoBytes);
        String plateNo = new String(plateNoBytes, Charset.forName("gbk")).trim();

        log.info("<<<<< jtt808 客户端注册 " +
                        "\n============================== 注册信息 ===================================" +
                        "\n IP地址: {} " +
                        "\n--------------------------------------------------------------------------" +
                        "\n 终端手机号: {} 协议版本: {} " +
                        "\n--------------------------------------------------------------------------" +
                        "\n 省域ID: {} 市县域ID: {} 制造商ID: {} "+
                        "\n--------------------------------------------------------------------------" +
                        "\n 终端型号: {} 终端ID: {} 车牌颜色: {} 车辆标识: {} " +
                        "\n===========================================================================",
                session.getChannel().remoteAddress().toString(),clientId,version,provinceId, cityId,
                makerId,deviceModel, deviceId , plateColor,plateNo);

        // 应答消息 0x8100 应答流水号
        int responseSerialNo = request.getSerialNo();
        // 结果：0.成功 1.车辆已被注册 2.数据库中无该车辆 3.终端已被注册 4.数据库中无该终端
        int resultCode = 0;
        // 鉴权码
        String token = IdUtil.fastSimpleUUID();
        log.info("T0100Receive clientId {} create token {}",clientId,token);
        byte[] tokenBytes = token.getBytes();
        ByteBuf payload = Unpooled.buffer();
        payload.writeShort(responseSerialNo);
        payload.writeByte(resultCode);
        payload.writeBytes(tokenBytes);
        int readableBytes = payload.readableBytes();
        byte[] data = new byte[readableBytes];
        payload.readBytes(data);

        // 返回 0x8100
        super.dataResponse(session,request,data,0x8100);

        // 保存终端信息
        Client client = clientRepository.findByClientId(clientId);
        if(client == null){
            client = new Client();
            client.setRegistTime(new Date());
        }
        client.setOnline(1);
        client.setToken(token);
        client.setClientId(clientId);
        client.setVersion(String.valueOf(version));
        client.setProvinceId(String.valueOf(provinceId));
        client.setCityId(String.valueOf(cityId));
        client.setMakerId(makerId);
        client.setDeviceModel(deviceModel);
        client.setDeviceId(deviceId);
        client.setPlateColor(String.valueOf(plateColor));
        client.setPlateNo(plateNo);
        client.setClientIp(session.getSocketAddress().getAddress().getHostAddress());
        client.setClientPort(session.getSocketAddress().getPort());
        client.setMaxAudioChannels(4);
        client.setMaxVideoChannels(4);
        client.setAudioVideoChannels(4);
        clientRepository.save(client);
        // 先不查询 TODO
        if(client.getAudioVideoChannels() > 0){
            return;
        }
        // 查询视频通道数量
        JTT808_T0100_POOL.execute(()->{
            // 查询终端参数 通道数量
            T8104 t8104 = new T8104();
            t8104.setClientId(clientId);
            CmdResult<T0104> resultT0104 = jt808Send.sendT8104(t8104);
            if(resultT0104.isSuccess()){
                T0104.Param0x0076 param = resultT0104.getData().getParam0x0076();
                Client old = clientRepository.findByClientId(clientId);
                old.setAudioVideoChannels(param.getAudioVideoChannels());
                log.info("clientId {} audioVideoChannels {}",clientId,old.getAudioVideoChannels());
                clientRepository.save(old);
            }
            // 物理通道
            T9003 t9003 = new T9003();
            t9003.setClientId(clientId);
            CmdResult<T1003> resultT1003 = jt1078Send.sendT9003(t9003);
            if(resultT1003.isSuccess()){
                Client old = clientRepository.findByClientId(clientId);
                old.setMaxAudioChannels(resultT1003.getData().getMaxAudioChannels());
                old.setMaxVideoChannels(resultT1003.getData().getMaxVideoChannels());
                log.info("clientId {} maxVideoChannels {}",clientId,old.getMaxVideoChannels());
                clientRepository.save(old);
            }
        });
    }

}
