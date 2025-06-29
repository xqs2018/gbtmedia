package cn.gbtmedia.gbt28181.server.sip.receive;

import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.hutool.extra.spring.SpringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.header.CSeqHeader;
import java.util.Map;

/**
 * @author xqs
 */
@Slf4j
public class SipReceive {

    @SneakyThrows
    public static void handle(RequestEvent requestEvent){
        Map<String, IRequestHandler> handlerMap = SpringUtil.getBeansOfType(IRequestHandler.class);
        IRequestHandler handler = handlerMap.values().stream()
                .filter(v -> v.support(requestEvent))
                .findFirst().orElse(null);
        if(handler == null){
            // 没有对应处理器直接返回 400
            log.error("no IRequestHandler for request \n {}",requestEvent.getRequest());
            SpringUtil.getBean(SipServer.class).response400(requestEvent);
            return;
        }
        long timeMillis = System.currentTimeMillis();
        handler.handle(requestEvent);
        timeMillis = System.currentTimeMillis() - timeMillis;
        if(timeMillis > 10000){
            log.warn("SipReceive requestEvent handle too slow cost {} ms",timeMillis);
        }
    }

    @SneakyThrows
    public static void handle(ResponseEvent responseEvent){
        Map<String, IResponseHandler> handlerMap = SpringUtil.getBeansOfType(IResponseHandler.class);
        IResponseHandler handler = handlerMap.values().stream()
                .filter(v -> v.support(responseEvent))
                .findFirst().orElse(null);
        CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
        String method = cseqHeader.getMethod();
        if(handler == null){
            log.error("no IResponseHandler for response \n {}",responseEvent.getResponse());
            return;
        }
        long timeMillis = System.currentTimeMillis();
        handler.handle(responseEvent);
        timeMillis = System.currentTimeMillis() - timeMillis;
        if(timeMillis > 10000){
            log.warn("SipReceive responseEvent handle too slow cost {} ms",timeMillis);
        }
    }

}
