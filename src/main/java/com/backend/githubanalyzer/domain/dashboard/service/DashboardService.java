package com.backend.githubanalyzer.domain.dashboard.service;

import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.dashboard.dto.DashboardStatsResponse;
import com.backend.githubanalyzer.domain.dashboard.dto.UserProfileResponse;
import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import com.backend.githubanalyzer.domain.sprint.service.SprintService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final CommitRepository commitRepository;
    private final SprintService sprintService;

    public DashboardStatsResponse getDashboardStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Basic Stats
        long totalScore = user.getScore();
        // Recalculate or trust user.getScore()? Trust for now as it's updated via
        // RefreshStats.

        // Count total commits for user across all repos
        // We lack a direct "total commits" field on User or a fast query.
        // contributionRepository might imply count, but let's query CommitRepo directly
        // if indexed.
        // Assuming performance is okay for now or we add a field later.
        // For MVP, maybe count distinct commits authored by user.
        // But CommitRepository query might be heavy. Let's start with a new query or
        // use stored stats if available.
        // User entity doesn't have commit count.
        // We can add `countByAuthorId` to CommitRepository.
        long totalCommits = commitRepository.countByAuthorId(userId);

        // 2. Streak Calculation
        int currentStreak = calculateCurrentStreak(userId);

        // 3. Participating Sprints
        List<SprintResponse> mySprints = sprintService.getMyParticipatingSprints(userId);

        return DashboardStatsResponse.builder()
                .username(user.getUsername())
                .totalScore(totalScore)
                .totalCommits(totalCommits)
                .currentStreak(currentStreak)
                .activeSprints(mySprints)
                .build();
    }

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        long totalCommits = commitRepository.countByAuthorId(user.getId());
        List<SprintResponse> mySprints = sprintService.getMyParticipatingSprints(user.getId());

        // Recent Activity? Maybe return last 5 commits for profile?
        // For simplicity:
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .profileUrl(user.getProfileUrl())
                .totalScore(user.getTotalScore())
                .averageScore(user.getScore()) // Maps to User.score (Average)
                .totalCommits(totalCommits)
                .participatingSprints(mySprints)
                .build();
    }

    private int calculateCurrentStreak(Long userId) {
        List<LocalDateTime> commitDates = commitRepository.findDistinctCommittedAtByAuthorId(userId);

        if (commitDates.isEmpty()) {
            return 0;
        }

        // Convert to LocalDates and distinct sorted descending
        List<LocalDate> dates = commitDates.stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .sorted((a, b) -> b.compareTo(a)) // Descending
                .collect(Collectors.toList());

        if (dates.isEmpty())
            return 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // If no commit today or yesterday, streak is 0
        if (!dates.get(0).equals(today) && !dates.get(0).equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        for (int i = 0; i < dates.size() - 1; i++) {
            LocalDate current = dates.get(i);
            LocalDate next = dates.get(i + 1);

            if (current.minusDays(1).equals(next)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
