package cn.gbtmedia.jtt808.server.alarm.receive.impl;

import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessage808;
import cn.gbtmedia.jtt808.server.alarm.receive.AbstractAlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 终端心跳处理
 * @author xqs
 */
@Slf4j
@Component("AlarmFileT0002Receive")
public class T0002Receive extends AbstractAlarmFileReceive {

    @Resource
    private ClientRepository clientRepository;

    @Override
    public boolean doSupport(AlarmFileMessage808 request) {
        return request.getMessageId() == 0x0002;
    }

    @Override
    public void doHandle(AlarmFileSession session, AlarmFileMessage808 request) {
        String clientId = session.getClientId();
        int version = session.getVersion();

        log.debug("T0002Receive clientId {}",clientId);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);
    }

}
