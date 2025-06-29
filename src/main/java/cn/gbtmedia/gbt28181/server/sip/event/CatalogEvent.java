package cn.gbtmedia.gbt28181.server.sip.event;

import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author xqs
 */
@Getter
public class CatalogEvent extends ApplicationEvent {

    private final String platformId;

    private final List<PlatformChannel> platformChannelList;

    private final String eventTye;

    public CatalogEvent( Object source,String platformId, List<PlatformChannel> platformChannelList, String eventTye) {
        super(source);
        this.platformId = platformId;
        this.platformChannelList = platformChannelList;
        this.eventTye = eventTye;
    }
}
