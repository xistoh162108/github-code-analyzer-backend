package com.backend.githubanalyzer.domain.sprint.dto;

public record SprintTeamRankingResponse(
        Long rank,
        String teamName,
        Long score,
        Long commits,
        Long memberCount) {
}
