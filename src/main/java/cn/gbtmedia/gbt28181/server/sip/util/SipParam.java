package cn.gbtmedia.gbt28181.server.sip.util;

import lombok.Data;

import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * @author xqs
 */
@Data
public class SipParam {
    private String sipId;
    private String sipDomain;
    private String sipIp;
    private int sipPort;
    private String sipTransport;
    private String content;
    private String contentType;
    private String channelId;
    private String callId;
    private String ssrc;
    private String fromTag;
    private String toTag;
    private String viaBranch;
    private Request request;
    private Response response;
    private int expires;
    private String password;
    private WWWAuthenticateHeader www;
    private String eventType;
}
