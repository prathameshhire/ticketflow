package com.ticketflow.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "alertTaskExecutor")
    Executor alertTaskExecutor(
            @Value("${ticketflow.alerts.async.core-pool-size:2}") int corePoolSize,
            @Value("${ticketflow.alerts.async.max-pool-size:2}") int maxPoolSize,
            @Value("${ticketflow.alerts.async.queue-capacity:50}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ticketflow-alert-");
        executor.initialize();
        return executor;
    }
}

