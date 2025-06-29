package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipParam;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gov.nist.javax.sip.message.SIPResponse;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.header.EventHeader;
import javax.sip.message.Request;

/**
 * @author xqs
 */
@Slf4j
@Component
public class SubscribeRequestHandler implements IRequestHandler {

    @Resource
    private SipServer sipServer;
    @Resource
    private PlatformRepository platformRepository;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.SUBSCRIBE);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        // 收到上级发起的订阅请求
        Request request = requestEvent.getRequest();
        String xmlStr = new String(request.getRawContent(), "gb2312");
        JSONObject jsonObject = JSONUtil.parseFromXml(xmlStr);
        String cmdType = jsonObject.getJSONObject("Query").getStr("CmdType");
        // 订阅通道
        if("Catalog".equals(cmdType)){
            processNotifyCatalogList(requestEvent, jsonObject);
        }
        // 其它不处理
        else {
            sipServer.response200(requestEvent);
        }
    }

    @SneakyThrows
    private void processNotifyCatalogList(RequestEvent requestEvent, JSONObject jsonObject) {
        Request request = requestEvent.getRequest();
        String userId = SipUtil.getUserIdFromFromHeader(request);
        log.info("Receive Catalog Subscribe Request userId {}",userId);
        Platform platform = platformRepository.findByPlatformId(userId);
        if(platform == null || platform.getEnable() == 0){
            log.error("no platform or not enable userId {} ",userId);
            sipServer.response403(requestEvent);
            return;
        }
        String sn = jsonObject.getStr("SN");
        String deviceId = jsonObject.getJSONObject("Query").getStr("DeviceID");
        StringBuilder params = new StringBuilder(200);
        params.append("<?xml version=\"1.0\" ?>\r\n");
        params.append("<Response>\r\n");
        params.append("<CmdType>Catalog</CmdType>\r\n");
        params.append("<SN>").append(sn).append("</SN>\r\n");
        params.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n");
        params.append("<Result>OK</Result>\r\n");
        params.append("</Response>\r\n");
        SipParam sipParam = new SipParam();
        sipParam.setRequest(request);
        sipParam.setSipId(platform.getPlatformId());
        sipParam.setSipIp(platform.getSipIp());
        sipParam.setSipPort(platform.getSipPort());
        sipParam.setSipTransport(platform.getSipTransport());
        sipParam.setContent(params.toString());
        sipParam.setContentType("MANSCDP+xml");
        sipServer.responseContent(sipParam);
        // 保存订阅信息
        SIPResponse response = (SIPResponse) sipParam.getResponse();
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        JSONObject info = new JSONObject();
        info.set("eventId", eventHeader.getEventId());
        info.set("eventType",eventHeader.getEventType());
        info.set("callId",response.getCallIdHeader().getCallId());
        info.set("fromTag",response.getFromTag());
        info.set("toTag",response.getToTag());
        info.set("viaBranch",response.getTopmostViaHeader().getBranch());
        platform.setSubscribeCatalogInfo(info.toString());
        log.info("platformId {} subscribeCatalogInfo {} ",userId ,info);
        platformRepository.save(platform);
    }
}
