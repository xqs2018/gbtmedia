package cn.gbtmedia.common.aop;

import cn.gbtmedia.common.util.SpringUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xqs
 */
@Slf4j
@Aspect
@Configuration
@EnableAspectJAutoProxy
public class SyncWaitAspect {

    @Pointcut("@annotation(cn.gbtmedia.common.aop.SyncWait)")
    public void syncWaitPointCut() {
    }

    private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    private final Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    @Around("syncWaitPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object target = point.getTarget();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?> returnType = signature.getReturnType();
        Method method = signature.getMethod();
        String className =  method.getDeclaringClass().getName();
        String methodName = method.getName();
        Object[] args = point.getArgs();
        SyncWait syncWait = method.getAnnotation(SyncWait.class);
        int maxWaitTime = syncWait.maxWaitTime();
        int maxWaitThread = syncWait.maxWaitThread();
        String key = syncWait.key();
        if(ObjectUtil.isNotEmpty(key)){
            key = className+ "_"+methodName+ "_" + SpringUtil.spelParse(key, method, args);
        }else {
            key = className+ "_"+ Arrays.stream(args).filter(Objects::nonNull).reduce(methodName, (a, b) -> a + "_" + b);
        }
        // 先检查多少排队的
        Semaphore semaphore = semaphoreMap.computeIfAbsent(key, k -> new Semaphore(maxWaitThread));
        if (!semaphore.tryAcquire()) {
            log.error("semaphore busy methodName {} key {}",methodName, key);
            throw new RuntimeException("semaphore busy " + methodName);
        }
        // 加锁执行
        Lock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock(true));
        boolean tryLock = lock.tryLock(maxWaitTime, TimeUnit.MILLISECONDS);
        try {
            if(!tryLock){
                log.error("lock busy methodName {} key {}",methodName, key);
                throw new RuntimeException("lock busy " + methodName);
            }
            return point.proceed();
        }finally {
            semaphore.release();
            if(semaphore.availablePermits() == maxWaitThread){
                lockMap.remove(key);
                semaphoreMap.remove(key);
            }
            if(tryLock){
                lock.unlock();
            }
        }
    }

}
