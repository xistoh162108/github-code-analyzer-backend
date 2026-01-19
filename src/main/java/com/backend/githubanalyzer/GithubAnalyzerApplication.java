package com.backend.githubanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import io.github.cdimascio.dotenv.Dotenv;

@org.springframework.cache.annotation.EnableCaching
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class GithubAnalyzerApplication {

    public static void main(String[] args) {
        // Explicitly load .env
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            // Ignore
        }

        SpringApplication.run(GithubAnalyzerApplication.class, args);
    }

    @Bean(name = "analysisTaskExecutor")
    public org.springframework.core.task.TaskExecutor analysisTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // Increased from 10
        executor.setMaxPoolSize(50); // Increased from 20 to handle bursts
        executor.setQueueCapacity(3000); // Increased from 500 to support large repo syncs
        executor.setThreadNamePrefix("AnalysisWorker-");
        // Prevent data loss by running in caller thread if queue is full (throttles
        // Redis consumer)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
