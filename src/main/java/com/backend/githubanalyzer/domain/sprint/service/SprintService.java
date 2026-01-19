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
    private final com.backend.githubanalyzer.domain.team.service.TeamService teamService;

    public java.util.List<Sprint> getPublicSprints() {
        return sprintRepository.findByIsPrivateFalse();
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
