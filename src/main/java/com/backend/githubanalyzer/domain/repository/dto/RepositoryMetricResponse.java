package com.backend.githubanalyzer.domain.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RepositoryMetricResponse {
    private Long commitCount;
    private Long averageScore;
}
