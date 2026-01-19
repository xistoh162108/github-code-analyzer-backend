package com.backend.githubanalyzer.domain.sprint.dto;

public record SprintRegisterRequest(
        String teamId,
        String repoId) {
}
