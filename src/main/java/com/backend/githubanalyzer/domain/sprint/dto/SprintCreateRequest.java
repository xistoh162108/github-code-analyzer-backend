package com.backend.githubanalyzer.domain.sprint.dto;

import java.time.LocalDateTime;

public record SprintCreateRequest(
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String description,
        Boolean isPrivate,
        Boolean isOpen,
        Long managerId) {
}
