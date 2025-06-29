package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.util.Map;

/**
 * @author xqs
 */
@Slf4j
@Component
public class MessageRequestHandler implements IRequestHandler {

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.MESSAGE);
    }

    public interface Process {

        boolean support(RequestEvent requestEvent, JSONObject message);

        void handle(RequestEvent requestEvent, JSONObject message);

    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        String textData = new String(requestEvent.getRequest().getRawContent(),"gb2312");
        JSONObject message = JSONUtil.parseFromXml(textData);
        Map<String, Process> handlerMap = SpringUtil.getBeansOfType(Process.class);
        Process process = handlerMap.values().stream()
                .filter(v -> v.support(requestEvent, message))
                .findFirst().orElse(null);
        if(process == null){
            // 没有对应处理器直接返回 400
            log.error("no MessageRequestProcess for request \n {}",requestEvent.getRequest());
            SpringUtil.getBean(SipServer.class).response400(requestEvent);
            return;
        }
        process.handle(requestEvent, message);
    }
}
