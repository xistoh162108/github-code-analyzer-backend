package com.backend.githubanalyzer.domain.sync.service;

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
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository;
import com.backend.githubanalyzer.domain.team.service.SprintRegistrationService;
import com.backend.githubanalyzer.infra.github.dto.GithubCommitResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse;
import com.backend.githubanalyzer.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubPersistenceService {

    private final GithubRepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final ContributionRepository contributionRepository;
    private final UserService userService;
    private final TeamService teamService;
    private final TeamRegisterSprintRepository teamRegisterSprintRepository;
    private final SprintRegistrationService sprintRegistrationService;
    private final AnalysisService analysisService;

    @Transactional(readOnly = true)
    public GithubRepository findById(String id) {
        return repositoryRepository.findById(id).orElse(null);
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
            GithubCommitResponse detailedDto) {
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
            Commit commit = Commit.builder()
                    .id(commitId)
                    .repository(repository)
                    .author(author)
                    .message(detailedDto.getCommit().getMessage())
                    .diff(diffBuilder.toString())
                    .committedAt(detailedDto.getCommit().getAuthor().getDate())
                    .build();
            commitRepository.save(commit);
            analysisService.analyzeCommitAsync(commit);
        } else {
            existingCommit.setDiff(diffBuilder.toString());
            existingCommit.setAuthor(author);
            commitRepository.save(existingCommit);
        }
        userService.refreshUserStats(author);

        // Refresh Participation Stats for all sprints involving this repo
        java.util.List<TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findByRepositoryId(repository.getId());
        for (TeamRegisterSprint reg : registrations) {
            sprintRegistrationService.refreshTeamSprintStats(reg);
        }

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
}
