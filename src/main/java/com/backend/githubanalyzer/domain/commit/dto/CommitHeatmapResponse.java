package com.backend.githubanalyzer.domain.commit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CommitHeatmapResponse {
    private String date;
    private Long count;
}
