package com.backend.githubanalyzer.domain.sprint.dto;

import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;

public record SprintRegistrationResponse(
        String sprintId,
        String teamId,
        String repoId,
        String status,
        String webhookUrl,
        Boolean webhookSent,
        String webhookError) {
    public static SprintRegistrationResponse from(TeamRegisterSprint registration) {
        return new SprintRegistrationResponse(
                registration.getSprint().getId(),
                registration.getTeam().getId(),
                registration.getRepository().getId(),
                registration.getStatus(),
                null, null, null);
    }

    public static SprintRegistrationResponse from(TeamRegisterSprint registration, String webhookUrl, boolean success,
            String error) {
        return new SprintRegistrationResponse(
                registration.getSprint().getId(),
                registration.getTeam().getId(),
                registration.getRepository().getId(),
                registration.getStatus(),
                webhookUrl, success, error);
    }
}
