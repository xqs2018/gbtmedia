package cn.gbtmedia.gbt28181.server.media.stream;

import cn.gbtmedia.gbt28181.server.flv.FlvSubscriber;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
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

    private static final ExecutorService GBT28181_STREAM_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-gbt28181-stream-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("gbt28181 stream pool ex t {}", t, e))
                            .factory());

    private static final Map<String, StreamTask> STREAM_TASK_MAP = new ConcurrentHashMap<>();

    public StreamTask getStreamTask(String ssrc){
        return STREAM_TASK_MAP.computeIfAbsent(ssrc, k->{
            log.info("create new streamTask ssrc {}", ssrc);
            StreamTask streamTask = new StreamTask(ssrc);
            GBT28181_STREAM_POOL.execute(streamTask);
            return streamTask;
        });
    }

    public void publish(String ssrc, RtpMessage message) {
        StreamTask streamTask = getStreamTask(ssrc);
        streamTask.pushRtpMessage(message);
    }

    public void unPublish(String ssrc) {
        StreamTask streamTask = STREAM_TASK_MAP.get(ssrc);
        if(streamTask != null){
            boolean stop = streamTask.stop(1);
            if(stop){
                log.info("unPublish remove streamTask ssrc {}",ssrc);
                STREAM_TASK_MAP.remove(ssrc);
            }
        }
    }

    public void subscribe(FlvSubscriber subscriber) {
        StreamTask streamTask = getStreamTask(subscriber.getSsrc());
        streamTask.addSubscriber(subscriber);
    }

    public void unSubscribe(FlvSubscriber subscriber) {
        StreamTask streamTask = STREAM_TASK_MAP.get(subscriber.getSsrc());
        if(streamTask != null){
            streamTask.removeSubscriber(subscriber);
            boolean stop = streamTask.stop(2);
            if(stop){
                log.info("unSubscribe remove streamTask ssrc {}",subscriber.getSsrc());
                STREAM_TASK_MAP.remove(streamTask.getSsrc());
            }
        }
    }

}
