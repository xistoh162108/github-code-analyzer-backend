package com.backend.githubanalyzer.domain.sprint.dto;

import java.time.LocalDateTime;
import com.backend.githubanalyzer.domain.sprint.entity.Sprint;

public record SprintResponse(
        @io.swagger.v3.oas.annotations.media.Schema(description = "Sprint ID", example = "2024-winter-challenge")
        String id,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Sprint Name", example = "2024 Winter Challenge")
        String name,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Start Date", example = "2024-12-01T00:00:00")
        LocalDateTime startDate,
        @io.swagger.v3.oas.annotations.media.Schema(description = "End Date", example = "2024-12-31T23:59:59")
        LocalDateTime endDate,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Sprint Description", example = "Monthly coding challenge")
        String description,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Manager Username", example = "admin")
        String managerName,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Private Sprint Flag", example = "false")
        Boolean isPrivate,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Registration Open Flag", example = "true")
        Boolean isOpen,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Number of registered teams", example = "5")
        Long teamsCount,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Total number of participants", example = "25")
        Long participantsCount,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Sprint status (UPCOMING, ONGOING, COMPLETED)", example = "ONGOING")
        String status) {
    public static SprintResponse from(Sprint sprint, Long teamsCount, Long participantsCount, String status) {
        return new SprintResponse(
                sprint.getId(),
                sprint.getName(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getDescription(),
                sprint.getManager().getUsername(),
                sprint.getIsPrivate(),
                sprint.getIsOpen(),
                teamsCount,
                participantsCount,
                status);
    }
}
