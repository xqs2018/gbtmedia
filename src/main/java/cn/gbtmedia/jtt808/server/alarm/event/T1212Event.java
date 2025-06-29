package cn.gbtmedia.jtt808.server.alarm.event;

import cn.gbtmedia.jtt808.server.alarm.message.T1211;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class T1212Event extends ApplicationEvent {

    private final String platformAlarmId;

    private final T1211 t1211;

    public T1212Event(Object source,String platformAlarmId,T1211 t1211) {
        super(source);
        this.platformAlarmId = platformAlarmId;
        this.t1211 = t1211;
    }
}
