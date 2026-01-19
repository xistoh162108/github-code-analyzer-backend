package com.backend.githubanalyzer.domain.dashboard.dto;

import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardStatsResponse {
    private String username;
    private Long totalScore;
    private Long totalCommits;
    private Integer currentStreak;
    private List<SprintResponse> activeSprints;
}
