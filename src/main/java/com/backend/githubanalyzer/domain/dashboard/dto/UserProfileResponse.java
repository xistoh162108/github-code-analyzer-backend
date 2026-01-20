package com.backend.githubanalyzer.domain.dashboard.dto;

import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    @Schema(description = "Username", example = "johndoe")
    private String username;
    @Schema(description = "Profile Image URL", example = "https://github.com/johndoe.png")
    private String profileUrl;
    @Schema(description = "Total Score", example = "1500")
    private Long totalScore;
    @Schema(description = "Total Commits", example = "300")
    private Long totalCommits;
    @Schema(description = "Sprints user is participating in or managed")
    private List<SprintResponse> participatingSprints;
}
