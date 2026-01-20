package com.backend.githubanalyzer.domain.repository.dto;

import java.time.LocalDateTime;

public record SyncStatusResponse(
        String status,
        String repoId,
        LocalDateTime requestedAt) {
}
