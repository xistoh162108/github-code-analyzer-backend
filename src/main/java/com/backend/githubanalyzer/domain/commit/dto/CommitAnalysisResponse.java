package com.backend.githubanalyzer.domain.commit.dto;

import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CommitAnalysisResponse {
    private String sha;
    private String message;
    private LocalDateTime committedAt;
    private String authorName;
    private AnalysisStatus analysisStatus;
    private Long totalScore;

    // Detailed Scores
    private Long commitMessageQuality;
    private Long codeQuality;
    private Long changeAppropriateness;
    private Long necessity;
    private Long correctnessAndRisk;
    private Long testingAndVerification;

    // Notes
    private String messageNotes;
    private String codeQualityNotes;
    private String necessityNotes;
    private String correctnessRiskNotes;
    private String testingNotes;

    // Summary & Details
    private String summary;
    private String strengths; // JSON String
    private String issues; // JSON String
    private String suggestedNextCommit; // JSON String
    private RiskLevel riskLevel;
    private String analysisReason;
}
