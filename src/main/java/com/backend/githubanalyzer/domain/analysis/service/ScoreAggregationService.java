package com.backend.githubanalyzer.domain.analysis.service;

import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreAggregationService {

    private final CommitRepository commitRepository;
    private final GithubRepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final TeamRegisterSprintRepository teamRegisterSprintRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DIRTY_REPOS_KEY = "aggregation:dirty:repos";
    private static final String DIRTY_USERS_KEY = "aggregation:dirty:users";
    private static final String DIRTY_TEAMS_KEY = "aggregation:dirty:teams";

    @Transactional
    public void updateRepoStats(String repoId) {
        log.info("Updating real-time stats for Repo: {}", repoId);
        repositoryRepository.findById(repoId).ifPresent(repo -> {
            Long totalCompletedScore = commitRepository.sumCompletedScoreByRepositoryId(repoId);
            long completedCount = commitRepository.countCompletedByRepositoryId(repoId);
            long totalCount = commitRepository.countUniqueCommitsByRepositoryId(repoId);

            long avgScore = (completedCount > 0) ? (totalCompletedScore / completedCount) : 0L;

            repo.setScore(avgScore);
            repo.setCommitCount(totalCount);
            repositoryRepository.save(repo);
        });
    }

    @Transactional
    public void updateUserStats(Long userId) {
        log.info("Updating real-time stats for User: {}", userId);
        userRepository.findById(userId).ifPresent(user -> {
            Long totalCompletedScore = commitRepository.sumCompletedScoreByAuthorId(userId);
            long completedCount = commitRepository.countCompletedByAuthorId(userId);
            long totalCount = commitRepository.countByAuthorId(userId);

            long avgScore = (completedCount > 0) ? (totalCompletedScore / completedCount) : 0L;

            user.setScore(avgScore);
            // Formula: commit count * average score
            user.setTotalScore(totalCount * avgScore);
            user.setCommitCount(totalCount);
            userRepository.save(user);
        });
    }

    @Transactional
    public void updateTeamSprintStats(String repoId, Long userId) {
        log.info("Updating TeamSprint stats for Repo {} and User {}", repoId, userId);
        // Find all sprint registrations that use this repo
        List<TeamRegisterSprint> registrations = teamRegisterSprintRepository.findByRepositoryId(repoId);

        for (TeamRegisterSprint reg : registrations) {
            LocalDateTime start = reg.getSprint().getStartDate();
            LocalDateTime end = reg.getSprint().getEndDate();

            Long totalCompletedScore = commitRepository.sumCompletedScoreByRepoAndTime(repoId, start, end);
            long completedCount = commitRepository.countCompletedByRepoAndTime(repoId, start, end);
            long totalCount = commitRepository.countByRepoAndTime(repoId, start, end);

            long avgScore = (completedCount > 0) ? (totalCompletedScore / completedCount) : 0L;

            reg.setScore(avgScore);
            reg.setCommitNum(totalCount);
            teamRegisterSprintRepository.save(reg);
        }
    }

    public void markRepoDirty(String repoId) {
        redisTemplate.opsForSet().add(DIRTY_REPOS_KEY, repoId);
    }

    public void markUserDirty(Long userId) {
        redisTemplate.opsForSet().add(DIRTY_USERS_KEY, String.valueOf(userId));
    }

    public void markTeamDirty(String repoId) {
        // Teams are recalculated based on repository activity within a sprint
        redisTemplate.opsForSet().add(DIRTY_TEAMS_KEY, repoId);
    }
}
