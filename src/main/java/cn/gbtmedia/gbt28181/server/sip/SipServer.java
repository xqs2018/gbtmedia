package cn.gbtmedia.gbt28181.server.sip;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.server.sip.util.SipParam;
import cn.gbtmedia.gbt28181.server.sip.receive.SipReceive;
import cn.gbtmedia.gbt28181.server.sip.util.DigestServerAuthenticationHelper;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.util.WvpSipDate;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xqs
 */
@Order(2)
@Slf4j
@Getter
@Component
public class SipServer implements ApplicationRunner, SipListener {

    private String sipIp;
    private int sipPort;
    private String sipId;
    private String sipDomain;
    private SipFactory sipFactory;
    private SipStackImpl sipStack;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider tcpSipProvider;
    private SipProvider udpSipProvider;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ServerConfig serverConfig = ServerConfig.getInstance();
        sipIp = serverConfig.getAccessIp();
        sipPort = serverConfig.getGbt28181().getSipPort();
        sipId = serverConfig.getGbt28181().getSipId();
        sipDomain =  serverConfig.getGbt28181().getSipDomain();
        log.info("sipServer start port {}",sipPort);
        SipFactory sipFactory = SipFactory.getInstance();
        this.sipFactory = sipFactory;
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "GBT28181_SIP");
        properties.setProperty("javax.sip.IP_ADDRESS", "0.0.0.0");
        // 关闭自动会话，不使用Transaction发消息，全部手动发消息 ，防止订阅报错 SIP/2.0 481 Subscription does not exist
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
        // 日志记录消息内容
        properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        // 创建信令栈
        this.sipStack = (SipStackImpl) sipFactory.createSipStack(properties);
        this.addressFactory = sipFactory.createAddressFactory();
        this.headerFactory = sipFactory.createHeaderFactory();
        this.messageFactory = sipFactory.createMessageFactory();
        //同时监听 upd 和 tcp 端口
        try {
            ListeningPoint tcpListeningPoint = this.sipStack.createListeningPoint("0.0.0.0", sipPort,"TCP");
            ListeningPoint udpListeningPoint = this.sipStack.createListeningPoint("0.0.0.0", sipPort,"UDP");
            //设置消息监听器
            this.tcpSipProvider = this.sipStack.createSipProvider(tcpListeningPoint);
            this.tcpSipProvider.addSipListener(this);
            this.udpSipProvider = this.sipStack.createSipProvider(udpListeningPoint);
            this.udpSipProvider.addSipListener(this);
        }catch (Exception ex){
            log.error("SipServer Start Error Port {}",sipPort,ex);
            int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
            System.exit(exitCode);
        }
    }

    private static final ExecutorService GBT28181_SIP_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-gbt28181-sip-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("gbt28181 sip pool ex t {}", t, e))
                            .factory());

    @Override
    public void processRequest(RequestEvent requestEvent) {
        //sip默认一个线程处理会阻塞
        GBT28181_SIP_POOL.execute(()->{
            Request request = requestEvent.getRequest();
            try {
                if(log.isDebugEnabled()){
                    log.debug("processRequest <<<<< \n {}", request);
                }
                SipReceive.handle(requestEvent);
            }catch (Exception ex){
                log.error("processRequest err request \n {}", request, ex);
            }
        });
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        //sip默认一个线程处理会阻塞
        GBT28181_SIP_POOL.execute(()->{
            Response response = responseEvent.getResponse();
            try {
                if(log.isDebugEnabled()){
                    log.debug("processResponse <<<<< \n {}", response);
                }
                int status = response.getStatusCode();
                if (status >= 200 && status < 300) {
                    SipReceive.handle(responseEvent);
                } else if (status >= 100 && status < 200) {
                    // 无需回复的响应，如101、180等
                    if(log.isDebugEnabled()){
                        log.debug("processResponse status: {}" ,status);
                    }
                } else {
                    log.error("processResponse err responseEvent \n{}", response);
                    if (responseEvent.getDialog() != null) {
                        responseEvent.getDialog().delete();
                    }
                    SipReceive.handle(responseEvent);
                }
            }catch (Exception ex){
                log.error("processResponse err response \n {}", response, ex);
            }
        });
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.error("processTimeout timeoutEvent \n{}",timeoutEvent);
        ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
        if (clientTransaction != null) {
            Request request = clientTransaction.getRequest();
            log.error("processTimeout request \n {}",request);
        }
        SpringUtil.getApplicationContext().publishEvent(timeoutEvent);
    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {
        log.error("processIOException {}",ioExceptionEvent);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }

    public void sendResponse(Response response) throws Exception {
        ViaHeader reqViaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        if(log.isDebugEnabled()){
            log.debug("sendResponse >>>>> \n{}",response);
        }
        if ("TCP".equals(transport)) {
            tcpSipProvider.sendResponse(response);

        } else if ("UDP".equals(transport)) {
            udpSipProvider.sendResponse(response);
        }
    }

    public void sendRequest(Request request) throws Exception {
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = viaHeader.getTransport();
        if(log.isDebugEnabled()){
            log.debug("sendRequest >>>>> \n{}",request);
        }
        if ("TCP".equals(transport)) {
            tcpSipProvider.sendRequest(request);

        } else if ("UDP".equals(transport)) {
            udpSipProvider.sendRequest(request);
        }
    }


    public void response181(RequestEvent requestEvent) throws Exception {
        Request request = requestEvent.getRequest();
        if (((SIPRequest)request).getToHeader().getTag() == null) {
            ((SIPRequest)request).getToHeader().setTag(SipUtil.getNewTag());
        }
        Response response = sipFactory.createMessageFactory().createResponse(Response.CALL_IS_BEING_FORWARDED, request);
        sendResponse(response);
    }


    public void response200(RequestEvent requestEvent) throws Exception{
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.OK, request);
        sendResponse(response);
    }


    public void responseRegister200(RequestEvent requestEvent) throws Exception {
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.OK, request);
        SIPDateHeader dateHeader = new SIPDateHeader();
        WvpSipDate wvpSipDate = new WvpSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(wvpSipDate);
        response.addHeader(dateHeader);
        response.addHeader(request.getHeader(ContactHeader.NAME));
        response.addHeader(request.getExpires());
        sendResponse(response);
    }

    public void response400(RequestEvent requestEvent) throws Exception  {
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.BAD_REQUEST, request);
        sendResponse(response);
    }

    public void response401(RequestEvent requestEvent) throws Exception {
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.UNAUTHORIZED, request);
        new DigestServerAuthenticationHelper().generateChallenge(sipFactory.createHeaderFactory(), response,sipDomain);
        sendResponse(response);
    }

    public void response403(RequestEvent requestEvent) throws Exception {
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.FORBIDDEN, request);
        response.setReasonPhrase("FORBIDDEN");
        sendResponse(response);
    }

    public void response500(RequestEvent requestEvent) throws Exception {
        Request request = requestEvent.getRequest();
        Response response = sipFactory.createMessageFactory().createResponse(Response.SERVER_INTERNAL_ERROR,request);
        response.setReasonPhrase("sip err");
        sendResponse(response);
    }

    public void responseContent(SipParam param)  throws Exception{
        //  响应invite消息时, To : tag= 不能为空
        if (((SIPRequest)param.getRequest()).getToHeader().getTag() == null) {
            ((SIPRequest)param.getRequest()).getToHeader().setTag(IdUtil.fastSimpleUUID());
        }
        Response response = sipFactory.createMessageFactory().createResponse(Response.OK, param.getRequest());
        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", param.getContentType());
        response.setContent(param.getContent(), contentTypeHeader);
        // 兼容国标中的使用编码@域名作为RequestURI的情况
        SipURI sipURI = (SipURI)param.getRequest().getRequestURI();
        if (sipURI.getPort() == -1) {
            sipURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        }
        Address concatAddress = sipFactory.createAddressFactory().createAddress(
                sipFactory.createAddressFactory().createSipURI(sipURI.getUser(),  sipURI.getHost()+":"+sipURI.getPort()
                ));
        response.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
        sendResponse(response);
        param.setResponse(response);
    }

    public void sendMessageRequest(SipParam param) throws Exception {
        String viaTag = "z9hG4bK" + IdUtil.fastSimpleUUID();
        String fromTag = IdUtil.fastSimpleUUID();
        String toTag = null;
        String callId = Optional.ofNullable(param.getCallId()).orElse(IdUtil.fastSimpleUUID());
        // sipuri
        SipURI requestURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(callId);
        // from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
        // to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);
        // Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
        // ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);

        Request request = sipFactory.createMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);
        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
        request.setContent(param.getContent(), contentTypeHeader);
        MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
        // 设置编码， 防止中文乱码
        messageFactory.setDefaultContentEncodingCharset("gb2312");

        sendRequest(request);
    }

    public void sendSubscribeRequest(SipParam param) throws Exception {
        String viaTag = "z9hG4bK" + IdUtil.fastSimpleUUID();
        String fromTag = IdUtil.fastSimpleUUID();
        String toTag = null;
        String callId = Optional.ofNullable(param.getCallId()).orElse(IdUtil.fastSimpleUUID());
        // sipuri
        SipURI requestURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(callId);
        // from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
        // to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);
        // Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        // ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.SUBSCRIBE), Request.SUBSCRIBE);

        Request request = sipFactory.createMessageFactory().createRequest(requestURI, Request.SUBSCRIBE, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

        // Expires
        ExpiresHeader expireHeader = sipFactory.createHeaderFactory().createExpiresHeader(3600);
        request.addHeader(expireHeader);

        // Event
        EventHeader eventHeader = sipFactory.createHeaderFactory().createEventHeader("Catalog");

        String eventId = IdUtil.fastSimpleUUID();
        eventHeader.setEventId(eventId);
        request.addHeader(eventHeader);

        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
        request.setContent(param.getContent(), contentTypeHeader);

        sendRequest(request);
    }

    public void sendInviteRequest(SipParam param) throws Exception {
        String viaTag = "z9hG4bK" + IdUtil.fastSimpleUUID();
        String fromTag = IdUtil.fastSimpleUUID();
        String toTag = null;
        // sipuri
        SipURI requestLine = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(param.getCallId());
        // from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
        // to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,toTag);

        //Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        //ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.INVITE), Request.INVITE);
        Request request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));
        // Subject
        SubjectHeader subjectHeader = sipFactory.createHeaderFactory().createSubjectHeader(String.format("%s:%s,%s:%s", param.getChannelId(), param.getSsrc(), sipId, 0));
        request.addHeader(subjectHeader);
        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");
        request.setContent(param.getContent(), contentTypeHeader);

        sendRequest(request);
    }

    public void sendAckRequest(ResponseEvent responseEvent) throws Exception {
        ResponseEventExt event = (ResponseEventExt)responseEvent;
        SIPResponse response = (SIPResponse)responseEvent.getResponse();
        String contentString = new String(response.getRawContent());
        // jainSip不支持y=字段， 移除以解析。
        int ssrcIndex = contentString.indexOf("y=");
        // 检查是否有y字段
        SessionDescription sdp;
        if (ssrcIndex >= 0) {
            //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段
            String substring = contentString.substring(0, contentString.indexOf("y="));
            sdp = SdpFactory.getInstance().createSessionDescription(substring);
        } else {
            sdp = SdpFactory.getInstance().createSessionDescription(contentString);
        }
        SipURI requestUri = sipFactory.createAddressFactory().createSipURI(sdp.getOrigin().getUsername(), event.getRemoteIpAddress() + ":" + event.getRemotePort());

        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, response.getTopmostViaHeader().getTransport(), SipUtil.getNewViaTag());
        viaHeaders.add(viaHeader);

        //Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        //ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(response.getCSeqHeader().getSeqNumber(), Request.ACK);

        Request request = sipFactory.createMessageFactory().createRequest(requestUri, Request.ACK, response.getCallIdHeader(), cSeqHeader, response.getFromHeader(), response.getToHeader(), viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

        sendRequest(request);
    }

    public void sendInfoRequest(SipParam param) throws Exception {
        String viaTag = param.getViaBranch();
        String fromTag = param.getFromTag();
        String toTag = param.getToTag();

        // sipuri
        SipURI requestLine = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(param.getCallId());

        //from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);

        //to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,	toTag);

        //Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        //ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.INFO), Request.INFO);

        Request request = sipFactory.createMessageFactory().createRequest(requestLine, Request.INFO, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSRTSP");
        request.setContent(param.getContent(), contentTypeHeader);

        sendRequest(request);
    }

    public void sendByeRequest(SipParam param) throws Exception {
        String viaTag = param.getViaBranch();
        String fromTag = param.getFromTag();
        String toTag = param.getToTag();

        // sipuri
        SipURI requestLine = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(param.getCallId());

        //from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);

        //to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getChannelId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,	toTag);

        //Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        //ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.BYE), Request.BYE);

        Request request = sipFactory.createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));


        sendRequest(request);
    }

    public void sendRegisterRequest(SipParam param) throws Exception{
        String viaTag = "FromRegister" + System.currentTimeMillis();
        String fromTag =  "z9hG4bK-" +IdUtil.fastSimpleUUID();
        String toTag = param.getToTag();

        //请求行
        SipURI requestLine = sipFactory.createAddressFactory().createSipURI(param.getSipIp(), param.getSipIp() + ":" + param.getSipPort());
        //via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), viaTag);
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        // callId
        CallIdHeader callIdHeader = param.getSipTransport().equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId(): udpSipProvider.getNewCallId();
        callIdHeader.setCallId(param.getCallId());

        //from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
        //to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp() + ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress,null);

        //Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);

        //ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.REGISTER), Request.REGISTER);
        Request request = sipFactory.createMessageFactory().createRequest(requestLine, Request.REGISTER, callIdHeader,
                cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

        ExpiresHeader expires = sipFactory.createHeaderFactory().createExpiresHeader(param.getExpires());
        request.addHeader(expires);

        // 无需密码
        String password = param.getPassword();
        if(ObjectUtil.isEmpty(password)){
            sendRequest(request);
            return;
        }

        // 需要密码
        SipURI requestURI =  sipFactory.createAddressFactory().createSipURI(param.getSipIp(), param.getSipIp() + ":" + param.getSipPort());
        WWWAuthenticateHeader www = param.getWww();
        if(www == null){
            AuthorizationHeader authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader("Digest");
            authorizationHeader.setUsername(sipId);
            authorizationHeader.setURI(requestURI);
            authorizationHeader.setAlgorithm("MD5");
            request.addHeader(authorizationHeader);
            sendRequest(request);
            return;
        }
        String realm = www.getRealm();
        String nonce = www.getNonce();
        String scheme = www.getScheme();

        // 参考 https://blog.csdn.net/y673533511/article/details/88388138
        // qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
        String qop = www.getQop();

        callIdHeader.setCallId(param.getCallId());

        String cNonce = null;
        String nc = "00000001";
        if (qop != null) {
            if ("auth".equals(qop)) {
                // 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
                // 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
                cNonce = UUID.randomUUID().toString();

            }else if ("auth-int".equals(qop)){
                // TODO
            }
        }
        String HA1 = DigestUtils.md5DigestAsHex((sipId + ":" + realm + ":" + param.getPassword()).getBytes());
        String HA2=DigestUtils.md5DigestAsHex((Request.REGISTER + ":" + requestURI.toString()).getBytes());

        StringBuffer reStr = new StringBuffer(200);
        reStr.append(HA1);
        reStr.append(":");
        reStr.append(nonce);
        reStr.append(":");
        if (qop != null) {
            reStr.append(nc);
            reStr.append(":");
            reStr.append(cNonce);
            reStr.append(":");
            reStr.append(qop);
            reStr.append(":");
        }
        reStr.append(HA2);

        String RESPONSE = DigestUtils.md5DigestAsHex(reStr.toString().getBytes());

        AuthorizationHeader authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader(scheme);
        authorizationHeader.setUsername(sipId);
        authorizationHeader.setRealm(realm);
        authorizationHeader.setNonce(nonce);
        authorizationHeader.setURI(requestURI);
        authorizationHeader.setResponse(RESPONSE);
        authorizationHeader.setAlgorithm("MD5");
        if (qop != null) {
            authorizationHeader.setQop(qop);
            authorizationHeader.setCNonce(cNonce);
            authorizationHeader.setNonceCount(1);
        }
        request.addHeader(authorizationHeader);

        ExpiresHeader expires1 = sipFactory.createHeaderFactory().createExpiresHeader(param.getExpires());
        request.addHeader(expires1);

        sendRequest(request);
    }

    public void sendNotifyRequest(SipParam param) throws Exception{
        // sipuri
        SipURI requestURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp()+ ":" + param.getSipPort());
        // via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = sipFactory.createHeaderFactory().createViaHeader(sipIp, sipPort, param.getSipTransport(), param.getViaBranch());
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        // from
        SipURI fromSipURI = sipFactory.createAddressFactory().createSipURI(sipId, sipIp + ":" + sipPort);
        Address fromAddress = sipFactory.createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = sipFactory.createHeaderFactory().createFromHeader(fromAddress, param.getToTag());
        // to
        SipURI toSipURI = sipFactory.createAddressFactory().createSipURI(param.getSipId(), param.getSipIp()+ ":" + param.getSipPort());
        Address toAddress = sipFactory.createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = sipFactory.createHeaderFactory().createToHeader(toAddress, param.getFromTag());

        // Forwards
        MaxForwardsHeader maxForwards = sipFactory.createHeaderFactory().createMaxForwardsHeader(70);
        // ceq
        CSeqHeader cSeqHeader = sipFactory.createHeaderFactory().createCSeqHeader(getCSeq(Request.NOTIFY),Request.NOTIFY);
        MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
        // 设置编码， 防止中文乱码
        messageFactory.setDefaultContentEncodingCharset("gb2312");

        CallIdHeader callIdHeader = sipFactory.createHeaderFactory().createCallIdHeader(param.getCallId());

        SIPRequest request  = (SIPRequest) messageFactory.createRequest(requestURI, Request.NOTIFY, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        EventHeader event = sipFactory.createHeaderFactory().createEventHeader(param.getEventType());
        request.addHeader(event);

        SubscriptionStateHeader active = sipFactory.createHeaderFactory().createSubscriptionStateHeader("active");
        request.setHeader(active);

        Address concatAddress = sipFactory.createAddressFactory().createAddress(sipFactory.createAddressFactory().createSipURI(sipId,  sipIp + ":" + sipPort));
        request.addHeader(sipFactory.createHeaderFactory().createContactHeader(concatAddress));

        ContentTypeHeader contentTypeHeader = sipFactory.createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
        request.setContent(param.getContent(), contentTypeHeader);

        sendRequest(request);
    }

    private final ConcurrentHashMap<String, AtomicLong> incr = new ConcurrentHashMap<>();

    /**
     * 获取全局递增的 getCSeq
     */
    public Long getCSeq(String method) {
        AtomicLong atomicLong = incr.computeIfAbsent("ALL", k -> new AtomicLong(1));
        return atomicLong.getAndIncrement();
    }


}
