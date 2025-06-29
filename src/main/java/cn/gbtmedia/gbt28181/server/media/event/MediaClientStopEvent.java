package cn.gbtmedia.gbt28181.server.media.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class MediaClientStopEvent extends ApplicationEvent {

    private final String callId;

    public MediaClientStopEvent(Object source,String callId) {
        super(source);
        this.callId = callId;
    }
}
