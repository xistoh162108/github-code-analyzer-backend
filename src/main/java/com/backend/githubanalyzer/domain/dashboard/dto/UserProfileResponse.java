package com.backend.githubanalyzer.domain.dashboard.dto;

import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    private String username;
    private String profileUrl;
    private Long totalScore;
    private Long totalCommits;
    private List<SprintResponse> participatingSprints;
}
