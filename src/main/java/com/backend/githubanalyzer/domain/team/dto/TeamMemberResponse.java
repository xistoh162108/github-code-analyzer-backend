package com.backend.githubanalyzer.domain.team.dto;

public record TeamMemberResponse( // 팀원 조회 응답
        Long userId,
        String username,
        String role,
        String status,
        Long inTeamRank,
        Long commitCount,
        Long contributionScore) {
}