package com.backend.githubanalyzer.domain.sprint.service;

import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import com.backend.githubanalyzer.domain.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintTeamRankingResponse> getSprintRankings(
            String sprintId) {
        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findAllBySprintId(sprintId);

        // Sort by Score Descending
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
        java.time.LocalDateTime start = sprint.getStartDate();
        java.time.LocalDateTime end = sprint.getEndDate();

        java.util.List<com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint> registrations = teamRegisterSprintRepository
                .findAllBySprintId(sprintId);

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

        // Sort by Score Descending
        allStats.sort((a, b) -> Long.compare(b.score, a.score));

        java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintIndividualRankingResponse> response = new java.util.ArrayList<>();
        long rank = 1;
        for (ParticipantStats p : allStats) {
            response.add(new com.backend.githubanalyzer.domain.sprint.dto.SprintIndividualRankingResponse(
                    rank++,
                    p.user.getUsername(),
                    p.user.getUsername(), // Using username for both fields for now, or use githubId/login if available
                    p.user.getProfileUrl(),
                    p.score,
                    p.commits));
        }
        return response;
    }

    @Transactional
    public String createSprint(com.backend.githubanalyzer.domain.sprint.dto.SprintCreateRequest request) {
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
        return sprintId;
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

    public java.util.List<com.backend.githubanalyzer.domain.sprint.dto.SprintResponse> getPublicSprints() {
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
    public void banTeam(String sprintId, String teamId, com.backend.githubanalyzer.domain.user.entity.User manager) {
        Sprint sprint = getSprint(sprintId);

        if (!sprint.getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Only the Sprint Manager can ban teams.");
        }

        if (isTeamBanned(sprintId, teamId)) {
            return;
        }

        com.backend.githubanalyzer.domain.team.entity.Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        com.backend.githubanalyzer.domain.sprint.entity.SprintBannedTeam ban = com.backend.githubanalyzer.domain.sprint.entity.SprintBannedTeam
                .builder()
                .sprint(sprint)
                .team(team)
                .build();

        bannedTeamRepository.save(ban);
    }

    public void validateSprintIdAccess(String sprintId, com.backend.githubanalyzer.domain.user.entity.User user) {
        Sprint sprint = getSprint(sprintId);

        // Manager always has access
        if (sprint.getManager().getId().equals(user.getId())) {
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
}
