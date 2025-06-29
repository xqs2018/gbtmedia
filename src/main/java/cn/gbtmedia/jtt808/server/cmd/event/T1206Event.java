package cn.gbtmedia.jtt808.server.cmd.event;

import cn.gbtmedia.jtt808.server.cmd.message.T1206;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class T1206Event extends ApplicationEvent {

     private final String clientId;

     private final T1206 t1206;

    public T1206Event(Object source,String clientId,T1206 t1206) {
        super(source);
        this.clientId = clientId;
        this.t1206 = t1206;
    }
}
