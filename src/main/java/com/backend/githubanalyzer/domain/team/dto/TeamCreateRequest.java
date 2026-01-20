package com.backend.githubanalyzer.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamCreateRequest(
        @Schema(description = "Team Name", example = "Dev Team A")
        String name,
        @Schema(description = "Team Description", example = "Backend development team")
        String description,
        @Schema(description = "Leader User ID", example = "1")
        Long leaderId,
        @Schema(description = "Public Team Flag", example = "true")
        Boolean isPublic) {
}
