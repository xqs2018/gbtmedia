package cn.gbtmedia.gbt28181.server.media.record;


import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author xqs
 */
@Slf4j
public class RecordManger {

    private static final RecordManger RECORD_MANGER = new RecordManger();

    public static RecordManger getInstance(){
        return RECORD_MANGER;
    }

    private static final ExecutorService GBT28181_RECORD_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-gbt28181-record-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("gbt28181 record pool ex t {}", t, e))
                            .factory());
    public void run(Runnable task){
        GBT28181_RECORD_POOL.execute(task);
    }

    public RecordTask createTask(RecordParam param) {
        RecordTask task = new RecordTaskFFmpeg();
        task.setRecordParam(param);
        return task;
    }
}
