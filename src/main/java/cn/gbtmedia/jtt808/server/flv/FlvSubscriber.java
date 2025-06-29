package cn.gbtmedia.jtt808.server.flv;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author xqs
 */
@Data
public class FlvSubscriber {

    private Channel channel;

    private String mediaKey;

    private boolean sendHeader;

    private long lastVideoTime;

    private long videoTime;

    private long lastAudioTime;

    private long audioTime;
}
