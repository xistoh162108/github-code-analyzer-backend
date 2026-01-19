package com.backend.githubanalyzer.domain.sync.worker;

import com.backend.githubanalyzer.domain.sync.dto.SyncJobRequest;
import com.backend.githubanalyzer.domain.sync.service.GithubPersistenceService;
import com.backend.githubanalyzer.domain.sync.service.GithubSyncService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.infra.github.service.GithubAppService;
import com.backend.githubanalyzer.infra.redis.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncQueueWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GithubSyncService githubSyncService;
    private final GithubAppService githubAppService;
    private final UserService userService;
    private final GithubPersistenceService githubPersistenceService;
    private final DeadLetterQueueService deadLetterQueueService;
    private static final String SYNC_QUEUE_KEY = "github:sync:queue";

    @Scheduled(fixedDelay = 1000) // Poll every second
    public void processJobs() {
        SyncJobRequest job = (SyncJobRequest) redisTemplate.opsForList().leftPop(SYNC_QUEUE_KEY, Duration.ofSeconds(1));

        if (job == null)
            return;

        log.info("Processing sync job: {} for installation: {}", job.getType(), job.getInstallationId());

        try {
            String token = githubAppService.getInstallationToken(job.getInstallationId());

            switch (job.getType()) {
                case PUSH:
                    githubSyncService.syncSelective(job.getRepositoryId(), job.getBranchName(), token);
                    break;
                case REPO_SYNC:
                case REPOSITORY_CREATED:
                    // For now, reuse syncSelective or implement a per-repo sync if needed
                    githubSyncService.syncSelective(job.getRepositoryId(), null, token);
                    break;
                case INSTALLATION:
                    handleInstallationJob(job);
                    break;
                case REPOSITORY_DELETED:
                    githubPersistenceService.deleteRepository(job.getRepositoryId());
                    break;
                case UNINSTALLATION:
                    handleUninstallation(job);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to process sync job (attempt {}): {}", job.getRetryCount() + 1, job, e);
            if (job.getRetryCount() < 3) {
                job.setRetryCount(job.getRetryCount() + 1);
                log.info("Retrying job: {}", job);
                redisTemplate.opsForList().rightPush(SYNC_QUEUE_KEY, job);
            } else {
                deadLetterQueueService.pushToSyncDlq(job, e.getMessage());
            }
        }
    }

    private void handleInstallationJob(SyncJobRequest job) {
        log.info("New installation detected: {}. Monitoring for future events.", job.getInstallationId());
        if (job.getGithubLogin() != null) {
            try {
                User user = userService.findByUsername(job.getGithubLogin()).orElse(null);
                if (user != null) {
                    user.setInstallationId(job.getInstallationId());
                    userService.save(user);
                    log.info("Associated installation ID {} with user {}", job.getInstallationId(),
                            user.getUsername());
                }
            } catch (Exception e) {
                log.warn("Could not find user to associate with installation: {}", job.getGithubLogin());
            }
        }
    }

    private void handleUninstallation(SyncJobRequest job) {
        log.info("Installation {} removed. Stopping synchronization.", job.getInstallationId());
        // Potential logic to cleanup or mark associated repositories/users
    }
}
