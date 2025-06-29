package cn.gbtmedia.gbt28181.server.sip.util;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Subject;

import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.UUID;


/**
 * @author xqs
 */
public class SipUtil {

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromFromHeader(fromHeader);
    }
    public static String getUserIdFromToHeader(Response response) {
        ToHeader toHeader = (ToHeader)response.getHeader(ToHeader.NAME);
        return getUserIdFromToHeader(toHeader);
    }

    /**
     * 从subject读取channelId
     * */
    public static String getChannelIdFromHeader(Request request) {
        Header subject = request.getHeader("subject");
        if (subject == null) {
            return null;
        }
        return ((Subject) subject).getSubject().split(":")[0];
    }

    public static String getUserIdFromFromHeader(FromHeader fromHeader) {
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static String getUserIdFromToHeader(ToHeader toHeader) {
        AddressImpl address = (AddressImpl)toHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static  String getNewViaTag() {
        return "z9hG4bK" + System.currentTimeMillis();
    }

    public static String getNewFromTag(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getNewTag(){
        return String.valueOf(System.currentTimeMillis());
    }

    public static String getNewSn(){
        return String.valueOf((int) ((Math.random() * 9 + 1) * 10000000));
    }
}
