package cn.gbtmedia.common.aop;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.common.util.SpringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

/**
 * @author xqs
 */
@Slf4j
@Aspect
@Configuration
@EnableAspectJAutoProxy
public class AsyncBatchAspect {

    @Pointcut("@annotation(cn.gbtmedia.common.aop.AsyncBatch)")
    public void asyncBatchPointCut() {
    }

    private final static Map<String, Task> TASK_MAP = new ConcurrentHashMap<>();

    private final static ThreadLocal<Boolean> SAVE_BATCH_CONTEXT = new ThreadLocal<>();

    @Around("asyncBatchPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object target = point.getTarget();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?> returnType = signature.getReturnType();
        Method method = signature.getMethod();
        String className =  method.getDeclaringClass().getName();
        String methodName = method.getName();
        Object[] args = point.getArgs();
        String key = className + "_" + methodName;
        if(!(args[0] instanceof List<?> elements)){
            log.error("asyncBatch {} elements is not list",key);
            return point.proceed();
        }
        Boolean isSave = SAVE_BATCH_CONTEXT.get();
        if(isSave != null && isSave){
            return point.proceed();
        }
        AsyncBatch asyncBatch = method.getAnnotation(AsyncBatch.class);
        int maxThread = asyncBatch.maxThread();
        int maxElements = asyncBatch.maxElements();
        int maxWaitTime = asyncBatch.maxWaitTime();
        Task task = TASK_MAP.computeIfAbsent(key, k -> {
           Task tk = new Task(key, target, method, maxElements);
            IntStream.range(1, maxThread + 1).forEach(n->{
                log.info("start asyncBatch task {}", key + "_" + n);
                SchedulerTask.getInstance().startPeriod(key + "_" + n,tk,maxWaitTime,true);
            });
           return tk;
        });
        task.getElementQueue().addAll(elements);
        return null;
    }

    @Data
    @AllArgsConstructor
    private static class Task implements Runnable{
        private String key;
        private Object target;
        private Method method;
        private int maxElements;
        private final Queue<Object> elementQueue = new ConcurrentLinkedQueue<>();
        @Override
        public void run() {
            try {
                long timeMillis = System.currentTimeMillis();
                List<Object> elements = new ArrayList<>();
                int size = elementQueue.size();
                for(int i = 0; i < size; i++){
                    Object element = elementQueue.poll();
                    if(element == null){
                        continue;
                    }
                    elements.add(element);
                    if(elements.size() >= maxElements){
                        break;
                    }
                }
                if(elements.isEmpty()){
                    return;
                }
                SAVE_BATCH_CONTEXT.set(true);
                ReflectionUtils.invokeMethod(method, target,elements);
                timeMillis = System.currentTimeMillis() - timeMillis;
                if(timeMillis > 1000){
                    log.warn("asyncBatch {} too slow cost {} ms saveSize {} queueSize {}",
                            key,timeMillis,elements.size(),elementQueue.size());
                }
            }catch (Exception ex){
                log.error("asyncBatch {} ex",key,ex);
            }finally {
                SAVE_BATCH_CONTEXT.remove();
            }
        }
    }
}
