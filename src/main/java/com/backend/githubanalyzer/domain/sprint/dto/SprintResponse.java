package com.backend.githubanalyzer.domain.sprint.dto;

import java.time.LocalDateTime;
import com.backend.githubanalyzer.domain.sprint.entity.Sprint;

public record SprintResponse(
        String id,
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String description,
        String managerName,
        Boolean isPrivate,
        Boolean isOpen,
        Long teamsCount,
        Long participantsCount,
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
