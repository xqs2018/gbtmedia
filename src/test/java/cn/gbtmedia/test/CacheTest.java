package cn.gbtmedia.test;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.hutool.core.util.IdUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author xqs
 */
@Slf4j
public class CacheTest {

    public static void main(String[] args) throws InterruptedException {

         Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .removalListener((key, value, cause) -> {
                    log.warn("removalListener key {} value {} cause {}",key,value,cause);
                })
                .build();

            // 主动清理
            SchedulerTask.getInstance().startPeriod("cache", cache::cleanUp,100);

        SchedulerTask.getInstance().startDelay("test",()->{
            log.info("test startDelay..");
        },1000);

        new Thread(()->{
            String a = cache.get("a", new Function<String, String>() {
                @Override
                public String apply(String string) {
                    String id =  IdUtil.fastSimpleUUID();
                    log.info("put id1 {}",id);
                    return id;
                }
            });
        }).start();

        new Thread(()->{
            String a = cache.get("a", new Function<String, String>() {
                @Override
                public String apply(String string) {
                    String id =  IdUtil.fastSimpleUUID();
                    log.info("put id2 {}",id);
                    return id;
                }
            });
        }).start();

        new Thread(()->{
            String a = cache.get("a", new Function<String, String>() {
                @Override
                public String apply(String string) {
                    String id =  IdUtil.fastSimpleUUID();
                    log.info("put id3 {}",id);
                    return id;
                }
            });
        }).start();

        Thread.sleep(1000);
        //cache.invalidate("a");
        Thread.sleep(Integer.MAX_VALUE);
    }
}
