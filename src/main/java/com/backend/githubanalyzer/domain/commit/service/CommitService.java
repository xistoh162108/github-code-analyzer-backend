package com.backend.githubanalyzer.domain.commit.service;

import com.backend.githubanalyzer.domain.commit.dto.CommitAnalysisResponse;
import com.backend.githubanalyzer.domain.commit.dto.CommitResponse;
import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommitService {

    private final CommitRepository commitRepository;
    private final com.backend.githubanalyzer.domain.user.repository.UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CommitResponse> getUserRecentCommits(String email) {
        com.backend.githubanalyzer.domain.user.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return commitRepository.findAllByAuthorIdOrderByCommittedAtDesc(user.getId()).stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommitResponse> getUserCommitsInRepo(Long userId, String repoId) {
        return commitRepository.findAllByAuthorIdAndRepositoryIdOrderByCommittedAtDesc(userId, repoId).stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommitResponse> getCommits(String repoId) {
        return commitRepository.findAllByRepositoryIdOrderByCommittedAtDesc(repoId).stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CommitAnalysisResponse> getAnalysisBySha(String sha) {
        return commitRepository.findAllById_CommitSha(sha).stream()
                .findFirst()
                .map(this::toAnalysisResponse);
    }

    @Transactional(readOnly = true)
    public CommitAnalysisResponse getAnalysis(String repoId, String sha) {
        return commitRepository.findAllById_CommitShaAndRepositoryId(sha, repoId).stream()
                .findFirst()
                .map(this::toAnalysisResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AnalysisStatus getStatus(String repoId, String sha) {
        return commitRepository.findAllById_CommitShaAndRepositoryId(sha, repoId).stream()
                .findFirst()
                .map(Commit::getAnalysisStatus)
                .orElse(null);
    }

    private CommitResponse toCommitResponse(Commit commit) {
        return CommitResponse.builder()
                .sha(commit.getId().getCommitSha())
                .message(commit.getMessage())
                .committedAt(commit.getCommittedAt())
                .authorName(commit.getAuthor().getUsername())
                .authorProfileUrl(commit.getAuthor().getProfileUrl())
                .analysisStatus(commit.getAnalysisStatus())
                .totalScore(commit.getTotalScore())
                .build();
    }

    private CommitAnalysisResponse toAnalysisResponse(Commit commit) {
        return CommitAnalysisResponse.builder()
                .sha(commit.getId().getCommitSha())
                .message(commit.getMessage())
                .committedAt(commit.getCommittedAt())
                .authorName(commit.getAuthor().getUsername())
                .analysisStatus(commit.getAnalysisStatus())
                .totalScore(commit.getTotalScore())
                .commitMessageQuality(commit.getCommitMessageQuality())
                .codeQuality(commit.getCodeQuality())
                .changeAppropriateness(commit.getChangeAppropriateness())
                .necessity(commit.getNecessity())
                .correctnessAndRisk(commit.getCorrectnessAndRisk())
                .testingAndVerification(commit.getTestingAndVerification())
                .messageNotes(commit.getMessageNotes())
                .codeQualityNotes(commit.getCodeQualityNotes())
                .necessityNotes(commit.getNecessityNotes())
                .correctnessRiskNotes(commit.getCorrectnessRiskNotes())
                .testingNotes(commit.getTestingNotes())
                .summary(commit.getSummary())
                .strengths(commit.getStrengths())
                .issues(commit.getIssues())
                .suggestedNextCommit(commit.getSuggestedNextCommit())
                .riskLevel(commit.getRiskLevel())
                .analysisReason(commit.getAnalysisReason())
                .build();
    }
}
