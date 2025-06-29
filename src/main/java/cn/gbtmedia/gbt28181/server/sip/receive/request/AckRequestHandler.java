package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
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
public class AckRequestHandler implements IRequestHandler {

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.ACK);
    }

    @Override
    public void handle(RequestEvent requestEvent) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        CallIdHeader callIdHeader = (CallIdHeader)requestEvent.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        log.info("ack request userId {} callId {}", userId, callId);
        ClientInvite clientInvite = SipSessionManger.getInstance().getClientInvite(callId);
        if(clientInvite != null && clientInvite.getMediaClient() != null){
            // 启动流媒体客户端
            boolean started = clientInvite.getMediaClient().start();
        }
    }
}
