package com.backend.githubanalyzer.domain.team.dto;

public record TeamCreateRequest( // 팀 생성 요청
        String name,
        String description,
        Long leaderId) {
}
