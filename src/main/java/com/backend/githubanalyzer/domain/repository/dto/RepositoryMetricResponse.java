package com.backend.githubanalyzer.domain.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "레포지토리 통계 응답")
public class RepositoryMetricResponse {
    @io.swagger.v3.oas.annotations.media.Schema(description = "총 커밋 수", example = "150")
    private Long commitCount;

    @io.swagger.v3.oas.annotations.media.Schema(description = "평균 코드 품질 점수 (0-100)", example = "85.5")
    private Double averageScore;

    @io.swagger.v3.oas.annotations.media.Schema(description = "레포지토리 활동 점수 (Activity Score)", example = "1500")
    private Long totalScore;
}
