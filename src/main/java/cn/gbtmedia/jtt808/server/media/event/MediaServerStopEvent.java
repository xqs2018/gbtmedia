package cn.gbtmedia.jtt808.server.media.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class MediaServerStopEvent extends ApplicationEvent {

    private final String mediaKey;

    public MediaServerStopEvent(Object source, String mediaKey) {
        super(source);
        this.mediaKey = mediaKey;
    }
}
