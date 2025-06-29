package cn.gbtmedia.gbt28181.server.sip.receive.response;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IResponseHandler;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;

/**
 * @author xqs
 */
@Component
public class CancelResponseHandler implements IResponseHandler {

    @Resource
    private SipServer sipServer;

    @Override
    public boolean support(ResponseEvent responseEvent) {
        CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
        String method = cseqHeader.getMethod();
        return Request.CANCEL.equals(method);
    }

    @Override
    public void handle(ResponseEvent responseEvent) {

    }
}
