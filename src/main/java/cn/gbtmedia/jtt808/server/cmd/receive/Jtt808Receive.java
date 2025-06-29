package cn.gbtmedia.jtt808.server.cmd.receive;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;


/**
 * @author xqs
 */
@Slf4j
public class Jtt808Receive {

    public static void handle(ClientSession session, Jtt808Message request){
        Map<String, IJtt808Receive> handlerMap = SpringUtil.getBeansOfType(IJtt808Receive.class);
        IJtt808Receive handler = handlerMap.values().stream()
                .filter(v -> v.support(request))
                .findFirst().orElse(null);
        if(handler == null){
            log.error("no IJtt808Receive for clientId {} messageId 0x{}",
                    session.getClientId(),Integer.toHexString(request.getMessageId()));
            return;
        }
        long timeMillis = System.currentTimeMillis();
        handler.handle(session, request);
        timeMillis = System.currentTimeMillis() - timeMillis;
        if(timeMillis > 200){
            log.warn("IJtt808Receive handle too slow cost {} ms clientId {} messageId 0x{}",
                    timeMillis,session.getClientId(),Integer.toHexString(request.getMessageId()));
        }
    }
}
