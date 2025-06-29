package cn.gbtmedia.gbt28181.server.sip.receive;

import javax.sip.RequestEvent;

/**
 * @author xqs
 */
public interface IRequestHandler {

    boolean support(RequestEvent requestEvent);

    void handle(RequestEvent requestEvent);

}
