package com.goldmediatech.videometadata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Configuration for asynchronous processing using Java 21 virtual threads.
 * 
 * This configuration sets up virtual thread executors for background video import processing.
 * Virtual threads provide better resource utilization and higher concurrency for I/O-bound operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Virtual thread executor for video import operations.
     * 
     * Virtual threads are ideal for I/O-bound operations like external API calls
     * as they can handle thousands of concurrent operations with minimal resource usage.
     * 
     * @return Executor using virtual threads for video import tasks
     */
    @Bean("videoImportExecutor")
    public Executor videoImportExecutor() {
        // Use virtual threads with custom naming
        ThreadFactory threadFactory = Thread.ofVirtual()
            .name("video-import-", 0)
            .factory();
        return task -> {
            Thread thread = threadFactory.newThread(task);
            thread.start();
        };
    }

    /**
     * Virtual thread executor for reactive processing operations.
     * 
     * Virtual threads work well with reactive programming patterns and can handle
     * high concurrency for background processing tasks.
     * 
     * @return Executor using virtual threads for reactive processing tasks
     */
    @Bean("reactiveProcessorExecutor")
    public Executor reactiveProcessorExecutor() {
        // Use virtual threads with custom naming
        ThreadFactory threadFactory = Thread.ofVirtual()
            .name("reactive-processor-", 0)
            .factory();
        return task -> {
            Thread thread = threadFactory.newThread(task);
            thread.start();
        };
    }
} 