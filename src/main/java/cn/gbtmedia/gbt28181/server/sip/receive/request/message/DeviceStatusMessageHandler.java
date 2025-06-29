package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.FutureContext;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;

/**
 * @author xqs
 */
@Slf4j
@Component
public class DeviceStatusMessageHandler implements MessageRequestHandler.Process{

    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private PlatformRepository platformRepository;

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "DeviceStatus".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级返回的设备状态
        if(message.containsKey("Response")){
            // 回复200
            sipServer.response200(requestEvent);
            Device device = deviceRepository.findByDeviceId(userId);
            if(device == null){
                log.error("DeviceStatus Response device is null userId {}",userId);
                return;
            }
            JSONObject response = message.getJSONObject("Response");
            String sn = response.getStr("SN");
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("DeviceStatus Response future callBack snKey {}", snKey);
            FutureContext.callBack(snKey, response);
            // 更新设备状态信息
            String online = response.getStr("Online");
            if ("ONLINE".equalsIgnoreCase(online)) {
                device.setOnline(1);
            } else {
                device.setOnline(0);
            }
            deviceRepository.save(device);
        }
        // 收到上级下发的查询设备请求
        if(message.containsKey("Query")){
            JSONObject query = message.getJSONObject("Query");
            String sn = query.getStr("SN");
            log.info("DeviceStatus Query userId {} sn {}",userId, sn);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            FromHeader fromHeader = (FromHeader) requestEvent.getRequest().getHeader(FromHeader.NAME);
            SipResult<?> result = sipPlatformSend.responseDeviceStatus(userId, sn, fromHeader.getTag());
        }
    }
}
