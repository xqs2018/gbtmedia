package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.message.Request;

/**
 * @author xqs
 */
@Slf4j
@Component
public class CancelRequestHandler implements IRequestHandler {

    @Resource
    private SipServer sipServer;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.CANCEL);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        // 先回复200
        sipServer.response200(requestEvent);
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
    }
}
