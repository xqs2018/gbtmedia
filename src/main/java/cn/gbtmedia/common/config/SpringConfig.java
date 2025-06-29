package cn.gbtmedia.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author xqs
 */
@Slf4j
@Configuration
@EnableJpaAuditing
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableSpringDataWebSupport(pageSerializationMode =
        EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SpringConfig{

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ExecutorService executorService = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name("vt-spring-executor-", 0)
                        .uncaughtExceptionHandler((t, e) -> log.error("spring executor pool ex t {}", t, e))
                        .factory());
        return new TaskExecutorAdapter(executorService);
    }

    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadFactory(
                Thread.ofVirtual()
                .name("vt-spring-scheduler-", 0)
                .uncaughtExceptionHandler((t, e) -> log.error("spring scheduler pool ex t {}", t, e))
                .factory());
        scheduler.initialize();
        return scheduler;
    }

}
