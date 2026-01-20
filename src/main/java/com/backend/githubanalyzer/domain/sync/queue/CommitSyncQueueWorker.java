package com.backend.githubanalyzer.domain.sync.queue;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.sync.service.GithubPersistenceService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.infra.github.GithubApiService;
import com.backend.githubanalyzer.infra.github.dto.GithubCommitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommitSyncQueueWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GithubApiService githubApiService;
    private final GithubPersistenceService githubPersistenceService;
    private final UserService userService;

    private static final String SYNC_QUEUE_KEY = "github:sync:commit_queue";

    @Scheduled(fixedDelay = 900) // ~4000 jobs per hour
    public void processSyncJobs() {
        try {
            Object popped = redisTemplate.opsForList().leftPop(SYNC_QUEUE_KEY);
            if (popped == null)
                return;

            CommitSyncJobRequest job = convertToRequest(popped);
            if (job == null)
                return;

            processJob(job);
        } catch (Exception e) {
            log.error("Error in commit sync queue worker loop", e);
        }
    }

    private void processJob(CommitSyncJobRequest job) {
        try {
            User user = userService.findById(job.getUserId());
            if (user == null) {
                log.error("User not found for sync job: {}", job.getUserId());
                return;
            }

            GithubRepository repository = githubPersistenceService.findById(job.getRepositoryId());
            if (repository == null) {
                log.error("Repository not found: {}", job.getRepositoryId());
                return;
            }

            String token = job.getAccessToken();
            if (token == null) {
                log.warn("No token provided for commit sync job: {}", job.getSha());
                return;
            }

            log.info("Executing queued sync job for commit: {} in repo: {}", job.getSha(), job.getRepoName());
            GithubCommitResponse detailedDto = githubApiService.fetchCommitDetail(
                    job.getOwner(), job.getRepoName(), job.getSha(), token).block();

            if (detailedDto != null) {
                githubPersistenceService.saveCommit(repository, user, job.getBranchName(), detailedDto);
            }
        } catch (Exception e) {
            log.error("Failed to execute queued sync job for commit {}", job.getSha(), e);
        }
    }

    private CommitSyncJobRequest convertToRequest(Object popped) {
        if (popped instanceof CommitSyncJobRequest) {
            return (CommitSyncJobRequest) popped;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.convertValue(popped, CommitSyncJobRequest.class);
        } catch (Exception e) {
            log.error("Could not convert popped object to CommitSyncJobRequest", e);
            return null;
        }
    }
}
