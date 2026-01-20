package com.backend.githubanalyzer.domain.sprint.dto;

import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SprintInfoResponse {
    private String sprintId;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private boolean isPrivate;
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
