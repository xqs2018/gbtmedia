package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.json.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import jakarta.annotation.Resource;
import javax.sip.RequestEvent;
import javax.sip.header.ViaHeader;
import java.util.Date;

/**
 * @author xqs
 */
@Slf4j
@Component
public class KeepaliveMessageHandler implements MessageRequestHandler.Process{

    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private SipServer sipServer;

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "Keepalive".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        // 先回复200
        sipServer.response200(requestEvent);
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        Device device = deviceRepository.findByDeviceId(userId);
        if(device == null){
            log.error("Keepalive device is null {}",userId);
            return;
        }
        if(log.isDebugEnabled()){
            log.debug("keepalive deviceId {}",userId);
        }
        ViaHeader viaHeader = (ViaHeader) requestEvent.getRequest().getHeader(ViaHeader.NAME);
        String received = viaHeader.getReceived();
        int rPort = viaHeader.getRPort();
        if (ObjectUtils.isEmpty(received) || rPort == -1) {
            received = viaHeader.getHost();
            rPort = viaHeader.getPort();
        }
        if (device.getSipPort() != rPort) {
            device.setSipPort(rPort);
        }
        device.setOnline(1);
        device.setKeepaliveTime(new Date());
        deviceRepository.save(device);
    }
}
