package cn.gbtmedia.jtt808.server.cmd.session;

import cn.gbtmedia.jtt808.server.media.server.MediaServer;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xqs
 */
@Data
public class ClientMedia {

    private long createTime = System.currentTimeMillis();

    /**
     * 播放类型 play 实时播放  playback 回放
     */
    private String mediaType;

    /**
     * 终端手机号
     */
    private String clientId;

    /**
     * 通道号
     */
    private Integer channelNo;

    /**
     * 播放地址
     */
    private String httpFlv;

    /**
     * 自增的序列号
     */
    private AtomicInteger sequenceNumber = new AtomicInteger(1);


    private String mediaKey;

    private MediaServer mediaServer;
}
