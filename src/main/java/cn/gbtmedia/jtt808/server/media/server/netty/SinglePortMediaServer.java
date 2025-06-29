package cn.gbtmedia.jtt808.server.media.server.netty;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import cn.gbtmedia.jtt808.server.media.server.MediaServer;
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
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class SinglePortMediaServer extends MediaServer {

    private static final Map<String, SinglePortMediaServer> TEMP = new ConcurrentHashMap<>();

    public static SinglePortMediaServer getByMediaKey(String mediaKey){
        SinglePortMediaServer mediaServer = TEMP.get(mediaKey);
        // 兼容jt808 2019版本
        if(mediaServer == null){
            String[] split = mediaKey.split("_");
            split[1]  = "00000000" + split[1];
            mediaKey = String.join("_", split);
            mediaServer = TEMP.get(mediaKey);
        }
        return mediaServer;
    }

    private Channel channel;

    @Override
    protected void doStart() throws Exception {
        ServerConfig.Jtt808 conf = ServerConfig.getInstance().getJtt808();
        mediaIp = ServerConfig.getInstance().getAccessIp();
        mediaPort = mediaKey.contains("play")? conf.getMediaSinglePlayPort() : conf.getMediaSinglePlaybackPort();
        TEMP.put(mediaKey,this);
        log.info("media single port {}", mediaPort);
    }

    @Override
    protected void doStop() {
        TEMP.remove(mediaKey, this);
        log.info("temp remove mediaKey {}",mediaKey);
    }

    @Override
    public void doSend1078RtpMessage(Jtt1078RtpMessage message) {
        if(channel == null){
            return;
        }
        channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("doSend1078RtpMessage error",future.cause());
            }
        });
    }
}
