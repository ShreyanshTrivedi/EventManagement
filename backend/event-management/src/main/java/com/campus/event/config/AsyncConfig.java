package com.campus.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Configures the async executor used by {@link com.campus.event.service.NotificationService}.
 *
 * Thread pool settings:
 *   - corePoolSize  = 4  — always-live threads for normal load
 *   - maxPoolSize   = 16 — burst capacity (e.g. mass-cancel event)
 *   - queueCapacity = 200 — back-pressure buffer before rejection
 *
 * Named "notificationExecutor" so it is unambiguous and doesn't conflict with
 * Spring's default SimpleAsyncTaskExecutor.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
