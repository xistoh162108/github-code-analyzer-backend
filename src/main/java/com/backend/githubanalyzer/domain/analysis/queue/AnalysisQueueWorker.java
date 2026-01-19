package com.backend.githubanalyzer.domain.analysis.queue;

import com.backend.githubanalyzer.domain.analysis.dto.AnalysisJobRequest;
import com.backend.githubanalyzer.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisQueueWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AnalysisService analysisService;
    private final com.backend.githubanalyzer.global.monitor.MetricsService metricsService;
    private static final String ANALYSIS_QUEUE_KEY = "github:analysis:queue";

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("analysisTaskExecutor")
    private org.springframework.core.task.TaskExecutor taskExecutor;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("AnalysisQueueWorker initialized. Polling Redis queue: {}", ANALYSIS_QUEUE_KEY);
        // Register Queue Size Gauge
        metricsService.registerQueueSize("analysis_queue", () -> {
            Long size = redisTemplate.opsForList().size(ANALYSIS_QUEUE_KEY);
            return size != null ? size : 0;
        });
    }

    @Scheduled(fixedDelay = 2000) // Poll every 2 seconds to utilize OpenAI limit safely (approx 30 RPM)
    public void processAnalysisJobs() {
        // Process 1 job per tick to strictly control Rate Limit
        for (int i = 0; i < 1; i++) {
            try {
                // Non-blocking pop to drain queue faster
                Object popped = redisTemplate.opsForList().leftPop(ANALYSIS_QUEUE_KEY);

                if (popped == null) {
                    return; // Queue empty, wait for next schedule
                }

                // Submit to thread pool for processing
                taskExecutor.execute(() -> processJob(popped));

            } catch (Exception e) {
                log.error("Error in analysis queue worker loop: {}", e.getMessage(), e);
            }
        }
    }

    private void processJob(Object popped) {
        metricsService.recordJobDuration("analysis_job", () -> {
            try {
                AnalysisJobRequest job;
                if (popped instanceof AnalysisJobRequest) {
                    job = (AnalysisJobRequest) popped;
                } else {
                    // Try to convert if it's a LinkedHashMap (common with
                    // GenericJackson2JsonRedisSerializer)
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        job = mapper.convertValue(popped, AnalysisJobRequest.class);
                    } catch (Exception e) {
                        log.error("Invalid job type in queue: {}. Could not convert to AnalysisJobRequest.",
                                popped.getClass().getName());
                        return;
                    }
                }

                if (job == null || job.getCommitSha() == null) {
                    log.error("Job or CommitSHA is null after conversion. Raw popped: {}", popped);
                    return;
                }
                log.info("Processing AI analysis job (Async) for commit: {}, repo: {}", job.getCommitSha(),
                        job.getRepositoryId());

                try {
                    analysisService.analyzeCommitSync(job.getCommitSha(), job.getRepositoryId());
                } catch (Exception e) {
                    log.error("Failed to analyze commit: {}", job.getCommitSha(), e);
                }
            } catch (Exception e) {
                log.error("Error processing async analysis job", e);
            }
        });
    }
}
