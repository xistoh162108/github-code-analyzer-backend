package com.backend.githubanalyzer.domain.commit.dto;

import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CommitResponse {
    private String sha;
    private String message;
    private LocalDateTime committedAt;
    private String authorName;
    private String authorProfileUrl;
    private AnalysisStatus analysisStatus;
    private Long totalScore;
}
