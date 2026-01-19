package com.backend.githubanalyzer.domain.team.service;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.team.entity.*;
import com.backend.githubanalyzer.domain.team.repository.TeamHasRepoRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRepository;
import com.backend.githubanalyzer.domain.team.repository.UserRegisterTeamRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRegisterTeamRepository userRegisterTeamRepository;
    private final TeamHasRepoRepository teamHasRepoRepository;

    @Transactional
    public void addMemberToTeam(Team team, User user, String role) {
        UserRegisterTeamId membershipId = new UserRegisterTeamId(team.getId(), user.getId());
        UserRegisterTeam membership = userRegisterTeamRepository.findById(membershipId).orElse(null);

        if (membership == null) {
            membership = UserRegisterTeam.builder()
                    .id(membershipId)
                    .team(team)
                    .user(user)
                    .role(role)
                    .status("APPROVED") // Auto-added from contribution is pre-approved
                    .build();
            userRegisterTeamRepository.save(membership);
            log.info("Automatically added user {} to team {} as {}", user.getUsername(), team.getName(), role);
        } else {
            // Role Upgrade Logic: MEMBER -> CONTRIBUTOR
            if ("MEMBER".equals(membership.getRole()) && "CONTRIBUTOR".equals(role)) {
                membership.setRole("CONTRIBUTOR");
                membership.setStatus("APPROVED"); // Ensure they are approved if they contribute
                userRegisterTeamRepository.save(membership);
                log.info("Upgraded user {} in team {} to CONTRIBUTOR", user.getUsername(), team.getName());
            }
        }
    }

    @Transactional
    public void applyToJoinTeam(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found for ID: " + teamId));

        UserRegisterTeamId membershipId = new UserRegisterTeamId(teamId, user.getId());
        if (userRegisterTeamRepository.existsById(membershipId)) {
            throw new IllegalStateException("Already a member or has a pending request.");
        }

        UserRegisterTeam request = UserRegisterTeam.builder()
                .id(membershipId)
                .team(team)
                .user(user)
                .role("MEMBER")
                .status("PENDING")
                .build();
        userRegisterTeamRepository.save(request);
        log.info("User {} applied to join team {}", user.getUsername(), team.getName());
    }

    @Transactional(readOnly = true)
    public List<User> listPendingRequests(String teamId, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new AccessDeniedException("Only the Team Leader can view pending requests.");
        }

        return userRegisterTeamRepository.findByTeamIdAndStatus(teamId, "PENDING").stream()
                .map(UserRegisterTeam::getUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveMember(String teamId, Long userId, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new AccessDeniedException("Only the Team Leader can approve members.");
        }

        UserRegisterTeamId membershipId = new UserRegisterTeamId(teamId, userId);
        UserRegisterTeam membership = userRegisterTeamRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("No join request found for this user."));

        membership.setStatus("APPROVED");
        userRegisterTeamRepository.save(membership);
        log.info("Leader {} approved user {} into team {}", leader.getUsername(), userId, team.getName());
    }

    @Transactional
    public void removeMember(String teamId, Long userId, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new AccessDeniedException("Only the Team Leader can remove members.");
        }

        UserRegisterTeamId membershipId = new UserRegisterTeamId(teamId, userId);
        UserRegisterTeam membership = userRegisterTeamRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("User is not in the team."));

        // Constraint: Cannot remove contributors
        if ("CONTRIBUTOR".equals(membership.getRole())) {
            throw new IllegalStateException(
                    "Cannot remove a member who is a direct contributor to the team's repositories.");
        }

        userRegisterTeamRepository.delete(membership);
        log.info("Leader {} removed user {} from team {}", leader.getUsername(), userId, team.getName());
    }

    @Transactional
    public void addRepoToTeam(Team team, GithubRepository repo) {
        // Constraint 2: Repository owner must be the leader or a member of the team
        boolean isOwnerLeader = repo.getOwner().getId().equals(team.getLeader().getId());
        boolean isOwnerMember = userRegisterTeamRepository
                .existsById(new UserRegisterTeamId(team.getId(), repo.getOwner().getId()));

        if (!isOwnerLeader && !isOwnerMember) {
            throw new IllegalArgumentException(
                    "Constraint Violation: Only repositories owned by Team Leader or Members can be added to the team.");
        }

        TeamHasRepoId mappingId = new TeamHasRepoId(team.getId(), repo.getId());
        if (!teamHasRepoRepository.existsById(mappingId)) {
            TeamHasRepo mapping = TeamHasRepo.builder()
                    .id(mappingId)
                    .team(team)
                    .repository(repo)
                    .build();
            teamHasRepoRepository.save(mapping);
            log.info("Added repository {} to team {}", repo.getReponame(), team.getName());
        }
    }

    @Transactional
    public void handleContributorAdded(User contributor, GithubRepository repo) {
        // Constraint 1: Contributor must be in Team
        List<TeamHasRepo> mappings = teamHasRepoRepository.findByRepositoryId(repo.getId());
        for (TeamHasRepo mapping : mappings) {
            Team team = mapping.getTeam();
            addMemberToTeam(team, contributor, "CONTRIBUTOR");
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserInTeam(String teamId, Long userId) {
        return userRegisterTeamRepository.existsById(new UserRegisterTeamId(teamId, userId));
    }

    public void validateTeamIdAccess(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        boolean isLeader = team.getLeader().getId().equals(user.getId());
        boolean isMember = isUserInTeam(teamId, user.getId());

        if (!isLeader && !isMember) {
            throw new AccessDeniedException("You do not have permission to view this Team ID.");
        }
    }
}
