package com.hydra.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorServiceConfig {

    // 自定义线程工厂类
    static class BizThreadFactory implements ThreadFactory {
        private int threadCount = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("Biz-Task-" + threadCount++);
            return thread;
        }
    }

    @Bean
    public ExecutorService executorService() {
       //  int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;  // 适用于 I/O 密集型任务
        int corePoolSize = Runtime.getRuntime().availableProcessors(); //我们有2个prod服务 用一倍
        int maxPoolSize = corePoolSize * 4;  // 允许更多并发
        long keepAliveTime = 60L;  // 空闲线程保持 60 秒
        ExecutorService executorService = new ThreadPoolExecutor(
                corePoolSize,   // 核心线程数
                maxPoolSize,    // 最大线程数
                keepAliveTime,  // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(100), // 队列
                new BizThreadFactory(), // 自定义线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
                //ThreadPoolExecutor.CallerRunsPolicy 拒绝策略确保任务不会丢失，而是将任务交由提交任务的线程执行。这适用于需要“尽量”执行任务的场景
        );
        return executorService;
    }
}