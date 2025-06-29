package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;

/**
 * @author xqs
 */
@Slf4j
@Component
public class DeviceControlMessageHandler implements MessageRequestHandler.Process{

    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "DeviceControl".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级回复控制响应
        if(message.containsKey("Response")){
            JSONObject response = message.getJSONObject("Response");
            String sn = response.getStr("SN");
            String result = response.getStr("Result");
            log.info("Response DeviceControl userId {} sn {} result {}",userId, sn,result);
        }
        // 收到上级下发控制，转发控制到对应下级
        if(message.containsKey("Control")){
            JSONObject control = message.getJSONObject("Control");
            String channelId = control.getStr("DeviceID");
            String sn = control.getStr("SN");
            log.info("Control DeviceControl userId {} channelId {}",userId, channelId);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            PlatformChannel platformChannel = platformChannelRepository.findByPlatformIdAndChannelId(userId, channelId);
            if(platformChannel == null){
                log.error("no platformChannel userId {} channelId {}",userId,channelId);
                sipServer.response403(requestEvent);
                return;
            }
            // 回复200
            sipServer.response200(requestEvent);
            if(control.containsKey("PTZCmd")){
                // 向上级回复xml
                String deviceId = platformChannel.getDeviceId();
                SipResult<?> result = sipPlatformSend.responseBroadcast(userId, deviceId, channelId, sn, "OK");
                // 转发控制到下级
                sipDeviceSend.controlPtzCmd(deviceId, channelId, control.getStr("PTZCmd"));
            }
        }
    }
}
