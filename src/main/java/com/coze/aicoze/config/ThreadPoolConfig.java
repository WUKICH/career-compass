package com.coze.aicoze.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    private static final int CORE_POOL_SIZE = 8;
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 100;
    private static final int KEEP_ALIVE_TIME = 2;

    @Bean
    public Executor assessmentExecutor(){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return executor;
    }

}
