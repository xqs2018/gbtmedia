package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.BroadcastDto;
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
public class BroadcastMessageHandler implements MessageRequestHandler.Process{

    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;


    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "Broadcast".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级返回的语音广播请求消息
        if(message.containsKey("Response")){
            // 回复200
            sipServer.response200(requestEvent);
            JSONObject response = message.getJSONObject("Response");
            String sn = response.getStr("SN");
            String channelId =  response.getStr("DeviceID");
            String result = response.getStr("Result");
            log.info("Broadcast Response deviceId {} channelId {} Result {}",userId,channelId,result);
        }
        // 收到上级下发的语音广播请求
        if(message.containsKey("Notify")){
            JSONObject notify = message.getJSONObject("Notify");
            String sn = notify.getStr("SN");
            String sourceId = notify.getStr("SourceID");
            String targetId = notify.getStr("TargetID");
            log.info("Broadcast Notify SourceID {} TargetID {}",sourceId,targetId);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            PlatformChannel platformChannel = platformChannelRepository.findByPlatformIdAndChannelId(userId, targetId);
            if(platformChannel == null){
                log.error("no platformChannel userId {} TargetID {}",userId,targetId);
                sipServer.response403(requestEvent);
                return;
            }
            // 第一步 向上级回复xml
            SipResult<?> result1 = sipPlatformSend.responseBroadcast(userId, platformChannel.getDeviceId(), platformChannel.getChannelId(), sn, "OK");
            if(!result1.isSuccess()){
                log.error("platform broadcast step 1 err {}",result1.getMessage());
                return;
            }
            // 第二步 向下级发起广播
            SipResult<BroadcastDto> result2 = sipDeviceSend.broadcast(platformChannel.getDeviceId(), platformChannel.getChannelId());
            if(!result2.isSuccess()){
                log.error("platform broadcast step 2 err {}",result1.getMessage());
                return;
            }
            // 第三步 向上级发送广播invite
            SipResult<?> result3 = sipPlatformSend.broadcast(userId, platformChannel.getDeviceId(), platformChannel.getChannelId());
            if(!result3.isSuccess()){
                log.error("platform broadcast step 3 err {}",result1.getMessage());
                return;
            }
        }
    }
}
