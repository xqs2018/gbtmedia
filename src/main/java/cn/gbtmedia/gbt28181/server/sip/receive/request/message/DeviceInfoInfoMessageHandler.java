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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;

/**
 * @author xqs
 */
@Slf4j
@Component
public class DeviceInfoInfoMessageHandler implements MessageRequestHandler.Process {

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
        return "DeviceInfo".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级返回的设备信息
        if(message.containsKey("Response")){
            // 回复200
            sipServer.response200(requestEvent);
            Device device = deviceRepository.findByDeviceId(userId);
            if(device == null){
                log.error("DeviceInfo Response device is null userId {}",userId);
                return;
            }
            JSONObject response = message.getJSONObject("Response");
            String sn = response.getStr("SN");
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("DeviceInfo Response future callBack snKey {}", snKey);
            FutureContext.callBack(snKey, response);
            // 更新设备信息
            String deviceName = response.getStr("DeviceName");
            device.setName(deviceName);
            device.setManufacturer(response.getStr("Manufacturer"));
            device.setModel(response.getStr("Model"));
            device.setFirmware(response.getStr("Firmware"));
            log.info("DeviceInfo Response " +
                            "\n============================== DeviceInfo ================================" +
                            "\n deviceId: {} hostAddress: {} {} " +
                            "\n--------------------------------------------------------------------------" +
                            "\n deviceName: {} Manufacturer: {} " +
                            "\n--------------------------------------------------------------------------" +
                            "\n Model: {} Firmware: {} "+
                            "\n===========================================================================",
                    device.getDeviceId(),device.getSipIp() + ":" + device.getSipPort(),device.getSipTransport(),
                    device.getName(),device.getManufacturer(),device.getModel(),device.getFirmware());
            deviceRepository.save(device);
        }
        // 收到上级下发的查询设备请求
        if(message.containsKey("Query")){
            JSONObject query = message.getJSONObject("Query");
            String sn = query.getStr("SN");
            log.info("DeviceInfo Query userId {} sn {}",userId, sn);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            FromHeader fromHeader = (FromHeader) requestEvent.getRequest().getHeader(FromHeader.NAME);
            SipResult<?> result = sipPlatformSend.responseDeviceInfo(userId, sn, fromHeader.getTag());
        }
    }
}
