package cn.gbtmedia.gbt28181.server.flv;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xqs
 */
@Slf4j
@Data
public class FlvSubscriber {

    private Channel channel;

    private String ssrc;

    private boolean sendHeader;

    private long lastVideoTime;

    private long videoTime;

    private long lastAudioTime;

    private long audioTime;

    // 转码画质 origin 720p 360p
    private String transcode;

    // 1 http 2 ws
    private int subscriberType;

    public void sendData(ByteBuf data){
        if(!channel.isActive()){
            return;
        }
        if (subscriberType == 2) {
            channel.writeAndFlush(new BinaryWebSocketFrame(data.copy()))
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {log.error("send ws-flv error",future.cause());}});;
        } else {
            channel.writeAndFlush(new DefaultHttpContent(data.copy()))
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {log.error("send http-flv error",future.cause());}});;
        }
    }

    public void close() {
        channel.close();
    }
}
