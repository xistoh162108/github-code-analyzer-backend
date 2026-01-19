package com.backend.githubanalyzer.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DLQ_SYNC_KEY = "github:dlq:sync";
    private static final String DLQ_ANALYSIS_KEY = "github:dlq:analysis";

    public void pushToSyncDlq(Object job, String reason) {
        pushToDlq(DLQ_SYNC_KEY, job, reason);
    }

    public void pushToAnalysisDlq(Object job, String reason) {
        pushToDlq(DLQ_ANALYSIS_KEY, job, reason);
    }

    private void pushToDlq(String dlqKey, Object job, String reason) {
        log.error("Exhausted retries. Pushing job to DLQ [{}]: {}, Reason: {}", dlqKey, job, reason);

        Map<String, Object> dlqEntry = new HashMap<>();
        dlqEntry.put("job", job);
        dlqEntry.put("reason", reason);
        dlqEntry.put("failedAt", LocalDateTime.now().toString());

        try {
            redisTemplate.opsForList().rightPush(dlqKey, dlqEntry);
        } catch (Exception e) {
            log.error("Failed to push to DLQ! Emergency log: {}", dlqEntry, e);
        }
    }
}
