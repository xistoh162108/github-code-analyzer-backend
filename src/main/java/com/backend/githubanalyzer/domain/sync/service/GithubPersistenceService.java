package com.backend.githubanalyzer.domain.sync.service;

import com.backend.githubanalyzer.domain.repository.dto.ContributorResponse;
import com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse;
import com.backend.githubanalyzer.domain.repository.dto.RepositoryMetricResponse;
import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.entity.CommitId;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.contribution.entity.Contribution;
import com.backend.githubanalyzer.domain.contribution.entity.ContributionId;
import com.backend.githubanalyzer.domain.contribution.entity.ContributionType;
import com.backend.githubanalyzer.domain.contribution.repository.ContributionRepository;
import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.domain.team.service.TeamService;
import com.backend.githubanalyzer.infra.github.dto.GithubCommitResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse;
import com.backend.githubanalyzer.domain.analysis.dto.AnalysisJobRequest;
import com.backend.githubanalyzer.domain.analysis.queue.AnalysisQueueProducer;
import com.backend.githubanalyzer.domain.analysis.service.ScoreAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubPersistenceService {

    private final GithubRepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final ContributionRepository contributionRepository;
    private final UserService userService;
    private final TeamService teamService;
    private final AnalysisQueueProducer analysisQueueProducer;
    private final ScoreAggregationService scoreAggregationService;

    @Transactional(readOnly = true)
    public GithubRepository findById(String id) {
        return repositoryRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public GithubRepositoryResponse getRepositoryResponse(String id) {
        return repositoryRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ... (Constructor injection handled by Lombok, but ensure field is declared)

    // Note: I will insert the field with other fields

    private GithubRepositoryResponse toDto(GithubRepository repository) {
        java.util.Map<String, Long> languagesMap = null;
        try {
            if (repository.getLanguages() != null) {
                languagesMap = objectMapper.readValue(repository.getLanguages(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Long>>() {
                        });
            }
        } catch (Exception e) {
            log.warn("Failed to parse languages JSON for repo: {}", repository.getReponame());
        }

        return GithubRepositoryResponse.builder()
                .id(repository.getId())
                .reponame(repository.getReponame())
                .repoUrl(repository.getRepoUrl())
                .description(repository.getDescription())
                .language(repository.getLanguage())
                .languages(languagesMap) // Added
                .size(repository.getSize())
                .stars(repository.getStars())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .pushedAt(repository.getPushedAt())
                .lastSyncAt(repository.getLastSyncAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RepositoryMetricResponse getRepositoryMetrics(String repoId) {
        GithubRepository repo = repositoryRepository.findById(repoId).orElse(null);
        if (repo == null) {
            return null;
        }

        // Calculate Real Average Score from Analysis
        Long sumScore = commitRepository.sumCompletedScoreByRepositoryId(repoId);
        long countAnalyzed = commitRepository.countCompletedByRepositoryId(repoId);
        
        Double averageScore = 0.0;
        if (countAnalyzed > 0 && sumScore != null) {
            averageScore = (double) sumScore / countAnalyzed;
        }

        return RepositoryMetricResponse.builder()
                .commitCount(repo.getCommitCount())
                .averageScore(Math.round(averageScore * 10.0) / 10.0) // Round to 1 decimal
                .totalScore(repo.getScore()) // Activity Score
                .build();
    }

    @Transactional(readOnly = true)
    public List<ContributorResponse> getContributors(String repoId) {
        List<Contribution> contributions = contributionRepository.findAllByRepositoryIdOrderByRankAsc(repoId);
        return contributions.stream()
                .map(c -> ContributorResponse.builder()
                        .username(c.getUser().getUsername())
                        .profileUrl(c.getUser().getProfileUrl())
                        .role(c.getContributionType().name())
                        .rank(c.getRank())
                        .score(c.getUser().getScore())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public GithubRepository saveRepository(User user, GithubRepoResponse dto) {
        GithubRepository repository = repositoryRepository.findById(dto.getId()).orElse(
                GithubRepository.builder().id(dto.getId()).build());

        repository.setReponame(dto.getName());
        repository.setRepoUrl(dto.getHtmlUrl());
        repository.setDescription(dto.getDescription());
        repository.setLanguage(dto.getLanguage());
        repository.setSize(dto.getSize());
        repository.setStars(dto.getStars());
        repository.setCreatedAt(dto.getCreatedAt());
        repository.setUpdatedAt(dto.getUpdatedAt());
        repository.setPushedAt(dto.getPushedAt());
        repository.setOwner(user);

        GithubRepository saved = repositoryRepository.save(repository);
        saveContribution(user, saved, ContributionType.OWNER);
        return saved;
    }

    @Transactional
    public void saveContribution(User user, GithubRepository repository, ContributionType type) {
        ContributionId contributionId = new ContributionId(user.getId(), repository.getId());
        Contribution contribution = contributionRepository.findById(contributionId).orElse(null);

        if (contribution == null) {
            contribution = Contribution.builder()
                    .id(contributionId)
                    .user(user)
                    .repository(repository)
                    .contributionType(type)
                    .build();
            contributionRepository.save(contribution);
        } else if (contribution.getContributionType() != ContributionType.OWNER) {
            contribution.setContributionType(type);
            contributionRepository.save(contribution);
        }
        teamService.handleContributorAdded(user, repository);
    }

    @Transactional
    public void saveCommit(GithubRepository repository, User repositoryOwner, String branchName,
            GithubCommitResponse detailedDto, String batchId) {
        CommitId commitId = new CommitId(detailedDto.getSha(), repository.getId(), branchName);
        Commit existingCommit = commitRepository.findById(commitId).orElse(null);

        User author;
        if (detailedDto.getAuthor() != null) {
            author = userService.getOrCreateGhostUser(
                    String.valueOf(detailedDto.getAuthor().getId()),
                    detailedDto.getAuthor().getLogin(),
                    detailedDto.getAuthor().getAvatarUrl());
        } else {
            author = repositoryOwner;
        }

        StringBuilder diffBuilder = new StringBuilder();
        if (detailedDto.getFiles() != null) {
            for (GithubCommitResponse.GithubFileResponse file : detailedDto.getFiles()) {
                if (file.getPatch() != null) {
                    diffBuilder.append("--- ").append(file.getFilename()).append("\n");
                    diffBuilder.append(file.getPatch()).append("\n\n");
                }
            }
        }

        if (existingCommit == null) {
            String parentsStr = null;
            if (detailedDto.getParents() != null && !detailedDto.getParents().isEmpty()) {
                parentsStr = String.join(",", detailedDto.getParents().stream()
                        .map(GithubCommitResponse.ParentInfo::getSha)
                        .toList());
            }

            Commit commit = Commit.builder()
                    .id(commitId)
                    .repository(repository)
                    .author(author)
                    .message(detailedDto.getCommit().getMessage())
                    .diff(diffBuilder.toString())
                    .beforeCommitId(parentsStr) // Populate beforeCommitId
                    .committedAt(detailedDto.getCommit().getAuthor().getDate())
                    .build();
            commitRepository.save(commit);

            // Push to AI Analysis Queue
            analysisQueueProducer.pushJob(AnalysisJobRequest.builder()
                    .commitSha(commit.getId().getCommitSha())
                    .repositoryId(repository.getId())
                    .batchId(batchId)
                    .build());
        } else {
            existingCommit.setDiff(diffBuilder.toString());
            existingCommit.setAuthor(author);
            // Update parents if needed (usually SHA is immutable, but for completeness)
            if (detailedDto.getParents() != null && !detailedDto.getParents().isEmpty()) {
                String parentsStr = String.join(",", detailedDto.getParents().stream()
                        .map(GithubCommitResponse.ParentInfo::getSha)
                        .toList());
                existingCommit.setBeforeCommitId(parentsStr);
            }

            // Re-trigger analysis if score is 0 AND summary is missing (implies incomplete
            // analysis)
            // OR if status is FAILED (Retry logic)
            if ((existingCommit.getTotalScore() == 0L && existingCommit.getSummary() == null)
                    || existingCommit.getAnalysisStatus() == AnalysisStatus.FAILED) {
                log.info("Re-queueing existing commit {} for analysis (Missing Analysis Data)",
                        existingCommit.getId().getCommitSha());
                analysisQueueProducer.pushJob(AnalysisJobRequest.builder()
                        .commitSha(existingCommit.getId().getCommitSha())
                        .repositoryId(repository.getId())
                        .build());
            }

            commitRepository.save(existingCommit);
        }

        // Lazy Aggregation (Performance Boost)
        scoreAggregationService.markUserDirty(author.getId());
        scoreAggregationService.markTeamDirty(repository.getId());

        saveContribution(author, repository, ContributionType.COMMITTER);
    }

    @Transactional
    public void refreshRepoStats(GithubRepository repository) {
        long commitCount = commitRepository.countUniqueCommitsByRepositoryId(repository.getId());
        long branchCount = commitRepository.countUniqueBranchesByRepositoryId(repository.getId());

        repository.setCommitCount(commitCount);
        repository.setBranchNum(branchCount);

        // Simple Score calculation logic
        long score = (commitCount * 10L) + (branchCount * 20L) + (repository.getStars() * 50L);
        repository.setScore(score);

        repositoryRepository.save(repository);
        log.info("Refreshed stats for repo {}: {} branches, {} commits, score: {}",
                repository.getReponame(), branchCount, commitCount, score);
    }

    @Transactional
    public void deleteRepository(String repositoryId) {
        GithubRepository repo = repositoryRepository.findById(repositoryId).orElse(null);
        if (repo != null) {
            log.info("Deleting repository: {}/{}", repo.getOwner().getUsername(), repo.getReponame());
            // Due to cascade or manual cleanup
            repositoryRepository.delete(repo);
        }
    }

    @Transactional
    public GithubRepository save(GithubRepository repository) {
        return repositoryRepository.save(repository);
    }
}
