package com.backend.githubanalyzer.infra.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiAnalysisResponse {

    @JsonProperty("commit_message_quality")
    private ScoreResult commitMessageQuality;

    @JsonProperty("code_quality")
    private ScoreResult codeQuality;

    @JsonProperty("change_appropriateness")
    private ScoreResult changeAppropriateness;

    @JsonProperty("necessity")
    private ScoreResult necessity;

    @JsonProperty("correctness_and_risk")
    private RiskResult correctnessAndRisk;

    @JsonProperty("testing_and_verification")
    private ScoreResult testingAndVerification;

    private String summary;
    private List<String> strengths;
    private List<String> issues; // Simple list or structured

    @JsonProperty("suggested_next_commit")
    private List<String> suggestedNextCommit;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreResult {
        private Long score;
        private String notes;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskResult {
        private Long score;
        private String notes;
        @JsonProperty("risk_level")
        private String riskLevel; // LOW, MED, HIGH
    }
}
