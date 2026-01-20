package com.backend.githubanalyzer.domain.sync.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommitSyncQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SYNC_QUEUE_KEY = "github:sync:commit_queue";

    public void pushJob(CommitSyncJobRequest job) {
        log.debug("Pushing commit sync job to Redis: {}/{} - {}", job.getOwner(), job.getRepoName(), job.getSha());
        redisTemplate.opsForList().rightPush(SYNC_QUEUE_KEY, job);
    }
}
