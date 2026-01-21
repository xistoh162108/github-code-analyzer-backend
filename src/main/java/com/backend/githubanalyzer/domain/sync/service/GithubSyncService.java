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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubSyncService {

    private final GithubApiService githubApiService;
    private final GithubPersistenceService githubPersistenceService;
    private final com.backend.githubanalyzer.domain.user.service.UserService userService;
    private final GithubAppService githubAppService;
    private final com.backend.githubanalyzer.domain.sync.queue.CommitSyncQueueProducer syncQueueProducer;
    private final com.backend.githubanalyzer.domain.commit.repository.CommitRepository commitRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

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

                    User repoOwner = userService.getOrCreateGhostUser(
                            String.valueOf(repoDto.getOwner().getId()),
                            repoDto.getOwner().getLogin(),
                            repoDto.getOwner().getAvatarUrl());
                    GithubRepository repository = githubPersistenceService.saveRepository(repoOwner, repoDto);

                    // --- Incremental Sync V2: Skip repo if not pushed recently ---
                    if (repository.getLastSyncAt() != null && repoDto.getPushedAt() != null) {
                        if (!repoDto.getPushedAt().isAfter(repository.getLastSyncAt())) {
                            log.info("Skipping repository {}/{} - No new pushes since last sync ({})",
                                    repoDto.getOwner().getLogin(), repoDto.getName(), repository.getLastSyncAt());
                            continue;
                        }
                    }

                    repository.setSyncStatus("RUNNING");
                    githubPersistenceService.saveContribution(user, repository, ContributionType.COLLABORATOR);

                    syncCommitsForRepo(repoDto.getOwner().getLogin(), repository, repoOwner, accessToken);

                    repository.setSyncStatus("COMPLETED");

                    // Update lastSyncAt to the latest push time if available
                    if (repoDto.getPushedAt() != null) {
                        repository.setLastSyncAt(repoDto.getPushedAt());
                    } else {
                        repository.setLastSyncAt(LocalDateTime.now());
                    }

                    githubPersistenceService.save(repository);
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

    public com.backend.githubanalyzer.domain.repository.dto.SyncStatusResponse syncRepo(String repoId) {
        GithubRepository repository = githubPersistenceService.findById(repoId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repoId);
        }

        // Trigger Async Process
        internalSyncRepo(repoId);

        return new com.backend.githubanalyzer.domain.repository.dto.SyncStatusResponse(
                "QUEUED",
                repoId,
                LocalDateTime.now());
    }

    @Async
    protected void internalSyncRepo(String repoId) {
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
            String sha = commitDto.getSha();
            if (commitRepository.existsById_CommitSha(sha)) {
                continue;
            }

            try {
                com.backend.githubanalyzer.domain.sync.queue.CommitSyncJobRequest syncJob = com.backend.githubanalyzer.domain.sync.queue.CommitSyncJobRequest
                        .builder()
                        .owner(owner)
                        .repoName(repository.getReponame())
                        .sha(sha)
                        .branchName(branchName)
                        .userId(repositoryOwner.getId())
                        .repositoryId(repository.getId())
                        .accessToken(accessToken)
                        .build();
                syncQueueProducer.pushJob(syncJob);
            } catch (Exception e) {
                log.error("Failed to queue sync job for commit {}: {}", sha, e.getMessage());
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

        // --- BATCH CONTEXT START ---
        String batchId = java.util.UUID.randomUUID().toString();
        // Set Total to -1 (Pending) so AnalysisService doesn't trigger early
        redisTemplate.opsForValue().set("analysis:batch:" + batchId + ":total", -1);
        redisTemplate.opsForValue().set("analysis:batch:" + batchId + ":processed", 0);
        redisTemplate.opsForValue().set("analysis:batch:" + batchId + ":success", 0);
        redisTemplate.opsForValue().set("analysis:batch:" + batchId + ":score_sum", 0);
        // Track unique commits found in this sync to avoid double counting across branches
        java.util.Set<String> thisBatchCommits = new java.util.HashSet<>();
        // ---------------------------

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
                String sha = commitDto.getSha();
                if (commitRepository.existsById_CommitSha(sha)) {
                    continue;
                }
                
                // Avoid queuing same commit multiple times in same batch (if multiple branches have same new commit)
                if (thisBatchCommits.contains(sha)) {
                    continue;
                }
                thisBatchCommits.add(sha);

                try {
                    com.backend.githubanalyzer.domain.sync.queue.CommitSyncJobRequest syncJob = com.backend.githubanalyzer.domain.sync.queue.CommitSyncJobRequest
                            .builder()
                            .owner(owner)
                            .repoName(repository.getReponame())
                            .sha(sha)
                            .branchName(branch.getName())
                            .userId(repositoryOwner.getId())
                            .repositoryId(repository.getId())
                            .accessToken(accessToken)
                            .batchId(batchId)
                            .build();
                    syncQueueProducer.pushJob(syncJob);
                } catch (Exception e) {
                    log.error("Failed to queue sync job for commit {}: {}", sha, e.getMessage());
                }
            }
        }
        
        // --- BATCH CONTEXT END ---
        int totalQueued = thisBatchCommits.size();
        redisTemplate.opsForValue().set("analysis:batch:" + batchId + ":total", totalQueued);
        log.info("Batch {} created with {} commits queued.", batchId, totalQueued);
        
        // If 0 queued, we might want to clean up or just leave it (it will expire if we set TTL, but for now manual cleanup logic in AnalysisService handles done batches)
        // If jobs finished FAST, we need to check if we should trigger summary now
        if (totalQueued > 0) {
             Object processedObj = redisTemplate.opsForValue().get("analysis:batch:" + batchId + ":processed");
             if (processedObj != null && Integer.parseInt(processedObj.toString()) == totalQueued) {
                 // Trigger summary technically handled by AnalysisService, but if it raced, AnalysisService saw -1
                 // Implementation detail: AnalysisService can check again or we trigger it?
                 // Simpler: Let AnalysisService polling or re-check happen? 
                 // Actually the easiest is: AnalysisService checks processed==total. If match, it sends.
                 // If processing finished when total=-1, it didn't send.
                 // So WE must check here.
                 // Ideally we'd call a method in AnalysisService to 'tryCompleteBatch(batchId)'
                 // But for now, let's leave as is, and ensure AnalysisService handles the verification robustly
                 // Or we can publish a "BatchReady" event. 
                 // For MVP: We will simply rely on the fact that AnalysisService checks this. 
                 // BUT to be safe, if we implement the check in AnalysisService, we need to ensure it runs AFTER total is set.
                 // We can simply call:
                 // analysisService.checkBatchCompletion(batchId); if we had access.
                 // Unreachable cross-domain call currently without injecting Service (cyclic dependency risk if SyncService depends on AnalysisService).
             }
        } else {
             // Cleanup empty batch
             redisTemplate.delete("analysis:batch:" + batchId + ":total");
             redisTemplate.delete("analysis:batch:" + batchId + ":processed");
             redisTemplate.delete("analysis:batch:" + batchId + ":success");
             redisTemplate.delete("analysis:batch:" + batchId + ":score_sum");
        }
    }

    // --- Branch API Implementation ---

    public List<com.backend.githubanalyzer.domain.repository.dto.RepositoryBranchResponse> getRepositoryBranches(String repoId) {
        GithubRepository repository = githubPersistenceService.findById(repoId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repoId);
        }

        // Fetch distinct branches from DB
        List<String> branches = commitRepository.findDistinctBranchNamesByRepositoryId(repoId);
        log.info("Found {} branches in DB for repo {}", branches.size(), repository.getReponame());

        return branches.stream()
                .map(branchName -> {
                    long count = commitRepository.countById_RepoIdAndId_BranchName(repoId, branchName);
                    com.backend.githubanalyzer.domain.commit.entity.Commit latestCommit = 
                            commitRepository.findFirstById_RepoIdAndId_BranchNameOrderByCommittedAtDesc(repoId, branchName);
                    
                    return com.backend.githubanalyzer.domain.repository.dto.RepositoryBranchResponse.builder()
                            .name(branchName)
                            .lastCommitSha(latestCommit != null ? latestCommit.getId().getCommitSha() : null)
                            .commitCount(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse> getCommitsByBranch(String repoId, String branchName) {
        GithubRepository repository = githubPersistenceService.findById(repoId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repoId);
        }

        // Fetch commits directly from DB, sorted by latest first
        log.info("Fetching commits from DB for repo {} branch {}", repoId, branchName);
        List<com.backend.githubanalyzer.domain.commit.entity.Commit> commits = 
                commitRepository.findAllByRepositoryIdAndId_BranchNameOrderByCommittedAtDesc(repoId, branchName);

        if (commits.isEmpty()) {
            // Optional: If DB is empty, maybe trigger a sync? 
            // For now, trusting the user's intent to rely on DB.
            return java.util.Collections.emptyList();
        }

        return commits.stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());
    }

    private com.backend.githubanalyzer.domain.commit.dto.CommitResponse toCommitResponse(com.backend.githubanalyzer.domain.commit.entity.Commit commit) {
        return com.backend.githubanalyzer.domain.commit.dto.CommitResponse.builder()
                .sha(commit.getId().getCommitSha())
                .message(commit.getMessage())
                .committedAt(commit.getCommittedAt())
                .authorName(commit.getAuthor().getUsername())
                .authorProfileUrl(commit.getAuthor().getProfileUrl())
                .analysisStatus(commit.getAnalysisStatus())
                .totalScore(commit.getTotalScore())
                .build();
    }

    private String getAccessTokenForUser(User user) {
        // Try to get installation token first as it is more reliable for repo ops
        String installationId = user.getInstallationId();
        if (installationId != null) {
            try {
                return githubAppService.getInstallationToken(installationId);
            } catch (Exception e) {
                log.warn("Failed to get installation token for user {}", user.getUsername());
            }
        }
        // Fallback or if no installation ID (though usually needed for meaningful sync)
        return null; // TODO: handle user personal token if needed, but for now rely on App
    }
}
