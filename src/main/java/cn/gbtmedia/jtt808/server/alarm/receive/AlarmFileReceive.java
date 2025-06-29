package cn.gbtmedia.jtt808.server.alarm.receive;

import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessageData;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * @author xqs
 */
@Slf4j
public class AlarmFileReceive {

    public static void handle(AlarmFileSession session, Object request){
        Map<String, IAlarmFileReceive> handlerMap = SpringUtil.getBeansOfType(IAlarmFileReceive.class);
        IAlarmFileReceive handler = handlerMap.values().stream()
                .filter(v -> v.support(request))
                .findFirst().orElse(null);
        if(handler == null){
            if(request instanceof AlarmFileMessage808 a){
                log.error("no AlarmFileReceive for clientId {} messageId 0x{}",
                        session.getClientId(),Integer.toHexString(a.getMessageId()));
            }else {
                log.error("no AlarmFileReceive for clientId {} request {}",session.getClientId(), request);
            }
            return;
        }
        long timeMillis = System.currentTimeMillis();
        handler.handle(session, request);
        timeMillis = System.currentTimeMillis() - timeMillis;
        if(timeMillis > 200){
            if(request instanceof AlarmFileMessage808 a){
                log.warn("AlarmFileReceive handle too slow cost {} ms clientId {} messageId 0x{}",
                        timeMillis,session.getClientId(),Integer.toHexString(a.getMessageId()));
            }else {
                log.warn("AlarmFileReceive handle too slow cost {} ms clientId {} request {}",
                        timeMillis,session.getClientId(),request);
            }
        }
    }
}
