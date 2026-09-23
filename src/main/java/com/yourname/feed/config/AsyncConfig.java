package com.yourname.feed.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool backing @Async fan-out-on-write calls (FanOutOnWriteService).
 * Sized via feed.async.* in application.yml.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${feed.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${feed.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${feed.async.queue-capacity:1000}")
    private int queueCapacity;

    @Bean(name = "feedTaskExecutor")
    public Executor feedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("feed-fanout-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return feedTaskExecutor();
    }
}
