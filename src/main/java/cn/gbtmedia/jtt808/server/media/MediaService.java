package cn.gbtmedia.jtt808.server.media;

import cn.gbtmedia.jtt808.server.media.server.MediaServer;

/**
 * @author xqs
 */
public interface MediaService {

    /**
     * 流媒体服务端
     */
    MediaServer createServer(MediaParam mediaParam);
}
