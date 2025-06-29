package cn.gbtmedia.jtt808.server.cmd.send;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.extra.SchedulerTask;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author xqs
 */
@Slf4j
public class FutureContext {

    public static class TempFuture implements Future<Object>{

        private final CountDownLatch latch = new CountDownLatch(1);

        private Object result;

        public void setResult(Object result){
            this.result = result;
            latch.countDown();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            try {
                return get(ServerConfig.getInstance().getJtt808().getCmdTimeOut(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            boolean await = latch.await(timeout, unit);
            return this.result;
        }
    }

    /**
     * 等待的回调
     */
    private static final Cache<String, TempFuture> FUTURE_MAP = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    static {
        SchedulerTask.getInstance().startPeriod("jtt808FutureContext", FUTURE_MAP::cleanUp,100);
    }

    /**
     * 注册回调
     */
    public static Future<Object> regist(String key){
        TempFuture tempFuture = new TempFuture();
        if(FUTURE_MAP.asMap().containsKey(key)){
            throw new RuntimeException("regist key duplicate "+ key);
        }
        FUTURE_MAP.put(key, tempFuture);
        return tempFuture;
    }

    /**
     * 触发回调
     */
    public static void callBack(String key, Object data){
        TempFuture future  = FUTURE_MAP.getIfPresent(key);
        if(future != null){
            future.setResult(data);
        }
        FUTURE_MAP.invalidate(key);
    }

    /**
     * 所有待回调的key
     */
    public static Set<String> allKeys(){
        return FUTURE_MAP.asMap().keySet();
    }
}
