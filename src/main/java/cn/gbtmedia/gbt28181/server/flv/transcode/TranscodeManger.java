package cn.gbtmedia.gbt28181.server.flv.transcode;

import cn.gbtmedia.gbt28181.server.flv.FlvSubscriber;
import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
public class TranscodeManger {

    private static final TranscodeManger TRANSCODE_MANGER = new TranscodeManger();

    public static TranscodeManger getInstance(){
        return TRANSCODE_MANGER;
    }

    private static final ThreadPoolExecutor GBT28181_TRANSCODE_POOL = new ThreadPoolExecutor(1,
            10,60, TimeUnit.SECONDS,new SynchronousQueue<>(),
            new NamedThreadFactory("gbt28181-transcode-", null, false,
                    (t, e) -> log.error("gbt28181 transcode pool ex t {} ",t,e)),
            (r, executor) -> log.error("gbt28181 transcode pool max"));

    private static final Map<String, TranscodeTask> TRANSCODE_TASK_MAP = new ConcurrentHashMap<>();

    public void subscribe(FlvSubscriber subscriber) {
        String key = subscriber.getSsrc() + "_" + subscriber.getTranscode();
        TranscodeTask transcodeTask = TRANSCODE_TASK_MAP.get(key);
        if(transcodeTask == null){
            log.info("create new transcodeTask ssrc {} transcode {}",subscriber.getSsrc(),subscriber.getTranscode());
            transcodeTask = new TranscodeTaskJavaCv();
            transcodeTask.setSsrc(subscriber.getSsrc());
            transcodeTask.setTranscode(subscriber.getTranscode());
            TRANSCODE_TASK_MAP.put(key, transcodeTask);
            GBT28181_TRANSCODE_POOL.execute(transcodeTask);
        }
        transcodeTask.addSubscriber(subscriber);
    }

    public void unSubscribe(FlvSubscriber subscriber) {
        String key = subscriber.getSsrc() + "_" + subscriber.getTranscode();
        TranscodeTask transcodeTask = TRANSCODE_TASK_MAP.get(key);
        if(transcodeTask != null){
            transcodeTask.removeSubscriber(subscriber);
            if(transcodeTask.getSubscribers().isEmpty()){
                log.info("unSubscribe remove transcodeTask ssrc {} transcode {}",subscriber.getSsrc(),subscriber.getTranscode());
                TRANSCODE_TASK_MAP.remove(key);
            }
        }
    }

}
