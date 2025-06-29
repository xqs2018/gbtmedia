package cn.gbtmedia.gbt28181.server.sip.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class DeviceEvent extends ApplicationEvent {

    private final String deviceId;

    /**
     * 0 离线 1 在线
     */
    private final Integer online;

    public DeviceEvent(Object source,String deviceId,Integer online) {
        super(source);
        this.deviceId = deviceId;
        this.online = online;
    }
}
