package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Component
public class MediaStatusMessageHandler implements MessageRequestHandler.Process {

    private DeviceRepository deviceRepository;
    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;
    @Resource
    private SipPlatformSend sipPlatformSend;

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "MediaStatus".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        //先回复200
        sipServer.response200(requestEvent);
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级发送推流完毕，主要是录像回放或下载
        // TODO 模拟设备callId和发起的不一样
        if(message.containsKey("Notify")){
            JSONObject notify = message.getJSONObject("Notify");
            if(!"121".equals(notify.getStr("NotifyType"))){
                return;
            }
            String channelId = notify.getStr("DeviceID");
            CallIdHeader callIdHeader = (CallIdHeader)requestEvent.getRequest().getHeader(CallIdHeader.NAME);
            String callId = callIdHeader.getCallId();
            log.info("MediaStatus Notify stop invite userId {} channelId {} callId {}",userId,channelId,callId);
            List<ClientInvite> clientInvites = new ArrayList<>();
            // 模拟设备callId和发起的不一样 模糊查找一下
            List<ServerInvite> playback = SipSessionManger.getInstance().getServerInviteList(userId, MediaType.playback);
            if(ObjectUtil.isNotEmpty(playback)){
                ServerInvite invite = playback.stream()
                        .filter(v -> v.getCallId().equals(callId)||v.getChannelId().equals(callId))
                        .findFirst().orElse(null);
                // 取最早的一个停止
                if(invite == null){
                    invite = playback.stream().sorted(Comparator.comparing(ServerInvite::getCreateTime)).toList().get(0);
                }
                clientInvites.addAll(invite.getClientInvites());
                sipDeviceSend.stopPlayback(invite.getSsrc());
            }
            List<ServerInvite> download = SipSessionManger.getInstance().getServerInviteList(userId, MediaType.download);
            if(ObjectUtil.isNotEmpty(download)){
                ServerInvite invite = download.stream()
                        .filter(v -> v.getCallId().equals(callId)||v.getChannelId().equals(callId))
                        .findFirst().orElse(null);
                // 取最早的一个停止
                if(invite == null){
                    invite = download.stream().sorted(Comparator.comparing(ServerInvite::getCreateTime)).toList().get(0);
                }
                clientInvites.addAll(invite.getClientInvites());
                sipDeviceSend.stopDownload(invite.getSsrc());
            }
            // 继续通知到上级
            for(ClientInvite clientInvite : clientInvites){
                SipResult<?> result = sipPlatformSend.notifyMediaStatus(clientInvite.getCallId());
            }
        }
    }
}
