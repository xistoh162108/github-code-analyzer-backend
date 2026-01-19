package com.backend.githubanalyzer.domain.analysis.queue;

import com.backend.githubanalyzer.domain.analysis.dto.AnalysisJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    public static final String ANALYSIS_QUEUE_KEY = "github:analysis:queue";

    public void pushJob(AnalysisJobRequest job) {
        log.info("Pushing analysis job to queue for commit: {}", job.getCommitSha());
        redisTemplate.opsForList().rightPush(ANALYSIS_QUEUE_KEY, job);
    }
}
