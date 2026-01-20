package com.backend.githubanalyzer.domain.dashboard.dto;

import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardStatsResponse {
    @Schema(description = "Username", example = "johndoe")
    private String username;
    @Schema(description = "Total Score", example = "1500")
    private Long totalScore;
    @Schema(description = "Total Commits", example = "300")
    private Long totalCommits;
    @Schema(description = "Current Commit Streak", example = "5")
    private Integer currentStreak;
    @Schema(description = "Active Sprints participating in")
    private List<SprintResponse> activeSprints;
}
