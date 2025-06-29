package cn.gbtmedia.gbt28181.server.sip.receive.response;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IResponseHandler;
import cn.gbtmedia.gbt28181.server.sip.send.FutureContext;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.Vector;

/**
 * 下级响应视频请求
 * @author xqs
 */
@Slf4j
@Component
public class InviteResponseHandler implements IResponseHandler {

    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;

    @Override
    public boolean support(ResponseEvent responseEvent) {
        CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
        String method = cseqHeader.getMethod();
        return Request.INVITE.equals(method);
    }

    @Override
    @SneakyThrows
    public void handle(ResponseEvent responseEvent) {
        int code = responseEvent.getResponse().getStatusCode();
        SIPResponse response = (SIPResponse)responseEvent.getResponse();
        ResponseEventExt event = (ResponseEventExt)responseEvent;
        String callId = ((CallIdHeader)response.getHeader(CallIdHeader.NAME)).getCallId();
        log.info("Invite Response future callBack code {} callId {}", code,callId);;
        FutureContext.callBack(callId, responseEvent);
        ServerInvite invite = SipSessionManger.getInstance().getServerInviteByCallId(callId);
        if (code == Response.OK && invite != null) {
            checkSsrc(responseEvent, invite);
            sipServer.sendAckRequest(responseEvent);
            invite.setCallId(response.getCallIdHeader().getCallId());
            invite.setFromTag(response.getFromTag());
            invite.setToTag(response.getToTag());
            invite.setViaBranch(response.getTopmostViaHeader().getBranch());
            invite.setInviteAck(true);
            invite.getMediaServer().setSsrc(invite.getSsrc());
            // tcpActive 服务器连接到设备拉流
            if(invite.getMediaTransport().equals(MediaTransport.tcpActive.name())){
                // 解析sdp信息 , 默认不支持y=参数，先移除
                String contentString = new String(response.getRawContent());
                int ssrcIndex = contentString.indexOf("y=");
                SessionDescription sdp;
                if (ssrcIndex >= 0) {
                    String substring = contentString.substring(0, contentString.indexOf("y="));
                    sdp = SdpFactory.getInstance().createSessionDescription(substring);
                } else {
                    sdp = SdpFactory.getInstance().createSessionDescription(contentString);
                }
                Vector<?> mediaDescriptions = sdp.getMediaDescriptions(true);
                MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(0);
                Media media = mediaDescription.getMedia();
                // 从sdp中获取设备拉流的ip和端口
                String mediaIp = sdp.getConnection().getAddress();
                int mediaPort = media.getMediaPort();
                invite.setMediaIp(mediaIp);
                invite.setMediaPort(mediaPort);
                log.info("invite mediaTransport tcpActive callId {} mediaIp {} mediaPort {}",callId, mediaIp, mediaPort);
                MediaServer mediaServer = invite.getMediaServer();
                mediaServer.setMediaIp(mediaIp);
                mediaServer.setMediaPort(mediaPort);
                mediaServer.start();
            }
        }
    }

    /**
     *  ssrc 验证 下级有可能会自定义ssrc
     */
    private void checkSsrc(ResponseEvent responseEvent, ServerInvite invite){
        String content = new String(responseEvent.getResponse().getRawContent());
        int ssrcIndex = content.indexOf("y=");
        if (ssrcIndex >= 0) {
            String newSsrc = content.substring(ssrcIndex + 2, ssrcIndex + 12);
            if (!invite.getSsrc().equals(newSsrc)) {
                log.warn("ssrc changed ssrc {} newSsrc {} callId {}",invite.getSsrc(),newSsrc,invite.getCallId());
                // 多端口可以不用重置ssrc，是按端口区分流 ,单端口也不重置了，直接报错
                String mediaModel = ServerConfig.getInstance().getGbt28181().getMediaModel();
                if(mediaModel.equals("single")){
                    throw new RuntimeException("single mediaModel not support ssrc change ");
                }else if(mediaModel.equals("multiple")){
                    return;
                }
                boolean reset = SipSessionManger.getInstance().resetServerInviteSsrc(invite, newSsrc);
                if(reset){
                    invite.getMediaServer().resetSsrc(newSsrc);
                    log.info("ssrc reset success {}->{}", invite.getSsrc(), newSsrc);
                }else {
                    log.error("ssrc reset fail {}->{}",invite.getSsrc(), newSsrc);
                }
            }
        }
    }

}
