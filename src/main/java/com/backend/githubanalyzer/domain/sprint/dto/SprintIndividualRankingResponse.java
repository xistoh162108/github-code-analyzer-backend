package com.backend.githubanalyzer.domain.sprint.dto;

public record SprintIndividualRankingResponse(
        Long rank,
        String username, // GitHub nickname/name
        String userId, // GitHub Login ID (@handle)
        String avatarUrl,
        Long score,
        Long commits) {
}
