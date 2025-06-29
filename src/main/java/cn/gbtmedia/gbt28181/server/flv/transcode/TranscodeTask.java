package cn.gbtmedia.gbt28181.server.flv.transcode;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.flv.FlvSubscriber;
import cn.hutool.core.codec.Base64;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author xqs
 */
@Slf4j
@Data
public abstract class TranscodeTask implements Runnable{

    protected volatile boolean isStop;

    protected String ssrc;

    // 转码画质 origin 720p 360p
    protected String transcode;

    protected String pullFlvUrl;

    protected int imageWidth;

    protected int imageHeight;

    protected String drawText;

    protected final List<FlvSubscriber> subscribers = new CopyOnWriteArrayList<>();

    public void addSubscriber(FlvSubscriber subscriber){
        subscribers.add(subscriber);
    }

    public void removeSubscriber(FlvSubscriber subscriber){
        subscribers.removeIf(s -> subscriber.getChannel().id().asLongText()
                .equals(s.getChannel().id().asLongText()));
        if(subscribers.isEmpty()){
            stop();
        }
    }

    protected ByteArrayOutputStream flvData = new ByteArrayOutputStream();

    protected byte[] flvHeader;

    public void stop(){
        if(isStop){
            return;
        }
        isStop = true;
        log.info("stop transcodeTask ssrc {} transcode {}",ssrc,transcode);
        subscribers.clear();
        doStop();
    }

    protected abstract void doStop();


    protected abstract void doStart() throws Exception;

    @Override
    public void run() {
        try {
            pullFlvUrl = Base64.decodeStr(ssrc);
            if(!pullFlvUrl.startsWith("http")){
                ServerConfig serverConfig = ServerConfig.getInstance();
                int flvPort = serverConfig.getGbt28181().getFlvPort();
                pullFlvUrl = String.format("http://127.0.0.1:%s/video/%s.flv",flvPort, ssrc);
            }
            if("720p".equals(transcode)){
                imageWidth = 1280;
                imageHeight = 720;
            }
            if("360p".equals(transcode)){
                imageWidth = 480;
                imageHeight = 360;
            }
            drawText = "水印文本-1116";
            doStart();
        }catch (Exception e){
          log.error("transcodeTask ex",e);
        } finally {
            stop();
        }
    }

    // 发送数据给每个播放者
    protected void receiveFlvData(byte[] data){
        for(FlvSubscriber subscriber : subscribers){
            // 先发送flvHeader
            if(!subscriber.isSendHeader()){
                subscriber.sendData(Unpooled.copiedBuffer(flvHeader));
                subscriber.setSendHeader(true);
            }
            subscriber.sendData(Unpooled.copiedBuffer(data));
        }
    }

}
