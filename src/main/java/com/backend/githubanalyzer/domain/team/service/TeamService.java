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

import com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRegisterTeamRepository userRegisterTeamRepository;
    private final TeamHasRepoRepository teamHasRepoRepository;
    private final UserRepository userRepository;
    private final com.backend.githubanalyzer.domain.commit.repository.CommitRepository commitRepository;
    private final com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository teamRegisterSprintRepository;

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
    public com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse createTeam(
            com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest request) {
        User leader = userRepository.findById(request.leaderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String teamId = java.util.UUID.randomUUID().toString();

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

        userRegisterTeamRepository.save(member);

        return com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse.from(team);
    }

    // 4. Team Update (Name & Description) - Leader Only
    @Transactional
    public com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse updateTeam(String teamId,
            com.backend.githubanalyzer.domain.team.dto.TeamUpdateRequest request,
            User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only Team Leader can update team details.");
        }

        team.setName(request.name());
        team.setDescription(request.description());
        // team.setIsPublic(request.isPublic()); // If we want to allow public switch

        teamRepository.save(team);
        return com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse.from(team);
    }

    // 5. Team Details (Visibility Logic)
    @Transactional(readOnly = true)
    public com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse getTeamDetails(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        boolean isMember = isUserInTeam(teamId, user.getId());
        boolean isLeader = team.getLeader().getId().equals(user.getId());

        // Sprint Manager check? (Req 9)
        // Need to check if this team is in any sprint managed by 'user'
        // Complex query: Find Sprints where (Manager=user AND TeamRegistered in Sprint)
        // Or simplified: Pass a flag if caller is manager context.
        // For general API, we'll check:
        // isPublic OR isMember OR isLeader -> Full Details

        // Check if user is manager of ANY sprint this team is in
        boolean isSprintManager = teamRegisterSprintRepository.findAllByTeamId(teamId).stream()
                .anyMatch(reg -> reg.getSprint().getManager().getId().equals(user.getId()));

        if (team.getIsPublic() || isMember || isLeader || isSprintManager) {
            return com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse.from(team); // Full
        } else {
            // Limited View (Req 8)
            return com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse.limited(team);
        }
    }

    @Transactional
    public com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse joinTeam(String teamId, User user,
            String code) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (isUserInTeam(teamId, user.getId())) {
            throw new IllegalStateException("Already a member or has a pending request.");
        }

        String status = "APPROVED";
        if (!team.getIsPublic()) {
            // Private Team: Check Code
            if (!team.getId().equals(code)) {
                throw new AccessDeniedException("Invalid Team Code.");
            }
            status = "PENDING";
        }

        UserRegisterTeam request = UserRegisterTeam.builder()
                .id(new UserRegisterTeamId(teamId, user.getId()))
                .team(team)
                .user(user)
                .role("MEMBER")
                .status(status)
                .inTeamRank(0L)
                .build();
        UserRegisterTeam saved = userRegisterTeamRepository.save(request);

        return new com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse(
                saved.getUser().getId(),
                saved.getUser().getUsername(),
                saved.getRole(),
                saved.getStatus(),
                0L, 0L, 0L);
    }

    @Transactional
    public com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse approveMember(String teamId, Long userId,
            User leader) {
        // Logic same as existing, just ensuring status transition
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new AccessDeniedException("Only the Team Leader can approve members.");
        }

        UserRegisterTeam membership = userRegisterTeamRepository.findById(new UserRegisterTeamId(teamId, userId))
                .orElseThrow(() -> new IllegalArgumentException("No join request found."));

        membership.setStatus("APPROVED");
        UserRegisterTeam saved = userRegisterTeamRepository.save(membership);

        return new com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse(
                saved.getUser().getId(),
                saved.getUser().getUsername(),
                saved.getRole(),
                saved.getStatus(),
                saved.getInTeamRank(),
                0L, 0L // Stats might not be instant, or need recalc. sending 0 is safe for immediate
                       // ACK
        );
    }

    @Transactional
    public void removeMember(String teamId, Long userId, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new AccessDeniedException("Only the Team Leader can remove members.");
        }

        // Constraint 10: Cannot remove if committed
        List<String> repoIds = teamHasRepoRepository.findByTeamId(teamId).stream()
                .map(m -> m.getRepository().getId())
                .collect(Collectors.toList());

        if (!repoIds.isEmpty() && commitRepository.existsByAuthorIdAndRepositoryIdIn(userId, repoIds)) {
            throw new IllegalStateException("Cannot remove a member who has contributed commits to team repositories.");
        }

        UserRegisterTeam membership = userRegisterTeamRepository.findById(new UserRegisterTeamId(teamId, userId))
                .orElseThrow(() -> new IllegalArgumentException("User is not in the team."));

        userRegisterTeamRepository.delete(membership);
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

    // 3. 내 팀 조회
    @Transactional(readOnly = true)
    public List<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse> getMyTeams(Long userId) {
        List<UserRegisterTeam> memberships = userRegisterTeamRepository.findByUserId(userId);

        return memberships.stream()
                .map(membership -> {
                    Team team = membership.getTeam();
                    // Simplified view or full view? User is member, so likely full view is okay.
                    // But let's reuse TeamDetailResponse.from(team)
                    return com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse.from(team);
                })
                .collect(Collectors.toList());
    }
}
