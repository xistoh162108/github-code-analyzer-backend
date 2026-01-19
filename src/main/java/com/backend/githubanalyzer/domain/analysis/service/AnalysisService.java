package com.backend.githubanalyzer.domain.analysis.service;

import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.entity.RiskLevel;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.domain.team.service.SprintRegistrationService;
import com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.infra.openai.OpenAiClient;
import com.backend.githubanalyzer.infra.openai.dto.OpenAiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final OpenAiClient openAiClient;
    private final CommitRepository commitRepository;
    private final UserService userService;
    private final SprintRegistrationService sprintRegistrationService;
    private final TeamRegisterSprintRepository teamRegisterSprintRepository;
    private final ObjectMapper objectMapper;

    @Async("aiAnalysisExecutor")
    @Transactional
    public void analyzeCommitAsync(Commit commit) {
        log.info("Starting AI Analysis for commit: {}", commit.getId().getCommitSha());

        commit.setAnalysisStatus(AnalysisStatus.PROCESSING);
        commitRepository.save(commit);

        try {
            String systemPrompt = constructSystemPrompt();
            String userPrompt = constructUserPrompt(commit);

            openAiClient.analyzeCommit(systemPrompt, userPrompt)
                    .doOnSuccess(response -> updateCommitWithAnalysis(commit, response))
                    .doOnError(error -> handleAnalysisError(commit, error))
                    .block(); // Since we are already in @Async, we can block the Mono for simplicity in this
                              // flow

        } catch (Exception e) {
            handleAnalysisError(commit, e);
        }
    }

    private void updateCommitWithAnalysis(Commit commit, OpenAiAnalysisResponse response) {
        log.info("Analysis completed for commit: {}", commit.getId().getCommitSha());

        commit.setCommitMessageQuality(response.getCommitMessageQuality().getScore());
        commit.setMessageNotes(response.getCommitMessageQuality().getNotes());

        commit.setCodeQuality(response.getCodeQuality().getScore());
        commit.setCodeQualityNotes(response.getCodeQuality().getNotes());

        commit.setChangeAppropriateness(response.getChangeAppropriateness().getScore());
        commit.setScopeNotes(response.getChangeAppropriateness().getNotes());

        commit.setNecessity(response.getNecessity().getScore());
        commit.setNecessityNotes(response.getNecessity().getNotes());

        commit.setCorrectnessAndRisk(response.getCorrectnessAndRisk().getScore());
        commit.setCorrectnessRiskNotes(response.getCorrectnessAndRisk().getNotes());
        commit.setRiskLevel(RiskLevel.valueOf(response.getCorrectnessAndRisk().getRiskLevel()));

        commit.setTestingAndVerification(response.getTestingAndVerification().getScore());
        commit.setTestingNotes(response.getTestingAndVerification().getNotes());

        commit.setSummary(response.getSummary());

        try {
            commit.setStrengths(objectMapper.writeValueAsString(response.getStrengths()));
            commit.setIssues(objectMapper.writeValueAsString(response.getIssues()));
            commit.setSuggestedNextCommit(objectMapper.writeValueAsString(response.getSuggestedNextCommit()));
        } catch (Exception e) {
            log.warn("Failed to serialize list to JSON for commit {}", commit.getId().getCommitSha());
        }

        commit.setAnalysisStatus(AnalysisStatus.COMPLETED);
        commit.setAnalysisCreatedAt(LocalDateTime.now());
        commit.setAnalysisModel("gpt-4o-mini"); // Should come from config ideally

        // Re-calculate total score (handled by @PreUpdate mostly but to be safe)
        commit.calculateTotalScore();
        commitRepository.save(commit);

        // Refresh stats
        userService.refreshUserStats(commit.getAuthor());
        List<TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findByRepositoryId(commit.getRepository().getId());
        for (TeamRegisterSprint reg : registrations) {
            sprintRegistrationService.refreshTeamSprintStats(reg);
        }
    }

    private void handleAnalysisError(Commit commit, Throwable error) {
        log.error("Analysis failed for commit: {}", commit.getId().getCommitSha(), error);
        commit.setAnalysisStatus(AnalysisStatus.FAILED);
        commit.setAnalysisReason(error.getMessage());
        commitRepository.save(commit);
    }

    private String constructSystemPrompt() {
        return "You are an expert Senior Software Engineer and Code Reviewer. " +
                "Your task is to analyze a GitHub commit based on its message and diff. " +
                "Provide scores (0-10) and detailed notes for the following categories: " +
                "1. Commit Message Quality, 2. Code Quality, 3. Change Appropriateness (Atomicity/Scope), " +
                "4. Necessity & Signal-to-Noise, 5. Correctness & Risk, 6. Testing & Verification Evidence. " +
                "Also provide a 'risk_level' (LOW, MED, HIGH). " +
                "Include a summary, strengths, issues, and suggested next commits. " +
                "Respond strictly in JSON format.\n\n" +
                "### EXAMPLE ANALYSIS (Few-Shot)\n" +
                "Input Commit Message: 'fix null pointer in user service'\n" +
                "Input Diff: '- if (user == null) user.getName(); + if (user != null) user.getName();'\n" +
                "Output JSON:\n" +
                "{\n" +
                "  \"commit_message_quality\": {\"score\": 6, \"notes\": \"Clear purpose but could follow conventional commits (e.g., fix: ...).\"},\n"
                +
                "  \"code_quality\": {\"score\": 9, \"notes\": \"Correctly handles the potential null pointer with a guard clause.\"},\n"
                +
                "  \"change_appropriateness\": {\"score\": 10, \"notes\": \"Highly atomic change focusing only on the fix.\"},\n"
                +
                "  \"necessity\": {\"score\": 10, \"notes\": \"Essential fix for application stability.\"},\n" +
                "  \"correctness_and_risk\": {\"score\": 9, \"notes\": \"Low risk, direct fix.\", \"risk_level\": \"LOW\"},\n"
                +
                "  \"testing_and_verification\": {\"score\": 4, \"notes\": \"No test case added to prevent regression.\"},\n"
                +
                "  \"summary\": \"Fixed a critical NullPointerException in UserService by adding a null check.\",\n" +
                "  \"strengths\": [\"Atomic change\", \"Correct logic\"],\n" +
                "  \"issues\": [\"No regression test added\", \"Non-conventional commit message\"],\n" +
                "  \"suggested_next_commit\": [\"Add unit test for UserService null case\", \"Refactor UserService to use Optional\"]\n"
                +
                "}";
    }

    private String constructUserPrompt(Commit commit) {
        StringBuilder userPrompt = new StringBuilder();

        // Add parent commit context if available
        if (commit.getBeforeCommitId() != null) {
            commitRepository
                    .findById_CommitShaAndRepositoryId(commit.getBeforeCommitId(), commit.getRepository().getId())
                    .ifPresent(parent -> {
                        userPrompt.append("### PREVIOUS COMMIT CONTEXT\n");
                        userPrompt.append("Message: ").append(parent.getMessage()).append("\n");
                        userPrompt.append("Changed Files: ").append(extractFilesFromDiff(parent.getDiff()))
                                .append("\n\n");
                    });
        }

        userPrompt.append("### CURRENT COMMIT TO ANALYZE\n");
        userPrompt.append("Message: ").append(commit.getMessage()).append("\n\n");
        userPrompt.append("Diff Context:\n").append(commit.getDiff());

        return userPrompt.toString();
    }

    private String extractFilesFromDiff(String diff) {
        if (diff == null || diff.isEmpty())
            return "None";
        java.util.Set<String> files = new java.util.HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^diff --git a/(.+?) b/",
                java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(diff);
        while (matcher.find()) {
            files.add(matcher.group(1));
        }
        return files.isEmpty() ? "None" : String.join(", ", files);
    }
}
