package cn.gbtmedia.gbt28181.server.media;

import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;

/**
 * @author xqs
 */
public interface MediaService {

    /**
     * 流媒体服务端
     */
    MediaServer createServer(MediaParam mediaParam);

    /**
     * 流媒体客户端
     */
    MediaClient createClient(MediaParam mediaParam);
}
