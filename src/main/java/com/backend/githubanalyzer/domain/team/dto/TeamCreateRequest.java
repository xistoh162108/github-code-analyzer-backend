package com.backend.githubanalyzer.domain.team.dto;

public record TeamCreateRequest(
        @io.swagger.v3.oas.annotations.media.Schema(description = "Team Name", example = "Dev Team A")
        String name,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Team Description", example = "Backend development team")
        String description,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Leader User ID", example = "1")
        Long leaderId) {
}
