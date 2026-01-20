package com.backend.githubanalyzer.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamUpdateRequest(
        @Schema(description = "Updated Team Name", example = "SRE Team")
        String name,
        @Schema(description = "Updated Team Description", example = "Site Reliability Engineering")
        String description
) {
}
