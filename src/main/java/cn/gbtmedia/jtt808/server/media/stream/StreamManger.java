package cn.gbtmedia.jtt808.server.media.stream;

import cn.gbtmedia.jtt808.server.flv.FlvSubscriber;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author xqs
 */
@Slf4j
public class StreamManger {

    private static final StreamManger STREAM_MANGER = new StreamManger();

    public static StreamManger getInstance(){
        return STREAM_MANGER;
    }

    private static final ExecutorService JTT808_STREAM_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-jtt808-stream-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("jtt808 stream pool ex t {}", t, e))
                            .factory());

    private static final Map<String, StreamTask> STREAM_TASK_MAP = new ConcurrentHashMap<>();

    public StreamTask getStreamTask(String mediaKey){
        return STREAM_TASK_MAP.computeIfAbsent(mediaKey, k->{
            log.info("create new mediaKey mediaKey {}", mediaKey);
            StreamTask streamTask = new StreamTask(mediaKey);
            JTT808_STREAM_POOL.execute(streamTask);
            return streamTask;
        });
    }

    public void publish(String ssrc, Jtt1078RtpMessage message) {
        StreamTask streamTask = getStreamTask(ssrc);
        streamTask.pushJtt1078RtpMessage(message);
    }

    public void unPublish(String mediaKey) {
        StreamTask streamTask = STREAM_TASK_MAP.get(mediaKey);
        if(streamTask != null){
            boolean stop = streamTask.stop(1);
            if(stop){
                log.info("unPublish remove streamTask mediaKey {}",mediaKey);
                STREAM_TASK_MAP.remove(mediaKey);
            }
        }
    }

    public void subscribe(FlvSubscriber subscriber) {
        StreamTask streamTask = getStreamTask(subscriber.getMediaKey());
        streamTask.addSubscriber(subscriber);
    }

    public void unSubscribe(FlvSubscriber subscriber) {
        StreamTask streamTask = STREAM_TASK_MAP.get(subscriber.getMediaKey());
        if(streamTask != null){
            streamTask.removeSubscriber(subscriber);
            boolean stop = streamTask.stop(2);
            if(stop){
                log.info("unSubscribe remove streamTask mediaKey {}",subscriber.getMediaKey());
                STREAM_TASK_MAP.remove(streamTask.getMediaKey());
            }
        }
    }

}
