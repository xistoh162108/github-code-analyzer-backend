package com.backend.githubanalyzer.domain.analysis.service;

import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.entity.RiskLevel;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.infra.openai.OpenAiClient;
import com.backend.githubanalyzer.infra.openai.dto.OpenAiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final OpenAiClient openAiClient;
    private final CommitRepository commitRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final ScoreAggregationService scoreAggregationService;

    private static final String SCORE_STATS_KEY_PREFIX = "analysis:stats:";

    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    // Helper record to hold prompts
    private record AnalysisPrompts(String system, String user) {
    }

    public void analyzeCommitSync(String commitSha, String repositoryId) {
        log.info("Starting AI Analysis for commit: {}", commitSha);

        try {
            // Step 1: Prepare (Set Status PROCESSING, Build Prompts) - Short Transaction
            AnalysisPrompts prompts = transactionTemplate.execute(status -> {
                Commit commit = commitRepository.findById_CommitShaAndRepositoryId(commitSha, repositoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Commit not found: " + commitSha));

                commit.setAnalysisStatus(AnalysisStatus.PROCESSING);
                commitRepository.saveAndFlush(commit); // Commit status change immediately

                String systemPrompt = constructSystemPrompt();
                String userPrompt = constructUserPrompt(commit);
                return new AnalysisPrompts(systemPrompt, userPrompt);
            });

            if (prompts == null)
                throw new IllegalStateException("Failed to prepare analysis prompts");

            // Step 2: AI Call (No Transaction, Long running)
            // Block here is safe because we are not holding a DB connection
            OpenAiAnalysisResponse response = openAiClient.analyzeCommit(prompts.system(), prompts.user()).block();

            // Step 3: Save Results - Short Transaction
            transactionTemplate.execute(status -> {
                Commit commit = commitRepository.findById_CommitShaAndRepositoryId(commitSha, repositoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Commit not found during save: " + commitSha));
                updateCommitWithAnalysis(commit, response);
                return null;
            });

        } catch (Exception e) {
            log.error("Error during analysis flow for commit: {}", commitSha, e);
            // Step 4: Handle Error - Short Transaction
            try {
                transactionTemplate.execute(status -> {
                    commitRepository.findById_CommitShaAndRepositoryId(commitSha, repositoryId)
                            .ifPresent(commit -> handleAnalysisError(commit, e));
                    return null;
                });
            } catch (Exception ex) {
                log.error("Failed to save error status for commit: {}", commitSha, ex);
            }
        }
    }

    private void updateCommitWithAnalysis(Commit commit, OpenAiAnalysisResponse response) {
        log.info("Analysis completed for commit: {}", commit.getId().getCommitSha());
        // ... (rest of method logic remains same, but ensure no @Transactional on this
        // private method if mostly called from within TT)

        // Match existing logic...
        // Apply Z-Score Normalization for each metric
        log.info("Raw Message Quality Score: {}", response.getCommitMessageQuality().getScore());
        Long normalizedMessageScore = normalizeAndSave("message_quality",
                response.getCommitMessageQuality().getScore());

        commit.setCommitMessageQuality(normalizedMessageScore);
        commit.setMessageNotes(response.getCommitMessageQuality().getNotes());

        commit.setCodeQuality(normalizeAndSave("code_quality", response.getCodeQuality().getScore()));
        commit.setCodeQualityNotes(response.getCodeQuality().getNotes());

        commit.setChangeAppropriateness(
                normalizeAndSave("appropriateness", response.getChangeAppropriateness().getScore()));
        commit.setScopeNotes(response.getChangeAppropriateness().getNotes());

        commit.setNecessity(normalizeAndSave("necessity", response.getNecessity().getScore()));
        commit.setNecessityNotes(response.getNecessity().getNotes());

        commit.setCorrectnessAndRisk(normalizeAndSave("correctness", response.getCorrectnessAndRisk().getScore()));
        commit.setCorrectnessRiskNotes(response.getCorrectnessAndRisk().getNotes());
        commit.setRiskLevel(RiskLevel.valueOf(response.getCorrectnessAndRisk().getRiskLevel()));

        commit.setTestingAndVerification(normalizeAndSave("testing", response.getTestingAndVerification().getScore()));
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
        commit.setAnalysisModel("gpt-4o-mini");

        commit.calculateTotalScore();
        commitRepository.save(commit);

        // Remove flush here as transactionTemplate will commit at end of block
        // commitRepository.flush();

        scoreAggregationService.markRepoDirty(commit.getRepository().getId());
        scoreAggregationService.markUserDirty(commit.getAuthor().getId());
        scoreAggregationService.markTeamDirty(commit.getRepository().getId());
    }

    private void handleAnalysisError(Commit commit, Throwable error) {
        // ... (as before)
        log.error("!!! ANALYSIS FAILED for commit: {} !!!", commit.getId().getCommitSha());
        log.error("Reason: {}", error.getMessage());
        log.error("Stack Trace:", error);

        commit.setAnalysisStatus(AnalysisStatus.FAILED);
        commit.setAnalysisReason(error.getMessage());
        commitRepository.save(commit);
    }

    private String constructSystemPrompt() {
        return """
                You are an expert Senior Software Engineer and Code Reviewer with a strict and critical eye.
                Your task is to analyze a GitHub commit based on its message, diff, and repository context.

                Respond **strictly** in the following JSON format (no markdown blocks):
                {
                    "commit_message_quality": { "score": <0-100>, "notes": "..." },
                    "code_quality": { "score": <0-100>, "notes": "..." },
                    "change_appropriateness": { "score": <0-100>, "notes": "..." },
                    "necessity": { "score": <0-100>, "notes": "..." },
                    "correctness_and_risk": { "score": <0-100>, "notes": "...", "risk_level": "LOW|MED|HIGH" },
                    "testing_and_verification": { "score": <0-100>, "notes": "..." },
                    "summary": "...",
                    "strengths": ["...", "..."],
                    "issues": ["...", "..."],
                    "suggested_next_commit": ["...", "..."]
                }

                ### CRITICAL EVALUATION GUIDELINES
                - Be extremely critical. A 'standard' commit should score 50-60. 90-100 is reserved for exceptional, production-grade perfection.
                - Use the full scale (0-100). If a commit is poor, do not hesitate to give a 10-30.
                - "necessity" maps to Signal-to-Noise ratio.
                - "change_appropriateness" maps to Atomicity/Scope.
                """;
    }

    private String constructUserPrompt(Commit commit) {
        StringBuilder userPrompt = new StringBuilder();

        userPrompt.append("### REPOSITORY CONTEXT\n");
        userPrompt.append("Name: ").append(commit.getRepository().getReponame()).append("\n");
        userPrompt.append("Description: ").append(commit.getRepository().getDescription()).append("\n\n");

        // Add parent commit context (can be multiple)
        if (commit.getBeforeCommitId() != null) {
            String[] parents = commit.getBeforeCommitId().split(",");
            userPrompt.append("### PREVIOUS COMMIT(S) CONTEXT\n");
            for (String parentSha : parents) {
                commitRepository.findById_CommitShaAndRepositoryId(parentSha, commit.getRepository().getId())
                        .ifPresent(parent -> {
                            userPrompt.append("- SHA: ").append(parentSha).append("\n");
                            userPrompt.append("  Message: ").append(parent.getMessage()).append("\n");
                            userPrompt.append("  Changed Files: ").append(extractFilesFromDiff(parent.getDiff()))
                                    .append("\n");
                        });
            }
            userPrompt.append("\n");
        }

        userPrompt.append("### CURRENT COMMIT TO ANALYZE\n");
        userPrompt.append("Message: ").append(commit.getMessage()).append("\n\n");
        userPrompt.append("Diff Context:\n").append(commit.getDiff());

        return userPrompt.toString();
    }

    private Long normalizeAndSave(String metric, Long rawScore) {
        String baseKey = SCORE_STATS_KEY_PREFIX + metric;

        // Atomic update of running stats in Redis
        // count, sum, sum_sq
        redisTemplate.opsForValue().increment(baseKey + ":count");
        redisTemplate.opsForValue().increment(baseKey + ":sum", rawScore);
        redisTemplate.opsForValue().increment(baseKey + ":sum_sq", rawScore * rawScore);

        Object countObj = redisTemplate.opsForValue().get(baseKey + ":count");
        Object sumObj = redisTemplate.opsForValue().get(baseKey + ":sum");
        Object sumSqObj = redisTemplate.opsForValue().get(baseKey + ":sum_sq");

        if (countObj == null || sumObj == null || sumSqObj == null)
            return rawScore;

        long n = Long.parseLong(countObj.toString());
        double sum = Double.parseDouble(sumObj.toString());
        double sumSq = Double.parseDouble(sumSqObj.toString());

        if (n < 2)
            return rawScore;

        double mean = sum / n;
        double variance = (sumSq - (sum * sum / n)) / (n - 1);
        double stdDev = Math.sqrt(Math.max(0.1, variance)); // Avoid div by zero

        double zScore = (rawScore - mean) / stdDev;

        // Map Z-Score to 0-100 range (Z=0 -> 50, Z=2 -> 90, Z=-2 -> 10, etc.)
        double normalized = 50.0 + (zScore * 20.0);
        return Math.max(0L, Math.min(100L, Math.round(normalized)));
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
