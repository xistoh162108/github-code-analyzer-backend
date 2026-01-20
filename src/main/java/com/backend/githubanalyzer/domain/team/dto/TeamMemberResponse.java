package com.backend.githubanalyzer.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamMemberResponse(
        @Schema(description = "User ID", example = "1")
        Long userId,
        @Schema(description = "Username", example = "johndoe")
        String username,
        @Schema(description = "Role in team (LEADER, MEMBER, MENTOR)", example = "MEMBER")
        String role,
        @Schema(description = "Membership status (PENDING, APPROVED)", example = "APPROVED")
        String status,
        @Schema(description = "Rank within team", example = "1")
        Long inTeamRank,
        @Schema(description = "Total Commit Count", example = "150")
        Long commitCount,
        @Schema(description = "Contribution Score", example = "850")
        Long contributionScore) {
}