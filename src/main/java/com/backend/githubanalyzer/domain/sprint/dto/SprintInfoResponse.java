package com.backend.githubanalyzer.domain.sprint.dto;

import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SprintInfoResponse {
    @Schema(description = "Sprint ID", example = "sprint-123")
    private String sprintId;
    @Schema(description = "Sprint Name", example = "Alpha Sprint")
    private String name;
    @Schema(description = "Start Date", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;
    @Schema(description = "End Date", example = "2024-02-01T00:00:00")
    private LocalDateTime endDate;
    @Schema(description = "Sprint status", example = "ONGOING")
    private String status;
    @Schema(description = "Private Sprint Flag", example = "false")
    private boolean isPrivate;
    @Schema(description = "Registration Open Flag", example = "true")
    private boolean isOpen;

    public static SprintInfoResponse from(Sprint sprint, String status) {
        return SprintInfoResponse.builder()
                .sprintId(sprint.getId())
                .name(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(status)
                .isPrivate(sprint.getIsPrivate())
                .isOpen(sprint.getIsOpen())
                .build();
    }
}
