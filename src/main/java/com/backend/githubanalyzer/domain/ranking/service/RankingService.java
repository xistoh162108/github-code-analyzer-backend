package com.backend.githubanalyzer.domain.ranking.service;

import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.ranking.dto.CommitRankResponse;
import com.backend.githubanalyzer.domain.ranking.dto.UserRankResponse;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import com.backend.githubanalyzer.domain.sprint.repository.SprintRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final CommitRepository commitRepository;
    private final SprintRepository sprintRepository;
    private final com.backend.githubanalyzer.domain.team.repository.UserRegisterTeamRepository userRegisterTeamRepository;
    private final TeamRegisterSprintRepository teamRegisterSprintRepository;

    public enum Period {
        ALL, YEAR, MONTH, WEEK, DAY, HOUR, SPRINT
    }

    // --- Commit Rankings ---

    public List<CommitRankResponse> getCommitRankings(String scope, String id, Period period, int limit) {
        // ... (Time Calc same)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;

        if (period == Period.SPRINT && "sprint".equalsIgnoreCase(scope)) {
            Sprint sprint = sprintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sprint not found"));
            start = sprint.getStartDate();
            end = now.isAfter(sprint.getEndDate()) ? sprint.getEndDate() : now;
        } else {
            start = calculateStartDate(period, now);
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Commit> commits;

        switch (scope.toLowerCase()) {
            case "global":
                commits = commitRepository.findTopCommitsGlobal(start, end, pageable);
                break;
            case "sprint":
                List<String> sprintRepoIds = teamRegisterSprintRepository.findAllBySprintId(id).stream()
                        .map(reg -> reg.getRepository().getId())
                        .collect(Collectors.toList());
                if (sprintRepoIds.isEmpty())
                    return Collections.emptyList();
                commits = commitRepository.findTopCommitsByRepos(sprintRepoIds, start, end, pageable);
                break;
            case "team":
                List<Long> memberIds = userRegisterTeamRepository.findByTeamId(id).stream()
                        .map(m -> m.getUser().getId())
                        .collect(Collectors.toList());
                if (memberIds.isEmpty())
                    return Collections.emptyList();
                commits = commitRepository.findTopCommitsByAuthors(memberIds, start, end, pageable);
                break;
            case "user":
                commits = commitRepository.findTopCommitsByUser(Long.parseLong(id), start, end, pageable);
                break;
            default:
                throw new IllegalArgumentException("Invalid scope: " + scope);
        }

        long[] rank = { 1 };
        return commits.stream()
                .map(c -> CommitRankResponse.of(rank[0]++, c))
                .collect(Collectors.toList());
    }

    // --- User Rankings ---

    public List<UserRankResponse> getUserRankings(String scope, String id, Period period, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = calculateStartDate(period, now);
        LocalDateTime end = now;

        if (period == Period.SPRINT && "sprint".equalsIgnoreCase(scope)) {
            Sprint sprint = sprintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sprint not found"));
            start = sprint.getStartDate();
            end = now.isAfter(sprint.getEndDate()) ? sprint.getEndDate() : now;
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results;

        switch (scope.toLowerCase()) {
            case "global":
                results = commitRepository.findUserRankingGlobal(start, end, pageable);
                break;
            case "sprint":
                List<String> sprintRepoIds = teamRegisterSprintRepository.findAllBySprintId(id).stream()
                        .map(reg -> reg.getRepository().getId())
                        .collect(Collectors.toList());
                if (sprintRepoIds.isEmpty())
                    return Collections.emptyList();
                results = commitRepository.findUserRankingByRepos(sprintRepoIds, start, end, pageable);
                break;
            case "team":
                List<Long> memberIds = userRegisterTeamRepository.findByTeamId(id).stream()
                        .map(m -> m.getUser().getId())
                        .collect(Collectors.toList());
                if (memberIds.isEmpty())
                    return Collections.emptyList();
                results = commitRepository.findUserRankingByAuthors(memberIds, start, end, pageable);
                break;
            default:
                throw new IllegalArgumentException("Invalid scope: " + scope);
        }

        long[] rank = { 1 };
        return results.stream()
                .map(row -> {
                    User user = (User) row[0];
                    Long score = (Long) row[1];
                    return UserRankResponse.of(rank[0]++, user, score != null ? score : 0);
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateStartDate(Period period, LocalDateTime now) {
        return switch (period) {
            case ALL -> LocalDateTime.of(2000, 1, 1, 0, 0);
            case YEAR -> now.minusYears(1);
            case MONTH -> now.minusMonths(1);
            case WEEK -> now.minusWeeks(1);
            case DAY -> now.minusDays(1);
            case HOUR -> now.minusHours(1);
            case SPRINT -> LocalDateTime.of(2000, 1, 1, 0, 0); // Default if not handled
        };
    }
}
