package com.backend.githubanalyzer.domain.sync.service;

import com.backend.githubanalyzer.domain.contribution.entity.ContributionType;
import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.infra.github.GithubApiService;
import com.backend.githubanalyzer.infra.github.GithubApiService.GithubResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubBranchResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubCommitResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse;
import com.backend.githubanalyzer.infra.github.service.GithubAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubSyncService {

    private final GithubApiService githubApiService;
    private final GithubPersistenceService githubPersistenceService;
    private final com.backend.githubanalyzer.domain.user.service.UserService userService;
    private final GithubAppService githubAppService;

    public User findUserByGithubId(String githubId) {
        return userService.findByGithubId(githubId);
    }

    @Async
    public void syncAllData(Long userId, String accessToken) {
        User user = userService.findById(userId);
        if (user == null) {
            log.warn("User not found for sync: {}", userId);
            return;
        }
        String syncUsername = user.getUsername();
        log.info("Starting background sync for user: {} (Database ID: {}, GitHub ID: {})",
                syncUsername, user.getId(), user.getGithubId());
        try {
            GithubResponse<GithubRepoResponse> repoResponse = githubApiService
                    .fetchUserRepositories(accessToken, user.getReposEtag()).block();

            if (repoResponse == null)
                return;

            if (repoResponse.notModified()) {
                log.info("No changes in repository list for user: {} (304 Not Modified)", syncUsername);
                // Even if repo list didn't change, we might want to check individual repos
                // BUT for now let's assume if 304, we don't need to do anything for repo list
            } else {
                user.setReposEtag(repoResponse.etag());
                userService.save(user); // Updated to save ETag
            }

            List<GithubRepoResponse> repos = repoResponse.data();
            if (repos == null || repos.isEmpty()) {
                log.info("No repositories found or modified for user: {}", user.getUsername());
                // In case of 304, data is empty, but we might want to continue sync for
                // existing repos if needed.
                // For simplicity, let's only sync when repo list changes OR if we decide to
                // crawl all.
                // Better: If 304, fetch existing repos from DB and check them?
                // Let's stick to syncing modified ones for now.
                if (repoResponse.notModified()) {
                    // TODO: Decide if we want to deep-sync even if repo list is 304
                } else {
                    return;
                }
            }
            log.info("Total repositories found via GitHub API: {}. Repositories: {}",
                    repos.size(),
                    repos.stream().map(GithubRepoResponse::getName).toList());

            for (int i = 0; i < repos.size(); i++) {
                GithubRepoResponse repoDto = repos.get(i);
                try {
                    log.info("[Sync Progress: {}/{}] Processing repository: {}/{}",
                            (i + 1), repos.size(), repoDto.getOwner().getLogin(), repoDto.getName());

                    // Determine actual owner of the repo using numeric ID
                    User repoOwner = userService.getOrCreateGhostUser(
                            String.valueOf(repoDto.getOwner().getId()),
                            repoDto.getOwner().getLogin(),
                            repoDto.getOwner().getAvatarUrl());
                    GithubRepository repository = githubPersistenceService.saveRepository(repoOwner, repoDto);
                    repository.setSyncStatus("RUNNING");
                    githubPersistenceService.saveContribution(user, repository, ContributionType.COLLABORATOR);

                    syncCommitsForRepo(repoDto.getOwner().getLogin(), repository, repoOwner, accessToken);

                    repository.setSyncStatus("COMPLETED");
                    repository.setLastSyncAt(LocalDateTime.now());
                    githubPersistenceService.refreshRepoStats(repository);
                } catch (Exception e) {
                    log.error("Failed to sync repository {}/{} : {}",
                            repoDto.getOwner().getLogin(), repoDto.getName(), e.getMessage());
                }
            }
            log.info("Successfully completed sync for user: {}", syncUsername);
        } catch (Exception e) {
            log.error("Failed to sync data for userId: {}", userId, e);
        }
    }

    @Async
    public void syncRepo(String repoId) {
        GithubRepository repository = githubPersistenceService.findById(repoId);
        if (repository == null || repository.getOwner() == null) {
            log.warn("Repository or owner not found for sync: {}", repoId);
            return;
        }

        User owner = repository.getOwner();
        String installationId = owner.getInstallationId();

        if (installationId == null) {
            log.warn("No installation ID for owner {} of repo {}", owner.getUsername(), repoId);
            return;
        }

        try {
            String token = githubAppService.getInstallationToken(installationId);
            // Fetch single repo details or just sync commits
            // For now, let's just sync commits since repo metadata changes less often
            syncCommitsForRepo(owner.getUsername(), repository, owner, token);
            repository.setLastSyncAt(LocalDateTime.now());
            githubPersistenceService.save(repository);
            githubPersistenceService.refreshRepoStats(repository);
            log.info("Successfully synced repo: {}", repoId);
        } catch (Exception e) {
            log.error("Failed to sync repo {}: {}", repoId, e.getMessage());
        }
    }

    @Async("githubSyncExecutor")
    public void syncSelective(String repositoryId, String branchName, String accessToken) {
        GithubRepository repository = githubPersistenceService.findById(repositoryId);
        if (repository == null) {
            log.warn("Repository not found for selective sync: {}", repositoryId);
            return;
        }

        User owner = repository.getOwner();
        log.info("Starting selective sync for repo: {}/{} branch: {}",
                owner.getUsername(), repository.getReponame(), branchName);

        try {
            repository.setSyncStatus("RUNNING");
            syncCommitsForBranch(owner.getUsername(), repository, owner, branchName, accessToken);
            repository.setSyncStatus("COMPLETED");
            repository.setLastSyncAt(LocalDateTime.now());
            githubPersistenceService.refreshRepoStats(repository);
        } catch (Exception e) {
            log.error("Selective sync failed for repo {}: {}", repository.getId(), e.getMessage());
            repository.setSyncStatus("FAILED");
        }
    }

    @Async("githubSyncExecutor")
    public void syncSingleRepo(User user, GithubRepoResponse repoDto, String accessToken) {
        try {
            User repoOwner = userService.getOrCreateGhostUser(
                    String.valueOf(repoDto.getOwner().getId()),
                    repoDto.getOwner().getLogin(),
                    repoDto.getOwner().getAvatarUrl());
            GithubRepository repository = githubPersistenceService.saveRepository(repoOwner, repoDto);
            repository.setSyncStatus("RUNNING");
            githubPersistenceService.saveContribution(user, repository, ContributionType.COLLABORATOR);

            syncCommitsForRepo(repoDto.getOwner().getLogin(), repository, repoOwner, accessToken);

            repository.setSyncStatus("COMPLETED");
            repository.setLastSyncAt(LocalDateTime.now());
            githubPersistenceService.refreshRepoStats(repository);

            // Team Service Integration (From dev branch) - DISABLED for Production Safety
            /*
             * com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest teamRequest =
             * new com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest(
             * repository.getReponame(),
             * repository.getDescription(),
             * user.getId() // 또는 repository.getOwner().getId()
             * );
             * 
             * String teamId = teamService.createTeam(teamRequest);
             * teamService.addRepoToTeam(teamId, repository);
             */
        } catch (Exception e) {
            log.error("Single repo sync failed: {}", e.getMessage());
        }
    }

    private void syncCommitsForBranch(String owner, GithubRepository repository, User repositoryOwner,
            String branchName, String accessToken) {
        log.info("Fetching commits for branch: {} in repo: {}", branchName, repository.getReponame());

        // Use 'since' for incremental commit sync from the last known sync time
        GithubResponse<GithubCommitResponse> commitResponse = githubApiService
                .fetchCommits(owner, repository.getReponame(), branchName, accessToken, repository.getLastSyncAt(),
                        null)
                .block();

        if (commitResponse == null || commitResponse.data() == null)
            return;

        List<GithubCommitResponse> commits = commitResponse.data();
        log.info("Found {} new commits for branch: {} in repo: {}", commits.size(), branchName,
                repository.getReponame());

        for (GithubCommitResponse commitDto : commits) {
            GithubCommitResponse detailedDto = githubApiService.fetchCommitDetail(
                    owner,
                    repository.getReponame(),
                    commitDto.getSha(),
                    accessToken).block();

            if (detailedDto != null) {
                githubPersistenceService.saveCommit(repository, repositoryOwner, branchName, detailedDto);
            }
        }
    }

    private void syncCommitsForRepo(String owner, GithubRepository repository, User repositoryOwner,
            String accessToken) {

        // --- Added: Fetch and Save Languages ---
        try {
            java.util.Map<String, Long> languages = githubApiService
                    .fetchRepositoryLanguages(owner, repository.getReponame(), accessToken).block();
            if (languages != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                repository.setLanguages(mapper.writeValueAsString(languages));
                githubPersistenceService.save(repository); // Intermediate save
            }
        } catch (Exception e) {
            log.warn("Failed to fetch languages for repo: {}", repository.getReponame(), e);
        }
        // ---------------------------------------

        String repoName = repository.getReponame();

        log.info("Fetching branches for repo: {}", repoName);
        GithubResponse<GithubBranchResponse> branchResponse = githubApiService
                .fetchBranches(owner, repoName, accessToken, repository.getBranchesEtag()).block();

        if (branchResponse == null)
            return;

        if (!branchResponse.notModified()) {
            repository.setBranchesEtag(branchResponse.etag());
            // Persistence of repository etag happens later in refreshRepoStats or similar
        }

        List<GithubBranchResponse> branches = branchResponse.data();
        if (branchResponse.notModified()) {
            log.info("No changes in branches for repo: {} (304 Not Modified)", repoName);
            return;
        }

        for (GithubBranchResponse branch : branches) {
            log.info("Fetching commits for branch: {} in repo: {}", branch.getName(), repoName);
            // Use 'since' for incremental commit sync
            GithubResponse<GithubCommitResponse> commitResponse = githubApiService
                    .fetchCommits(owner, repoName, branch.getName(), accessToken, repository.getLastSyncAt(), null)
                    .block();

            if (commitResponse == null || commitResponse.data() == null)
                continue;

            List<GithubCommitResponse> commits = commitResponse.data();
            log.info("Found {} new commits for branch: {} in repo: {}", commits.size(), branch.getName(), repoName);
            for (GithubCommitResponse commitDto : commits) {
                // Determine if detailed fetch is needed (Phase 2 will optimize this)
                log.info("Fetching commit details (diff) for SHA: {}", commitDto.getSha());
                GithubCommitResponse detailedDto = githubApiService.fetchCommitDetail(
                        owner,
                        repository.getReponame(),
                        commitDto.getSha(),
                        accessToken).block();

                if (detailedDto != null) {
                    githubPersistenceService.saveCommit(repository, repositoryOwner, branch.getName(), detailedDto);
                }
            }
        }
    }
}
