package cn.gbtmedia.gbt28181.server.media.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class MediaServerStopEvent extends ApplicationEvent {

    private final String ssrc;

    public MediaServerStopEvent(Object source, String ssrc) {
        super(source);
        this.ssrc = ssrc;
    }
}
