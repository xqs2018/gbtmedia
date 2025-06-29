package cn.gbtmedia.common.extra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
public class SchedulerTask {

    private static final SchedulerTask SCHEDULER_TASK = new SchedulerTask();

    public static SchedulerTask getInstance(){
        return SCHEDULER_TASK;
    }

    private static final ThreadPoolTaskScheduler SCHEDULER = new ThreadPoolTaskScheduler();

    static {
        SCHEDULER.setThreadFactory(
                Thread.ofVirtual()
                        .name("vt-scheduler-task-", 0)
                        .uncaughtExceptionHandler((t, e) -> log.error("scheduler task pool ex t {}", t, e))
                        .factory());
        SCHEDULER.initialize();
    }

    private static final Map<String, ScheduledFuture<?>> FUTURE_MAP = new ConcurrentHashMap<>();

    /**
     *  周期执行任务
     * @param key 任务key
     * @param task /
     * @param period 周期毫秒值
     */
    public void startPeriod(String key, Runnable task, long period){
        startPeriod(key,task,period,false);
    }

    /**
     *  周期执行任务
     * @param key 任务key
     * @param task /
     * @param period 周期毫秒值
     * @param fixedDelay 等待上次执行完成再执行
     */
    public void startPeriod(String key, Runnable task, long period,boolean fixedDelay){
        ScheduledFuture<?> scheduledFuture = FUTURE_MAP.get(key);
        if(scheduledFuture != null){
            log.warn("startPeriod 存在相同的任务先停止 key {} ",key);
            stop(key);
        }
        Runnable wrappedTask = () -> {
            try {
                task.run();
            } catch (Exception ex) {
                log.error("startPeriod error key {}",key,ex);
            }
        };
        Instant instant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(period));
        if (fixedDelay) {
            scheduledFuture = SCHEDULER.scheduleWithFixedDelay(wrappedTask,instant,Duration.ofMillis(period)
            );
        } else {
            scheduledFuture = SCHEDULER.scheduleAtFixedRate(wrappedTask,instant,Duration.ofMillis(period));
        }
        FUTURE_MAP.put(key,scheduledFuture);
    }


    /**
     *  延迟执行任务
     * @param key 任务key
     * @param task /
     * @param delay 延迟毫秒值
     */
    public void startDelay(String key, Runnable task, long delay){
        ScheduledFuture<?> scheduledFuture = FUTURE_MAP.get(key);
        if(scheduledFuture != null){
            log.warn("startDelay 存在相同的任务先停止 key {} ",key);
            stop(key);
        }
        Runnable wrappedTask = () -> {
            try {
                task.run();
            }catch (Exception ex){
                log.error("startDelay error key {}",key,ex);
            } finally {
                FUTURE_MAP.remove(key);
            }
        };
        Instant instant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(delay));
        scheduledFuture = SCHEDULER.schedule(wrappedTask,instant);
        FUTURE_MAP.put(key,scheduledFuture);
    }

    /**
     * 停止执行任务
     * @param key 任务key
     */
    public void stop(String key) {
        ScheduledFuture<?> scheduledFuture = FUTURE_MAP.get(key);
        if(scheduledFuture != null){
            scheduledFuture.cancel(true);
        }
        FUTURE_MAP.remove(key);
    }

}
