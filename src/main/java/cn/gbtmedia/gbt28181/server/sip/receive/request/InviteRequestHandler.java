package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.media.MediaManger;
import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.dto.StreamDto;
import cn.gbtmedia.gbt28181.dto.TalkDto;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.FutureContext;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipParam;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sip.message.SIPResponse;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.util.Date;
import java.util.Set;
import java.util.Vector;

/**
 * @author xqs
 */
@Slf4j
@Component
public class InviteRequestHandler implements IRequestHandler {

    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;
    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.INVITE);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        String userId = SipUtil.getUserIdFromFromHeader(request);
        Device device = deviceRepository.findByDeviceId(userId);
        // 收到下级发起的 Invite ，需要服务器发送语音到下级
        if(device != null){
            handDeviceBroadcast(requestEvent, device);
            return;
        }
        // 收到上级下发的 Invite 级联
        Platform platform = platformRepository.findByPlatformId(userId);
        if(platform != null && platform.getEnable() == 1){
            handPlatformInvite(requestEvent, platform);
            return;
        }
        log.error("Invite no device or platform not enable userId {}",userId);
        sipServer.response403(requestEvent);
    }

    @SneakyThrows
    private void handDeviceBroadcast(RequestEvent requestEvent,Device device){
        log.info("handDeviceBroadcast deviceId {}",device.getDeviceId());
        Request request = requestEvent.getRequest();
        CallIdHeader callIdHeader = (CallIdHeader)requestEvent.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        String deviceId = device.getDeviceId();
        String channelId = SipUtil.getChannelIdFromHeader(request);
        Set<String> keys = FutureContext.allKeys();
        String key = keys.stream()
                .filter(k-> k.equals("broadcast_" + deviceId + "_" + channelId) || k.startsWith("broadcast_" + deviceId))
                .findFirst().orElse(null);
        if(key == null){
            log.info("Device Broadcast no task ! deviceId {}",deviceId);
            sipServer.response403(requestEvent);
            return;
        }
        ClientInvite clientInvite = new ClientInvite();
        clientInvite.setDeviceId( key.split("_")[1]);
        clientInvite.setChannelId( key.split("_")[2]);
        clientInvite.setMediaType(MediaType.broadcast);
        // 回复181
        log.info("Device Broadcast send 181 deviceId {} channelId {} ",clientInvite.getDeviceId(), clientInvite.getChannelId());
        sipServer.response181(requestEvent);
        // 解析sdp信息 , 默认不支持y=参数，先移除
        String content = new String(request.getRawContent());
        int ssrcIndex = content.indexOf("y=");
        String ssrc;
        SessionDescription sdp;
        if (ssrcIndex >= 0) {
            ssrc = content.substring(ssrcIndex + 2, ssrcIndex + 12);
            String substring = content.substring(0, content.indexOf("y="));
            sdp = SdpFactory.getInstance().createSessionDescription(substring);
        } else {
            ssrc = "0000000000";
            sdp = SdpFactory.getInstance().createSessionDescription(content);
        }
        Vector<?> mediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(0);
        Media media = mediaDescription.getMedia();
        String ip = sdp.getConnection().getAddress();
        int port = media.getMediaPort();
        String protocol = media.getProtocol();
        String mediaFormat = String.valueOf(media.getMediaFormats(false).get(0)) ;
        String mediaTransport = MediaTransport.udp.name();
        if ("TCP/RTP/AVP".equals(protocol)) {
            mediaTransport = MediaTransport.tcpPassive.name();
            String setup = mediaDescription.getAttribute("setup");
            if ("active".equals(setup)) {
                mediaTransport = MediaTransport.tcpActive.name();
            }
        }
        clientInvite.setSsrc(ssrc);
        clientInvite.setMediaTransport(mediaTransport);
        // 创建流媒体客户端
        MediaParam mediaParam = new MediaParam();
        mediaParam.setMediaType(MediaType.broadcast.name());
        mediaParam.setCallId(callId);
        mediaParam.setSsrc(ssrc);
        mediaParam.setMediaTransport(mediaTransport);
        MediaClient mediaClient = MediaManger.getInstance().createClient(mediaParam);
        if(mediaClient == null){
            log.error("Device Broadcast no mediaClient");
            sipServer.response400(requestEvent);
            return;
        }
        clientInvite.setHttpFlv(mediaClient.getHttpFlv());
        clientInvite.setMediaClient(mediaClient);
        mediaClient.setSsrc(ssrc);
        // tcpActive 设备主动发过来
        if(MediaTransport.tcpActive.name().equals(mediaTransport)){
            sdp.getConnection().setAddress(mediaClient.getMediaIp());
            media.setMediaPort(mediaClient.getMediaPort());
        }else {
            mediaClient.setMediaIp(ip);
            mediaClient.setMediaPort(port);
        }
        String sdpStr = sdp.toString() +  ("y=" + ssrc + "\r\n");
        // 回复sdp
        log.info("Device Broadcast send sdp deviceId {} channelId {} mediaTransport {} mediaIp {} mediaPort {}",clientInvite.getDeviceId(),
                clientInvite.getChannelId(),clientInvite.getMediaTransport(),mediaClient.getMediaIp(),mediaClient.getMediaPort());
        SipParam sipParam = new SipParam();
        sipParam.setRequest(request);
        sipParam.setSipId(device.getDeviceId());
        sipParam.setSipIp(device.getSipIp());
        sipParam.setSipPort(device.getSipPort());
        sipParam.setSipTransport(device.getSipTransport());
        sipParam.setContent(sdpStr);
        sipParam.setContentType("SDP");
        try {
            sipServer.responseContent(sipParam);
        }catch (Exception ex){
            // 回复SDP失败，关闭流媒体
            mediaClient.stop();
            log.error("broadcast responseContent ex",ex);
            return;
        }
        SIPResponse response = (SIPResponse) sipParam.getResponse();
        clientInvite.setCallId(response.getCallIdHeader().getCallId());
        clientInvite.setFromTag(response.getFromTag());
        clientInvite.setToTag(response.getToTag());
        clientInvite.setViaBranch(response.getTopmostViaHeader().getBranch());
        clientInvite.setInviteAck(true);
        mediaClient.setCallId(clientInvite.getCallId());
        SipSessionManger.getInstance().putClientInvite(clientInvite);
        log.info("broadcast future callBack key {}",key);
        FutureContext.callBack(key, clientInvite);
    }

    @SneakyThrows
    private void handPlatformInvite(RequestEvent requestEvent, Platform platform) {
        log.info("handPlatformInvite platformId {}", platform.getPlatformId());
        CallIdHeader callIdHeader = (CallIdHeader)requestEvent.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        Request request = requestEvent.getRequest();
        String platformId = platform.getPlatformId();
        String channelId = SipUtil.getChannelIdFromHeader(request);
        PlatformChannel platformChannel = platformChannelRepository.findByPlatformIdAndChannelId(platformId, channelId);
        if(platformChannel == null){
            log.info("platformInvite no channel platformId {} channelId {}",platformId,channelId);
            sipServer.response403(requestEvent);
            return;
        }
        String deviceId = platformChannel.getDeviceId();
        // 回复181
        log.info("platformInvite send 181 platformId {} channelId {}", platformId, channelId);
        sipServer.response181(requestEvent);
        // 解析sdp信息 , 默认不支持y=参数，先移除
        String content = new String(request.getRawContent());
        int ssrcIndex = content.indexOf("y=");
        String ssrc;
        SessionDescription sdp;
        if (ssrcIndex >= 0) {
            ssrc = content.substring(ssrcIndex + 2, ssrcIndex + 12);
            String substring = content.substring(0, content.indexOf("y="));
            sdp = SdpFactory.getInstance().createSessionDescription(substring);
        } else {
            ssrc = "0000000000";
            sdp = SdpFactory.getInstance().createSessionDescription(content);
        }
        String sessionName = sdp.getSessionName().getValue();
        long startTime = 0;
        long stopTime = 0;
        if (sdp.getTimeDescriptions(false) != null && !sdp.getTimeDescriptions(false).isEmpty()) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl)(sdp.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField)timeDescription.getTime();
            startTime = startTimeFiled.getStartTime() * 1000;
            stopTime = startTimeFiled.getStopTime() * 1000;
        }
        Vector<?> mediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(0);
        String downloadspeedStr = ObjectUtil.defaultIfEmpty("1",mediaDescription.getAttribute("downloadspeed"));
        int downloadspeed = Integer.parseInt(downloadspeedStr);
        Media media = mediaDescription.getMedia();
        String ip = sdp.getConnection().getAddress();
        int port = media.getMediaPort();
        String protocol = media.getProtocol();
        String mediaTransport = MediaTransport.udp.name();
        if ("TCP/RTP/AVP".equals(protocol)) {
            mediaTransport = MediaTransport.tcpPassive.name();
            String setup = mediaDescription.getAttribute("setup");
            if ("active".equals(setup)) {
                mediaTransport = MediaTransport.tcpActive.name();
            }
        }
        ServerInvite serverInvite = null;
        // 不通的类型向下级发起
        if(sessionName.equalsIgnoreCase("play")){
            SipResult<StreamDto> result = sipDeviceSend.play(deviceId, channelId);
            if(!result.isSuccess()){
                sipServer.response400(requestEvent);
                return;
            }
            serverInvite = SipSessionManger.getInstance().getServerInvite(result.getData().getSsrc());
        }
        if(sessionName.equalsIgnoreCase("playback")){
            SipResult<StreamDto> result = sipDeviceSend.playback(deviceId, channelId,new DateTime(startTime),new Date(stopTime));
            if(!result.isSuccess()){
                sipServer.response400(requestEvent);
                return;
            }
            serverInvite = SipSessionManger.getInstance().getServerInvite(result.getData().getSsrc());
        }
        if(sessionName.equalsIgnoreCase("download")){
            SipResult<StreamDto> result = sipDeviceSend.download(deviceId, channelId,new DateTime(startTime),new Date(stopTime),downloadspeed);
            if(!result.isSuccess()){
                sipServer.response400(requestEvent);
                return;
            }
            serverInvite = SipSessionManger.getInstance().getServerInvite(result.getData().getSsrc());
        }
        if(sessionName.equalsIgnoreCase("talk")){
            SipResult<TalkDto> result = sipDeviceSend.talk(deviceId, channelId);
            if(!result.isSuccess()){
                sipServer.response400(requestEvent);
                return;
            }
            serverInvite = SipSessionManger.getInstance().getServerInvite(result.getData().getSsrc());
        }
        if(serverInvite == null){
            log.info("platformInvite no ServerInvite platformId {} channelId {}",platformId,channelId);
            sipServer.response400(requestEvent);
            return;
        }
        ClientInvite clientInvite = new ClientInvite();
        serverInvite.getClientInvites().add(clientInvite);
        clientInvite.setServerInvite(serverInvite);
        clientInvite.setPlatformId(platformId);
        clientInvite.setDeviceId(deviceId);
        clientInvite.setChannelId(channelId);
        clientInvite.setMediaTransport(mediaTransport);
        clientInvite.setMediaType(serverInvite.getMediaType());
        // 创建流媒体客户端
        MediaParam mediaParam = new MediaParam();
        mediaParam.setMediaType(serverInvite.getMediaType().name());
        mediaParam.setCallId(callId);
        mediaParam.setSsrc(ssrc);
        mediaParam.setMediaTransport(mediaTransport);
        mediaParam.setMediaServer(serverInvite.getMediaServer());
        MediaClient mediaClient = MediaManger.getInstance().createClient(mediaParam);
        if(mediaClient == null){
            log.error("platformInvite no mediaClient");
            sipServer.response400(requestEvent);
            return;
        }
        clientInvite.setHttpFlv(mediaClient.getHttpFlv());
        clientInvite.setMediaClient(mediaClient);
        mediaClient.setSsrc(ssrc);
        // tcpActive 设备主动发过来
        if(MediaTransport.tcpActive.name().equals(mediaTransport)){
            sdp.getConnection().setAddress(mediaClient.getMediaIp());
            media.setMediaPort(mediaClient.getMediaPort());
        }else {
            mediaClient.setMediaIp(ip);
            mediaClient.setMediaPort(port);
        }
        String sdpStr = sdp.toString() +  ("y=" + ssrc + "\r\n");
        // 回复sdp
        log.info("platformInvite send sdp sessionName {} platformId {} deviceId {} channelId {} " +
                        "mediaTransport {} mediaIp {} mediaPort {}",
                sessionName,platformId, deviceId,channelId,mediaTransport, mediaClient.getMediaIp(), mediaClient.getMediaPort());
        SipParam sipParam = new SipParam();
        sipParam.setRequest(request);
        sipParam.setSipId(platform.getPlatformId());
        sipParam.setSipIp(platform.getSipIp());
        sipParam.setSipPort(platform.getSipPort());
        sipParam.setSipTransport(platform.getSipTransport());
        sipParam.setContent(sdpStr);
        sipParam.setContentType("SDP");
        try {
            sipServer.responseContent(sipParam);
        }catch (Exception ex){
            // 回复SDP失败，关闭流媒体
            mediaClient.stop();
            log.error("platformInvite responseContent ex",ex);
            return;
        }
        serverInvite.getMediaServer().addMediaClient(mediaClient);
        mediaClient.setMediaServer(serverInvite.getMediaServer());
        SIPResponse response = (SIPResponse) sipParam.getResponse();
        clientInvite.setCallId(response.getCallIdHeader().getCallId());
        clientInvite.setFromTag(response.getFromTag());
        clientInvite.setToTag(response.getToTag());
        clientInvite.setViaBranch(response.getTopmostViaHeader().getBranch());
        mediaClient.setCallId(clientInvite.getCallId());
        SipSessionManger.getInstance().putClientInvite(clientInvite);
    }
}
