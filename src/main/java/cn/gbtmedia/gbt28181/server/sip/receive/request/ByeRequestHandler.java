package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;

/**
 * @author xqs
 */
@Slf4j
@Component
public class ByeRequestHandler implements IRequestHandler {

    @Resource
    private SipServer sipServer;
    @Resource
    private SipDeviceSend sipDeviceSend;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.BYE);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        // 先回复200
        sipServer.response200(requestEvent);
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        CallIdHeader callIdHeader = (CallIdHeader)requestEvent.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        log.info("receive bye request userId {} callId {}",userId, callId);
        ClientInvite clientInvite = SipSessionManger.getInstance().getClientInvite(callId);
        if(clientInvite != null){
            log.info("receive bye request clientInvite callId {} inviteType {}",callId, clientInvite.getMediaType());
            clientInvite.setSendBye(true);
            sipDeviceSend.stopClientInvite(clientInvite.getCallId());
        }
        ServerInvite serverInvite = SipSessionManger.getInstance().getServerInviteByCallId(callId);
        if(serverInvite != null){
            log.info("receive bye request serverInvite ssrc {} inviteType {}",serverInvite.getSsrc(),serverInvite.getMediaType());
            serverInvite.setSendBye(true);
            sipDeviceSend.stopServerInvite(serverInvite.getSsrc());
        }
    }
}
