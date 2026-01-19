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

import com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest;
import com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRegisterTeamRepository userRegisterTeamRepository;
    private final TeamHasRepoRepository teamHasRepoRepository;
    private final UserRepository userRepository;
    private final com.backend.githubanalyzer.domain.commit.repository.CommitRepository commitRepository;

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

            List<User> contributors = commitRepository.findDistinctAuthorByRepositoryId(repo.getId());
            for (User contributor : contributors) {
                // 이미 멤버인지 확인하는 로직은 addMemberToTeam 내부 검사나 호출 전 검사로 처리 가능하지만
                // addMemberToTeam이 안전하게 처리하도록 구현되어 있는지 확인 후 호출
                // 여기서는 간단히 호출 (Service 내부 로직에 맡김 - addMemberToTeam 검토 필요)
                // 만약 addMemberToTeam이 중복 체크를 안한다면 여기서 exists 체크를 해야함.

                // 현재 addMemberToTeam은 role upgrade 로직만 있고 중복 insert 방지가 약할 수 있으므로
                // 안전하게 존재 여부 확인 후 호출
                if (!isUserInTeam(team.getId(), contributor.getId())) {
                    addMemberToTeam(team, contributor, "CONTRIBUTOR");
                }
            }
        }
    }

    @Transactional
    public void addRepoToTeam(String teamId, GithubRepository repo) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found for ID: " + teamId));
        addRepoToTeam(team, repo);
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

    // 1. 팀 생성
    public String createTeam(TeamCreateRequest request) {
        User leader = userRepository.findById(request.leaderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String teamId = UUID.randomUUID().toString();

        Team team = Team.builder()
                .id(teamId) // String ID
                .name(request.name())
                .description(request.description())
                .leader(leader)
                .build();

        teamRepository.save(team);

        // 리더를 팀 멤버로 추가 (LEADER 권한)
        UserRegisterTeamId registerId = new UserRegisterTeamId(teamId, leader.getId());
        UserRegisterTeam member = UserRegisterTeam.builder()
                .id(registerId)
                .user(leader)
                .team(team)
                .role("LEADER")
                .status("APPROVED")
                .inTeamRank(0L)
                .build();

        userRegisterTeamRepository.save(member);

        return teamId;
    }

    // 2. 팀 멤버 조회 (DTO 반환)
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(String teamId) {
        // 1. Get all Repo IDs linked to this Team
        List<String> teamRepoIds = teamHasRepoRepository.findByTeamId(teamId).stream()
                .map(mapping -> mapping.getRepository().getId())
                .collect(Collectors.toList());

        // 2. Fetch all members
        List<UserRegisterTeam> members = userRegisterTeamRepository.findByTeamId(teamId);

        // 3. Calculate stats for each member
        // Temporary DTO to hold data for sorting
        class MemberStats {
            UserRegisterTeam member;
            long commitCount;
            long score;

            MemberStats(UserRegisterTeam member, long commitCount, long score) {
                this.member = member;
                this.commitCount = commitCount;
                this.score = score;
            }
        }

        List<MemberStats> statsList = members.stream().map(member -> {
            long commitCount = 0;
            long score = 0;
            if (!teamRepoIds.isEmpty()) {
                commitCount = commitRepository.countByRepositoryIdInAndAuthorId(teamRepoIds, member.getUser().getId());
                Long scoreObj = commitRepository.sumTotalScoreByRepositoryIdInAndAuthorId(teamRepoIds,
                        member.getUser().getId());
                score = (scoreObj != null) ? scoreObj : 0L;
            }
            return new MemberStats(member, commitCount, score);
        }).collect(Collectors.toList());

        // 4. Sort by Commit Count Descending (Contributor Rank)
        statsList.sort((a, b) -> Long.compare(b.commitCount, a.commitCount));

        // 5. Assign Rank and Convert to Response DTO
        List<TeamMemberResponse> response = new java.util.ArrayList<>();
        long rank = 1;
        for (MemberStats s : statsList) {
            response.add(new TeamMemberResponse(
                    s.member.getUser().getId(),
                    s.member.getUser().getUsername(),
                    s.member.getRole(),
                    s.member.getStatus(),
                    rank++, // Dynamic Rank based on current stats
                    s.commitCount,
                    s.score));
        }

        return response;
    }
}
