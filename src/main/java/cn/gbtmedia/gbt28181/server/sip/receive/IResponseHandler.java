package cn.gbtmedia.gbt28181.server.sip.receive;

import javax.sip.ResponseEvent;

/**
 * @author xqs
 */
public interface IResponseHandler {

    boolean support(ResponseEvent responseEvent);

    void handle(ResponseEvent responseEvent);

}
