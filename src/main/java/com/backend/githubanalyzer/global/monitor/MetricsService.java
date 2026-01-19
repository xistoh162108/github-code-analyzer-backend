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

    // Gauges (AtomicLongs to hold value)
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong ghostUsers = new AtomicLong(0);
    private final AtomicLong totalTeams = new AtomicLong(0);
    private final AtomicLong totalSprints = new AtomicLong(0);

    // Analysis Progress Gauges
    private final AtomicLong totalRepos = new AtomicLong(0);
    private final AtomicLong analyzedRepos = new AtomicLong(0);
    private final AtomicReference<Double> repoAnalysisPercent = new AtomicReference<>(0.0);

    private final AtomicLong totalCommits = new AtomicLong(0);
    private final AtomicLong analyzedCommits = new AtomicLong(0);
    private final AtomicReference<Double> commitAnalysisPercent = new AtomicReference<>(0.0);

    // Initializer to register gauges
    @jakarta.annotation.PostConstruct
    public void init() {
        meterRegistry.gauge("business.users.total", totalUsers);
        meterRegistry.gauge("business.users.ghost", ghostUsers);
        meterRegistry.gauge("business.teams.total", totalTeams);
        meterRegistry.gauge("business.sprints.total", totalSprints);

        meterRegistry.gauge("analysis.repos.total", totalRepos);
        meterRegistry.gauge("analysis.repos.analyzed", analyzedRepos);
        meterRegistry.gauge("analysis.repos.percentage", repoAnalysisPercent, AtomicReference::get);

        meterRegistry.gauge("analysis.commits.total", totalCommits);
        meterRegistry.gauge("analysis.commits.completed", analyzedCommits);
        meterRegistry.gauge("analysis.commits.percentage", commitAnalysisPercent, AtomicReference::get);
    }

    // Custom Counter for External APIs
    public void incrementExternalRequest(String target) {
        meterRegistry.counter("external.api.requests", "target", target).increment();
    }

    // Helper to record Job duration
    public void recordJobDuration(String jobName, Runnable task) {
        meterRegistry.timer("job.execution.time", "job", jobName).record(task);
    }

    // Helper to register Queue Size Gauge
    public void registerQueueSize(String queueName, java.util.function.Supplier<Number> sizeSupplier) {
        io.micrometer.core.instrument.Gauge.builder("queue.size", sizeSupplier, s -> s.get().doubleValue())
                .tag("queue", queueName)
                .register(meterRegistry);
    }

    // Scheduled task to update business metrics
    @Scheduled(fixedRate = 60000) // Update every minute
    public void updateBusinessMetrics() {
        try {
            totalUsers.set(userRepository.count());
            ghostUsers.set(userRepository.countByIsGhostTrue());
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

            log.debug("Updated Business Metrics: Users={}, Ghosts={}, Teams={}, Sprints={}",
                    totalUsers.get(), ghostUsers.get(), totalTeams.get(), totalSprints.get());
        } catch (Exception e) {
            log.error("Failed to update business metrics", e);
        }
    }
}
