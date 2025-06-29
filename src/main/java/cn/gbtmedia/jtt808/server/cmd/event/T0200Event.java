package cn.gbtmedia.jtt808.server.cmd.event;

import cn.gbtmedia.jtt808.server.cmd.message.T0200;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class T0200Event extends ApplicationEvent {

    private final String clientId;

    private final T0200 t0200;

    public T0200Event(Object source,String clientId,T0200 t0200) {
        super(source);
        this.clientId = clientId;
        this.t0200 = t0200;
    }
}
