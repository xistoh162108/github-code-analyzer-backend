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
    private static final String ANALYSIS_QUEUE_KEY = "github:analysis:queue";

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("analysisTaskExecutor")
    private org.springframework.core.task.TaskExecutor taskExecutor;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("AnalysisQueueWorker initialized. Polling Redis queue: {}", ANALYSIS_QUEUE_KEY);
    }

    @Scheduled(fixedDelay = 50) // Poll every 50ms for faster processing
    public void processAnalysisJobs() {
        // Process up to 10 jobs per tick (approx 200 jobs/sec throughput capacity)
        for (int i = 0; i < 10; i++) {
            try {
                // Non-blocking pop to drain queue faster
                Object popped = redisTemplate.opsForList().leftPop(ANALYSIS_QUEUE_KEY);

                if (popped == null) {
                    return; // Queue empty, wait for next schedule
                }

                // Submit to thread pool for parallel processing
                taskExecutor.execute(() -> processJob(popped));

            } catch (Exception e) {
                log.error("Error in analysis queue worker loop: {}", e.getMessage(), e);
            }
        }
    }

    private void processJob(Object popped) {
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
    }
}
