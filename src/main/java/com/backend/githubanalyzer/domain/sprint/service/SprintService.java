package com.backend.githubanalyzer.domain.sprint.service;

import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import com.backend.githubanalyzer.domain.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SprintService {

    private final SprintRepository sprintRepository;
    private final com.backend.githubanalyzer.domain.sprint.repository.SprintBannedTeamRepository bannedTeamRepository;
    private final com.backend.githubanalyzer.domain.team.repository.TeamRepository teamRepository;
    private final com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository teamRegisterSprintRepository;
    private final com.backend.githubanalyzer.domain.commit.repository.CommitRepository commitRepository;
    private final com.backend.githubanalyzer.domain.team.service.TeamService teamService;
    private final com.backend.githubanalyzer.domain.user.repository.UserRepository userRepository;
    private final com.backend.githubanalyzer.domain.team.repository.UserRegisterTeamRepository userRegisterTeamRepository;
    private final com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository githubRepositoryRepository;

    private final com.backend.githubanalyzer.global.webhook.WebhookService webhookService;
    private final com.backend.githubanalyzer.domain.team.repository.TeamHasRepoRepository teamHasRepoRepository;
    // Injecting TeamHasRepoId is not needed as we can instantiate it or use findBy match

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintTeamRankingResponse> getSprintRankings(
            String sprintId) {
        Sprint sprint = getSprint(sprintId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // Req 11: Pre-start opacity - If before start, return empty unless manager
        // (assumed check at controller or here)
        // For public API, if before start, maybe return empty list?
        if (now.isBefore(sprint.getStartDate())) {
            // For simplicity, return empty list if not started, unless caller handles
            // permission.
            // But since this is public ranking, we hide it.
            return new java.util.ArrayList<>();
        }

        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findAllBySprintId(sprintId);

        // Filter: Only APPROVED teams should be in ranking
        registrations = registrations.stream()
                .filter(reg -> "APPROVED".equals(reg.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Req 9: Time-bounded scoring
        java.time.LocalDateTime effectiveEnd = now.isAfter(sprint.getEndDate()) ? sprint.getEndDate() : now;

        // Recalculate or use stored score? Req implies we need to calculate based on
        // repo score.
        // For performance, we might want to use stored `reg.getScore()`, but we need to
        // ensure it's up to date.
        // Let's assume `reg.getScore()` is updated via SyncService or we calculate on
        // fly here for accuracy.
        // Given implementations: let's use the stored score for now, assuming
        // SyncService updates `TeamRegisterSprint.score`.

        // Correct approach based on Req 9: "Score of Repo".
        // If we want real-time accuracy:
        for (com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg : registrations) {
            // Logic: Update score from Repository entity or calculation
            if (reg.getRepository() != null) {
                // For now, mapping Repository Score to Sprint Score directly.
                // Ideally this should be "Score gained DURING Sprint".
                // But detailed commit analysis is complex here.
                // Using Repository Total Score as proxy based on current architecture,
                // OR if `commitRepository` has `sumScore...` between dates.
                Long score = commitRepository.sumTotalScoreByRepo(reg.getRepository().getId(), sprint.getStartDate(),
                        effectiveEnd);
                reg.setScore(score != null ? score : 0L);
                // Note: We are not saving here to avoid write overhead on read, but for ranking
                // we use calculated value.
            }
        }

        registrations.sort((a, b) -> Long.compare(b.getScore(), a.getScore()));

        java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintTeamRankingResponse> rankings = new java.util.ArrayList<>();
        long rank = 1;
        for (com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg : registrations) {
            long memberCount = userRegisterTeamRepository.countByTeamId(reg.getTeam().getId());
            rankings.add(new com.backend.githubanalyzer.domain.sprint.dto.SprintTeamRankingResponse(
                    rank++,
                    reg.getTeam().getName(),
                    reg.getScore(),
                    reg.getCommitNum(),
                    memberCount));
        }
        return rankings;
    }

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintIndividualRankingResponse> getSprintIndividualRankings(
            String sprintId) {
        Sprint sprint = getSprint(sprintId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if (now.isBefore(sprint.getStartDate())) {
            return new java.util.ArrayList<>();
        }

        java.time.LocalDateTime end = now.isAfter(sprint.getEndDate()) ? sprint.getEndDate() : now;
        java.time.LocalDateTime start = sprint.getStartDate();

        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findAllBySprintId(sprintId);

        registrations = registrations.stream()
                .filter(reg -> "APPROVED".equals(reg.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // List to hold all participants' dynamic stats
        class ParticipantStats {
            com.backend.githubanalyzer.domain.user.entity.User user;
            long score;
            long commits;

            ParticipantStats(com.backend.githubanalyzer.domain.user.entity.User user, long score, long commits) {
                this.user = user;
                this.score = score;
                this.commits = commits;
            }
        }
        java.util.List<ParticipantStats> allStats = new java.util.ArrayList<>();

        for (com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg : registrations) {
            String repoId = reg.getRepository().getId();
            java.util.List<com.backend.githubanalyzer.domain.team.entity.UserRegisterTeam> members = userRegisterTeamRepository
                    .findByTeamId(reg.getTeam().getId());

            for (com.backend.githubanalyzer.domain.team.entity.UserRegisterTeam member : members) {
                long commits = commitRepository.countByRepoAndTimeAndAuthor(repoId, member.getUser().getId(), start,
                        end);
                Long scoreObj = commitRepository.sumTotalScoreByRepoAndTimeAndAuthor(repoId, member.getUser().getId(),
                        start, end);
                long score = (scoreObj != null) ? scoreObj : 0L;
                allStats.add(new ParticipantStats(member.getUser(), score, commits));
            }
        }

        allStats.sort((a, b) -> Long.compare(b.score, a.score));

        java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintIndividualRankingResponse> response = new java.util.ArrayList<>();
        long rank = 1;
        for (ParticipantStats p : allStats) {
            response.add(new com.backend.githubanalyzer.domain.sprint.dto.SprintIndividualRankingResponse(
                    rank++,
                    p.user.getUsername(),
                    p.user.getUsername(),
                    p.user.getProfileUrl(),
                    p.score,
                    p.commits));
        }
        return response;
    }

    @Transactional
    public com.backend.githubanalyzer.domain.sprint.dto.SprintResponse createSprint(
            com.backend.githubanalyzer.domain.sprint.dto.SprintCreateRequest request) {
        com.backend.githubanalyzer.domain.user.entity.User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        String sprintId = java.util.UUID.randomUUID().toString();

        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .description(request.description())
                .isPrivate(request.isPrivate())
                .isOpen(request.isOpen())
                .manager(manager)
                .build();

        sprintRepository.save(sprint);
        return com.backend.githubanalyzer.domain.sprint.dto.SprintResponse.from(sprint, 0L, 0L, "upcoming");
    }

    // Req 1 & 4 & 10: Update Sprint
    @Transactional
    public com.backend.githubanalyzer.domain.sprint.dto.SprintResponse updateSprint(String sprintId,
            com.backend.githubanalyzer.domain.sprint.dto.SprintCreateRequest request,
            Long userId) {
        Sprint sprint = getSprint(sprintId);

        // Req 1: Manager Only
        if (!sprint.getManager().getId().equals(userId)) {
            throw new AccessDeniedException("Only Sprint Manager can update the sprint.");
        }

        // Req 4: Name uneditable (Ignore request.name or throw if different? Let's just
        // not update it)

        // Req 3: Private -> Public OK, Public -> Private NO
        if (!sprint.getIsPrivate() && request.isPrivate()) {
            throw new IllegalArgumentException("Cannot change Public Sprint to Private.");
        }
        sprint.setIsPrivate(request.isPrivate());

        // Req 10: Date adjustments
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // If End Date passed, cannot change? Req says "Before End Date, End Date
        // adjustable".
        // Implicitly if passed, maybe locked. But code says "current > when
        // manipulating".
        // If current < endDate, we can change endDate.
        if (now.isBefore(sprint.getEndDate())) {
            sprint.setEndDate(request.endDate());
        }

        if (now.isBefore(sprint.getStartDate())) {
            sprint.setStartDate(request.startDate());
        }

        sprint.setDescription(request.description());
        sprint.setIsOpen(request.isOpen());

        sprintRepository.save(sprint);
        return com.backend.githubanalyzer.domain.sprint.dto.SprintResponse.from(sprint, 0L, 0L,
                determineStatus(sprint));
    }

    public Sprint getSprint(String sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + sprintId));
    }

    public void validateSprintOpen(String sprintId) {
        Sprint sprint = getSprint(sprintId);
        if (!sprint.getIsOpen()) {
            throw new IllegalStateException("Sprint is not open for registration: " + sprint.getName());
        }
    }

    // Register Team (Req 1, 5, 6, 12, Leader Constraint, 1 Repo Constraint)
    @Transactional
    public com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse registerTeamToSprint(String sprintId,
            String teamId, String repoId, Long userId) {
        Sprint sprint = getSprint(sprintId);

        // Req 5: Must be Open
        if (!sprint.getIsOpen()) {
            throw new IllegalStateException("Sprint is closed for registration.");
        }

        com.backend.githubanalyzer.domain.team.entity.Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        // Req: Leader Only
        if (!team.getLeader().getId().equals(userId)) {
            throw new AccessDeniedException("Only Team Leader can register the team.");
        }

        // Req: 1 Repo per Team (UniqueConstraint handles implementation, but business
        // logic check good)
        if (teamRegisterSprintRepository.existsBySprintIdAndTeamId(sprintId, teamId)) {
            throw new IllegalStateException("Team is already registered for this sprint.");
        }

        // If Private, check logic? Req 6 says "Enter Participation Code (Sprint ID)".
        // Since API call includes sprintId, we assume user knows it.

        String status = "APPROVED";
        if (sprint.getIsPrivate()) {
            status = "PENDING"; // Req 12: Manager approval needed
        }

        com.backend.githubanalyzer.domain.repository.entity.GithubRepository repo = githubRepositoryRepository
                .findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));

        // Constraint: Repo must be in TeamHasRepo
        if (!teamHasRepoRepository.existsById(new com.backend.githubanalyzer.domain.team.entity.TeamHasRepoId(teamId, repoId))) {
            throw new IllegalStateException("Constraint Violation: The repository must be registered in the Team before joining a Sprint.");
        }

        com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint registration = com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint
                .builder()
                .id(new com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprintId(sprintId, teamId, repoId))
                .sprint(sprint)
                .team(team)
                .repository(repo)
                .status(status)
                .build();

        teamRegisterSprintRepository.save(registration);

        // For Public Sprints (Auto-Approved), trigger Webhook immediately
        if ("APPROVED".equals(status)) {
            com.backend.githubanalyzer.global.webhook.WebhookService.WebhookResult result = webhookService
                    .sendTeamRepoUrl(team.getName(), repo.getRepoUrl());
            return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(
                    registration,
                    result.url(),
                    result.success(),
                    result.errorMessage());
        }

        return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(registration);
    }

    // Req 12: Approve/Reject
    @Transactional
    public com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse approveTeamRegistration(
            String sprintId, String teamId, Long managerId, boolean approve) {
        Sprint sprint = getSprint(sprintId);
        if (!sprint.getManager().getId().equals(managerId)) {
            throw new AccessDeniedException("Only Manager can approve/reject.");
        }

        com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg = teamRegisterSprintRepository
                .findBySprintIdAndTeamId(sprintId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));

        if (approve) {
            reg.setStatus("APPROVED");
            // Trigger Webhook
            try {
                if (reg.getTeam() != null && reg.getRepository() != null) {
                    com.backend.githubanalyzer.global.webhook.WebhookService.WebhookResult result = webhookService
                            .sendTeamRepoUrl(reg.getTeam().getName(), reg.getRepository().getRepoUrl());

                    return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(
                            reg,
                            result.url(),
                            result.success(),
                            result.errorMessage());
                }
            } catch (Exception e) {
                log.error("Failed to trigger webhook during sprint registration approval: {}", e.getMessage());
                return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(reg, null, false,
                        e.getMessage());
            }
        } else {
            // Rejected -> Delete registration
            teamRegisterSprintRepository.delete(reg);
            reg.setStatus("REJECTED"); // Mark for response
        }

        return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(reg);
    }

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintResponse> getPublicSprints() {
        // Req 8: Private not searchable
        java.util.List<Sprint> sprints = sprintRepository.findByIsPrivateFalse();
        return sprints.stream().map(sprint -> {
            long teamsCount = teamRegisterSprintRepository.countBySprintId(sprint.getId());
            long participantsCount = teamRegisterSprintRepository.countParticipantsBySprintId(sprint.getId());
            String status = determineStatus(sprint);
            return com.backend.githubanalyzer.domain.sprint.dto.SprintResponse.from(sprint, teamsCount,
                    participantsCount, status);
        }).collect(java.util.stream.Collectors.toList());
    }

    private String determineStatus(Sprint sprint) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isAfter(sprint.getEndDate())) {
            return "completed";
        } else if (sprint.getIsOpen() && now.isAfter(sprint.getStartDate()) && now.isBefore(sprint.getEndDate())) {
            return "active";
        } else {
            return "upcoming";
        }
    }

    @Transactional
    public void openSprint(String sprintId) {
        Sprint sprint = getSprint(sprintId);
        sprint.setIsOpen(true);
    }

    @Transactional
    public void closeSprint(String sprintId) {
        Sprint sprint = getSprint(sprintId);
        sprint.setIsOpen(false);
    }

    public boolean isTeamBanned(String sprintId, String teamId) {
        return bannedTeamRepository.existsBySprintIdAndTeamId(sprintId, teamId);
    }

    @Transactional
    public com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse banTeam(String sprintId,
            String teamId, com.backend.githubanalyzer.domain.user.entity.User manager) {
        Sprint sprint = getSprint(sprintId);

        if (!sprint.getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Only the Sprint Manager can ban teams.");
        }

        if (isTeamBanned(sprintId, teamId)) {
            // Already banned, return current state (which should be banned or deleted?)
            // Logic below creates ban record. If already banned, maybe just return found
            // reg?
            // Or throw? Existing code returns void if banned.
            // Let's fetch registration to return.
            return teamRegisterSprintRepository.findBySprintIdAndTeamId(sprintId, teamId)
                    .map(com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse::from)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Registration not found (even though ban check passed?)"));
        }

        com.backend.githubanalyzer.domain.team.entity.Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        com.backend.githubanalyzer.domain.sprint.entity.SprintBannedTeam ban = com.backend.githubanalyzer.domain.sprint.entity.SprintBannedTeam
                .builder()
                .sprint(sprint)
                .team(team)
                .build();

        bannedTeamRepository.save(ban);

        // Update Status to BANNED
        com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg = teamRegisterSprintRepository
                .findBySprintIdAndTeamId(sprintId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));

        reg.setStatus("BANNED");

        return com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse.from(reg);
    }

    public void validateSprintIdAccess(String sprintId, com.backend.githubanalyzer.domain.user.entity.User user) {
        Sprint sprint = getSprint(sprintId);

        // Manager always has access
        if (sprint.getManager().getId().equals(user.getId())) {
            return;
        }

        // Req: If not private, anyone can view? Req 8 says "Sprint Private -> Search
        // Impossible".
        // Implicitly Public is Viewable.
        if (!sprint.getIsPrivate()) {
            return;
        }

        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> sprintTeams = teamRegisterSprintRepository
                .findAll().stream()
                .filter(reg -> reg.getSprint().getId().equals(sprintId))
                .collect(java.util.stream.Collectors.toList());

        for (com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint reg : sprintTeams) {
            if (teamService.isUserInTeam(reg.getTeam().getId(), user.getId()) ||
                    reg.getTeam().getLeader().getId().equals(user.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("You do not have permission to view this Sprint ID.");
    }

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintResponse> getMyParticipatingSprints(
            Long userId) {
        // 1. Participated via Team
        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findSprintsByUserId(userId);

        java.util.Set<Sprint> uniqueSprints = registrations.stream()
                .map(com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint::getSprint)
                .collect(java.util.stream.Collectors.toSet());

        // 2. Managed Sprints
        java.util.List<Sprint> managedSprints = sprintRepository.findByManagerId(userId);
        uniqueSprints.addAll(managedSprints);

        return uniqueSprints.stream()
                .map(sprint -> {
                    long teamsCount = teamRegisterSprintRepository.countBySprintId(sprint.getId());
                    long participantsCount = teamRegisterSprintRepository.countParticipantsBySprintId(sprint.getId());
                    String status = determineStatus(sprint);
                    return com.backend.githubanalyzer.domain.sprint.dto.SprintResponse.from(sprint, teamsCount,
                            participantsCount, status);
                })
                .sorted((s1, s2) -> s2.startDate().compareTo(s1.startDate())) // Sort by recent
                .collect(java.util.stream.Collectors.toList());
    }

    public com.backend.githubanalyzer.domain.sprint.dto.SprintInfoResponse getSprintInfo(String sprintId) {
        Sprint sprint = getSprint(sprintId);
        return com.backend.githubanalyzer.domain.sprint.dto.SprintInfoResponse.from(sprint, determineStatus(sprint));
    }
}
