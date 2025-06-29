package cn.gbtmedia.gbt28181.server.sip.send;

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
                return get(ServerConfig.getInstance().getGbt28181().getSipTimeOut(), TimeUnit.MILLISECONDS);
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

    /**
     * 双key一个回调
     */
    private static final Cache<String, TempFuture> FUTURE_SN_MAP = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    static {
        SchedulerTask.getInstance().startPeriod("gbt28181FutureContext", ()->{FUTURE_MAP.cleanUp();FUTURE_SN_MAP.cleanUp();},100);
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
     * 注册回调
     */
    public static Future<Object> regist(String key, String snKey){
        TempFuture tempFuture = new TempFuture();
        if(FUTURE_MAP.asMap().containsKey(key)){
            throw new RuntimeException("regist key duplicate "+ key);
        }
        if(FUTURE_SN_MAP.asMap().containsKey(snKey)){
            throw new RuntimeException("regist sn duplicate "+ snKey);
        }
        FUTURE_MAP.put(key, tempFuture);
        FUTURE_SN_MAP.put(snKey, tempFuture);
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
        TempFuture snFuture  = FUTURE_SN_MAP.getIfPresent(key);
        if(snFuture != null){
            snFuture.setResult(data);
        }
        FUTURE_MAP.invalidate(key);
        FUTURE_SN_MAP.invalidate(key);
    }

    /**
     * 所有待回调的key
     */
    public static Set<String> allKeys(){
        return FUTURE_MAP.asMap().keySet();
    }

    /**
     * 包含key
     */
    public static boolean containsKey(String key){
        return FUTURE_MAP.asMap().containsKey(key) || (FUTURE_SN_MAP.asMap().containsKey(key));
    }
}
