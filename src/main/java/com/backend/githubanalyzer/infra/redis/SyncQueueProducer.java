package com.backend.githubanalyzer.infra.redis;

import com.backend.githubanalyzer.domain.sync.dto.SyncJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SYNC_QUEUE_KEY = "github:sync:queue";

    public void pushJob(SyncJobRequest job) {
        log.info("Pushing sync job to queue: {} for installation: {}", job.getType(), job.getInstallationId());
        redisTemplate.opsForList().rightPush(SYNC_QUEUE_KEY, job);
    }
}
