package com.backend.githubanalyzer.domain.sprint.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SprintRegisterRequest(
        @Schema(description = "Team ID to register", example = "550e8400-e29b-41d4-a716-446655440000")
        String teamId,
        @Schema(description = "Repository ID to use for the sprint", example = "MDEwOlJlcG9zaXRvcnkzMDc0NDQ4NDI=")
        String repoId) {
}
