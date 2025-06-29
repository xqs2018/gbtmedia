package cn.gbtmedia.gbt28181.server.sip.receive.response;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IResponseHandler;
import gov.nist.javax.sip.message.SIPResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;

/**
 * @author xqs
 */
@Slf4j
@Component
public class NotifyResponseHandler implements IResponseHandler {

    @Resource
    private SipServer sipServer;

    @Override
    public boolean support(ResponseEvent responseEvent) {
        CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
        String method = cseqHeader.getMethod();
        return Request.NOTIFY.equals(method);
    }

    @Override
    public void handle(ResponseEvent responseEvent) {
        int code = responseEvent.getResponse().getStatusCode();
        SIPResponse response = (SIPResponse)responseEvent.getResponse();
        String callId = response.getCallIdHeader().getCallId();
        log.info("Notify Response code {} callId {}", code, callId);
    }
}
