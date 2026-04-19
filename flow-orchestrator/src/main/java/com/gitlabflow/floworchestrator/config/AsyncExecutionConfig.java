package com.gitlabflow.floworchestrator.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncExecutionConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService asyncComposerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
