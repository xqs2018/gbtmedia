package cn.gbtmedia.jtt808.server.cmd.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class ClientEvent extends ApplicationEvent {

    private final String clientId;

    /**
     * 0 离线 1 在线
     */
    private final Integer online;

    public ClientEvent(Object source,String clientId,Integer online) {
        super(source);
        this.clientId = clientId;
        this.online = online;
    }
}
