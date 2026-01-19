package com.backend.githubanalyzer.domain.commit.entity;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commits")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Commit {

    @EmbeddedId
    private CommitId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repoId")
    @JoinColumn(name = "repo_id")
    private GithubRepository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "LONGTEXT")
    private String diff;

    @Column(name = "before_commit_id")
    private String beforeCommitId; // Storing parent SHA (String) as requested

    @Column(nullable = false)
    private LocalDateTime committedAt;

    @Column(name = "analysis_result", columnDefinition = "LONGTEXT")
    private String analysisResult;

    @Column(name = "analysis_created_at")
    private LocalDateTime analysisCreatedAt;

    @Column(name = "analysis_model")
    private String analysisModel;

    @Column(name = "analysis_confidence")
    private Double analysisConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "commit_message_quality", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (commit_message_quality BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long commitMessageQuality = 0L;

    @Column(name = "code_quality", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (code_quality BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long codeQuality = 0L;

    @Column(name = "change_appropriateness", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (change_appropriateness BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long changeAppropriateness = 0L;

    @Column(name = "necessity", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (necessity BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long necessity = 0L;

    @Column(name = "correctness_and_risk", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (correctness_and_risk BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long correctnessAndRisk = 0L;

    @Column(name = "testing_and_verification", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (testing_and_verification BETWEEN 0 AND 10)")
    @Min(0)
    @Max(10)
    @Builder.Default
    private Long testingAndVerification = 0L;

    @Column(name = "total_score", nullable = false, columnDefinition = "BIGINT DEFAULT 0 CHECK (total_score BETWEEN 0 AND 100)")
    @Min(0)
    @Max(100)
    @Builder.Default
    private Long totalScore = 0L;

    @Column(name = "analysis_reason", columnDefinition = "LONGTEXT")
    private String analysisReason;

    @Column(name = "message_notes", columnDefinition = "TEXT")
    private String messageNotes;

    @Column(name = "code_quality_notes", columnDefinition = "TEXT")
    private String codeQualityNotes;

    @Column(name = "scope_notes", columnDefinition = "TEXT")
    private String scopeNotes;

    @Column(name = "necessity_notes", columnDefinition = "TEXT")
    private String necessityNotes;

    @Column(name = "correctness_risk_notes", columnDefinition = "TEXT")
    private String correctnessRiskNotes;

    @Column(name = "testing_notes", columnDefinition = "TEXT")
    private String testingNotes;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "strengths", columnDefinition = "TEXT") // JSON array
    private String strengths;

    @Column(name = "issues", columnDefinition = "TEXT") // JSON array
    private String issues;

    @Column(name = "suggested_next_commit", columnDefinition = "TEXT") // JSON array
    private String suggestedNextCommit;

    @PrePersist
    @PreUpdate
    public void calculateTotalScore() {
        this.totalScore = ((commitMessageQuality != null ? commitMessageQuality : 0L) * 10 +
                (codeQuality != null ? codeQuality : 0L) * 30 +
                (changeAppropriateness != null ? changeAppropriateness : 0L) * 20 +
                (necessity != null ? necessity : 0L) * 15 +
                (correctnessAndRisk != null ? correctnessAndRisk : 0L) * 15 +
                (testingAndVerification != null ? testingAndVerification : 0L) * 10) / 10;

        // Clamp total score to 0-100 just in case
        this.totalScore = Math.max(0, Math.min(100, this.totalScore));
    }
}
