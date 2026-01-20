package com.backend.githubanalyzer.domain.sprint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SprintCreateRequest(
        @Schema(description = "Sprint Name", example = "2024 Winter Challenge")
        String name,
        @Schema(description = "Start Date", example = "2024-12-01T00:00:00")
        LocalDateTime startDate,
        @Schema(description = "End Date", example = "2024-12-31T23:59:59")
        LocalDateTime endDate,
        @Schema(description = "Description", example = "Monthly coding challenge")
        String description,
        @Schema(description = "Private Sprint Flag", example = "false")
        Boolean isPrivate,
        @Schema(description = "Registration Open Flag", example = "true")
        Boolean isOpen,
        @Schema(description = "Manager User ID", example = "1")
        Long managerId) {
}
