package cn.gbtmedia.jtt808.server.media;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.server.MediaServer;
import cn.gbtmedia.jtt808.server.media.server.netty.SinglePortMediaServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author xqs
 */
@Slf4j
@Component("jtt808MultiplePortMediaService")
@ConditionalOnProperty(prefix = "server-config.jtt808", name = "mediaModel", havingValue = "single")
public class SinglePortMediaService implements MediaService{

    @Override
    public MediaServer createServer(MediaParam mediaParam) {
        MediaServer mediaServer = new SinglePortMediaServer();
        mediaServer.setMediaParam(mediaParam);
        mediaServer.setMediaKey(mediaParam.getMediaKey());
        ServerConfig serverConfig = ServerConfig.getInstance();
        String serverIp = serverConfig.getPublicIp();
        int flvPort = serverConfig.getJtt808().getFlvPort();
        String httpFlv = String.format("http://%s:%s/video/%s.flv", serverIp, flvPort, mediaParam.getMediaKey());
        mediaServer.setHttFlv(httpFlv);
        // 直接启动返回端口
        boolean started = mediaServer.start();
        return started? mediaServer : null;
    }
}
