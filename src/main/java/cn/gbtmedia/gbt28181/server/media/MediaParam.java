package cn.gbtmedia.gbt28181.server.media;

import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import lombok.Data;

/**
 * @author xqs
 */
@Data
public class MediaParam {

    private String mediaTransport;

    private String mediaType;

    private String callId;

    private String ssrc;

    private String recordPath;

    private long recordSecond;

    private boolean recordSlice;

    private MediaServer mediaServer;
}
