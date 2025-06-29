package cn.gbtmedia.gbt28181.server.media.server.netty;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xqs
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SinglePortMediaServer extends MediaServer {

    private static final Map<String,SinglePortMediaServer> TEMP = new ConcurrentHashMap<>();

    public static SinglePortMediaServer getBySsrc(String ssrc){
        return TEMP.get(ssrc);
    }

    private Channel channel;

    @Override
    protected void doStart() {
        mediaPort = ServerConfig.getInstance().getGbt28181().getMediaSinglePort();
        mediaIp = ServerConfig.getInstance().getAccessIp();
        TEMP.put(ssrc, this);
        log.info("media single port {}", mediaPort);
    }

    @Override
    protected void doStop() {
        TEMP.remove(ssrc, this);
        log.info("temp remove ssrc {}",ssrc);
    }

    @Override
    public void doSendRtpMessage(RtpMessage message) {
        if(channel == null){
            return;
        }
        channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("doSendRtpMessage error",future.cause());
            }
        });
    }
}
