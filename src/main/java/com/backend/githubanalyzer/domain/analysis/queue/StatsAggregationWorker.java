package com.backend.githubanalyzer.domain.analysis.queue;

import com.backend.githubanalyzer.domain.analysis.service.ScoreAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsAggregationWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScoreAggregationService scoreAggregationService;

    private static final String DIRTY_REPOS_KEY = "aggregation:dirty:repos";
    private static final String DIRTY_USERS_KEY = "aggregation:dirty:users";
    private static final String DIRTY_TEAMS_KEY = "aggregation:dirty:teams";

    @Scheduled(fixedDelay = 60000) // Run every 1 minute
    public void processDirtyStats() {
        log.info("Starting periodic stats aggregation for dirty entities...");

        processRepos();
        processUsers();
        processTeams();

        log.info("Finished periodic stats aggregation.");
    }

    private void processRepos() {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_REPOS_KEY))) {
            return;
        }
        String processingKey = DIRTY_REPOS_KEY + ":processing";
        redisTemplate.renameIfAbsent(DIRTY_REPOS_KEY, processingKey);

        Set<Object> repoIds = redisTemplate.opsForSet().members(processingKey);
        if (repoIds != null && !repoIds.isEmpty()) {
            log.info("Found {} dirty repositories to aggregate.", repoIds.size());
            for (Object id : repoIds) {
                try {
                    scoreAggregationService.updateRepoStats((String) id);
                    redisTemplate.opsForSet().remove(processingKey, id);
                } catch (Exception e) {
                    log.error("Failed to aggregate stats for repo: {}. Keeping in pool.", id, e);
                }
            }
        }
        redisTemplate.delete(processingKey);
    }

    private void processUsers() {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_USERS_KEY))) {
            return;
        }
        String processingKey = DIRTY_USERS_KEY + ":processing";
        redisTemplate.renameIfAbsent(DIRTY_USERS_KEY, processingKey);

        Set<Object> userIds = redisTemplate.opsForSet().members(processingKey);
        if (userIds != null && !userIds.isEmpty()) {
            log.info("Found {} dirty users to aggregate.", userIds.size());
            for (Object id : userIds) {
                try {
                    scoreAggregationService.updateUserStats(Long.valueOf(id.toString()));
                    redisTemplate.opsForSet().remove(processingKey, id);
                } catch (Exception e) {
                    log.error("Failed to aggregate stats for user: {}. Keeping in pool.", id, e);
                }
            }
        }
        redisTemplate.delete(processingKey);
    }

    private void processTeams() {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_TEAMS_KEY))) {
            return;
        }
        String processingKey = DIRTY_TEAMS_KEY + ":processing";
        redisTemplate.renameIfAbsent(DIRTY_TEAMS_KEY, processingKey);

        Set<Object> repoIdsForTeams = redisTemplate.opsForSet().members(processingKey);
        if (repoIdsForTeams != null && !repoIdsForTeams.isEmpty()) {
            log.info("Found {} repositories with dirty team/sprint stats.", repoIdsForTeams.size());
            for (Object id : repoIdsForTeams) {
                try {
                    // This updates all sprint registrations linked to this repo
                    // (userId is not needed for the broad repo activity update)
                    scoreAggregationService.updateTeamSprintStats((String) id, null);
                    redisTemplate.opsForSet().remove(processingKey, id);
                } catch (Exception e) {
                    log.error("Failed to aggregate sprint stats for repo: {}. Keeping in pool.", id, e);
                    redisTemplate.opsForSet().add(processingKey, id);
                }
            }
        }
        redisTemplate.delete(processingKey);
    }
}
