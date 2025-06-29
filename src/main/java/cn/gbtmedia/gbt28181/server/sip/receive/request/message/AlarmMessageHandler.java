package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * @author xqs
 */
@Slf4j
public class AlarmMessageHandler implements MessageRequestHandler.Process{

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "Alarm".equals(data.getStr("CmdType"));
    }

    @Override
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        log.info("Alarm Message Request userId {}",userId);
    }
}
