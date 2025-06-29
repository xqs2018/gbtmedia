package cn.gbtmedia.jtt808.server.cmd.receive.impl;

import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Message;
import cn.gbtmedia.jtt808.server.cmd.receive.AbstractJtt808Receive;
import cn.gbtmedia.jtt808.server.cmd.session.ClientSession;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 终端心跳处理
 * @author xqs
 */
@Slf4j
@Component
public class T0002Receive extends AbstractJtt808Receive {

    @Resource
    private ClientRepository clientRepository;

    @Override
    public boolean support(Jtt808Message request) {
        return request.getMessageId() == 0x0002;
    }

    @Override
    public void doHandle(ClientSession session, Jtt808Message request) {
        String clientId = session.getClientId();
        int version = session.getVersion();
        Jtt808SessionManager.getInstance().putClientSession(session);

        log.debug("T0002Receive clientId {}",clientId);

        // 默认返回 平台通用应答 0x8001
        super.defaultResponse(session,request);

        Client client = clientRepository.findByClientId(clientId);
        if(client == null){
            log.error("T0002Receive client is null clientId {}",clientId);
            return;
        }
        //更新心跳
        client.setKeepaliveTime(new Date());
        client.setClientIp(session.getSocketAddress().getAddress().getHostAddress());
        client.setClientPort(session.getSocketAddress().getPort());
        client.setOnline(1);
        clientRepository.save(client);
    }

}
