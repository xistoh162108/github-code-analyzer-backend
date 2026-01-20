package com.backend.githubanalyzer.global.monitor;

import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.sprint.repository.SprintRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRepository;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SprintRepository sprintRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final CommitRepository commitRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    // Gauges (AtomicLongs to hold value)
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong ghostUsers = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong totalTeams = new AtomicLong(0);
    private final AtomicLong totalSprints = new AtomicLong(0);

    // Analysis Progress Gauges
    private final AtomicLong totalRepos = new AtomicLong(0);
    private final AtomicLong analyzedRepos = new AtomicLong(0);
    private final AtomicReference<Double> repoAnalysisPercent = new AtomicReference<>(0.0);

    private final AtomicLong totalCommits = new AtomicLong(0);
    private final AtomicLong analyzedCommits = new AtomicLong(0);
    private final AtomicReference<Double> commitAnalysisPercent = new AtomicReference<>(0.0);

    // Queue Sizes
    private final AtomicLong syncQueueSize = new AtomicLong(0);
    private final AtomicLong analysisQueueSize = new AtomicLong(0);

    // GitHub Rate Limits
    private final AtomicLong githubRateLimit = new AtomicLong(0);
    private final AtomicLong githubRateRemaining = new AtomicLong(0);

    // Initializer to register gauges
    @jakarta.annotation.PostConstruct
    public void init() {
        meterRegistry.gauge("business.users", totalUsers);
        meterRegistry.gauge("business.users.ghost", ghostUsers);
        meterRegistry.gauge("business.users.active", activeUsers);
        meterRegistry.gauge("business.teams", totalTeams);
        meterRegistry.gauge("business.sprints", totalSprints);

        meterRegistry.gauge("analysis.repos", totalRepos);
        meterRegistry.gauge("analysis.repos.analyzed", analyzedRepos);
        meterRegistry.gauge("analysis.repos.percentage", repoAnalysisPercent, AtomicReference::get);

        meterRegistry.gauge("analysis.commits", totalCommits);
        meterRegistry.gauge("analysis.commits.completed", analyzedCommits);
        meterRegistry.gauge("analysis.commits.percentage", commitAnalysisPercent, AtomicReference::get);

        // Queue Sizes with simple tags
        meterRegistry.gauge("queue.size", Tags.of("queue", "sync"), syncQueueSize);
        meterRegistry.gauge("queue.size", Tags.of("queue", "analysis"), analysisQueueSize);

        // GitHub Rate Limits
        meterRegistry.gauge("github.rate.limit", githubRateLimit);
        meterRegistry.gauge("github.rate.remaining", githubRateRemaining);
    }

    // Custom Counter for External APIs
    public void incrementExternalRequest(String target) {
        meterRegistry.counter("external.api.requests", "target", target).increment();
    }

    public void updateGithubRateLimits(long limit, long remaining) {
        githubRateLimit.set(limit);
        githubRateRemaining.set(remaining);
    }

    // Helper to record Job duration
    public void recordJobDuration(String jobName, Runnable task) {
        meterRegistry.timer("job.execution.time", "job", jobName).record(task);
    }

    // Scheduled task to update business metrics
    @Scheduled(fixedRate = 60000) // Update every minute
    public void updateBusinessMetrics() {
        try {
            long total = userRepository.count();
            long ghosts = userRepository.countByIsGhostTrue();

            totalUsers.set(total);
            ghostUsers.set(ghosts);
            activeUsers.set(total - ghosts);

            totalTeams.set(teamRepository.count());
            totalSprints.set(sprintRepository.count());

            // Repo Stats
            long repoCount = githubRepositoryRepository.count();
            long repoAnalyzedCount = githubRepositoryRepository.countBySyncStatus("COMPLETED");
            totalRepos.set(repoCount);
            analyzedRepos.set(repoAnalyzedCount);
            repoAnalysisPercent.set(repoCount > 0 ? (double) repoAnalyzedCount / repoCount * 100.0 : 0.0);

            // Commit Stats
            long commitCount = commitRepository.count();
            long commitAnalyzedCount = commitRepository.countByAnalysisStatus(AnalysisStatus.COMPLETED);
            totalCommits.set(commitCount);
            analyzedCommits.set(commitAnalyzedCount);
            commitAnalysisPercent.set(commitCount > 0 ? (double) commitAnalyzedCount / commitCount * 100.0 : 0.0);

            // Queue Sizes
            Long syncSize = redisTemplate.opsForList().size("github:sync:commit_queue");
            syncQueueSize.set(syncSize != null ? syncSize : 0);

            Long analysisSize = redisTemplate.opsForList().size("github:analysis:queue");
            analysisQueueSize.set(analysisSize != null ? analysisSize : 0);

            log.debug("Updated Business Metrics: Users={}, Teams={}, Sprints={}, SyncQueue={}, AnalysisQueue={}",
                    totalUsers.get(), totalTeams.get(), totalSprints.get(), syncQueueSize.get(),
                    analysisQueueSize.get());
        } catch (Exception e) {
            log.error("Failed to update business metrics", e);
        }
    }
}
