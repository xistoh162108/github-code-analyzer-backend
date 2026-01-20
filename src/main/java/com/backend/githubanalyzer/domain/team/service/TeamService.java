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
    private final com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository githubRepositoryRepository;
    private final com.backend.githubanalyzer.domain.notification.service.NotificationService notificationService;

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
    public void addRepoToTeam(String teamId, String repoId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found for ID: " + teamId));
        
        if (!team.getLeader().getId().equals(user.getId())) {
             throw new AccessDeniedException("Only the Team Leader can add repositories to the team.");
        }

        GithubRepository repo = githubRepositoryRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found for ID: " + repoId));
        
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
    @Transactional
    public com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse createTeam(
            com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest request) {
        User leader = userRepository.findById(request.leaderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String teamId = java.util.UUID.randomUUID().toString();

        Team team = Team.builder()
                .id(teamId)
                .name(request.name())
                .description(request.description())
                .leader(leader)
                .isPublic(request.isPublic() != null ? request.isPublic() : true)
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
        if (request.isPublic() != null) {
            team.setIsPublic(request.isPublic());
        }

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
    public com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse joinTeam(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (team.getLeader().getId().equals(user.getId())) {
            throw new IllegalStateException("Team Leader cannot join their own team as a regular member.");
        }

        if (isUserInTeam(teamId, user.getId())) {
            throw new IllegalStateException("Already a member or has a pending request.");
        }

        String status = "APPROVED";
        if (!team.getIsPublic()) {
            // Private Team: teamId acts as the join code (already provided in path)
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

        // Send Notification
        notificationService.send(saved.getUser().getId(),
                com.backend.githubanalyzer.domain.notification.entity.NotificationType.TEAM_JOIN_APPROVED,
                "Your request to join team '" + team.getName() + "' has been approved.");

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

        // Send Notification
        notificationService.send(userId,
                com.backend.githubanalyzer.domain.notification.entity.NotificationType.TEAM_BAN,
                "You have been removed from team '" + team.getName() + "'.");
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

    // New: 팀장이 관리하는 팀 조회
    @Transactional(readOnly = true)
    public List<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse> getTeamsLeaderOf(User user) {
        List<Team> teams = teamRepository.findAllByLeaderId(user.getId());
        return teams.stream()
                .map(com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse::from)
                .collect(Collectors.toList());
    }

    // New: 팀에 추가 가능한 레포지토리 목록 조회 (Leader Only)
    @Transactional(readOnly = true)
    public List<com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse> getAvailableReposForTeam(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        if (!team.getLeader().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the Team Leader can view available repositories to add.");
        }

        // 1. Get Repos owned by Leader
        List<GithubRepository> leaderRepos = githubRepositoryRepository.findAllByOwnerId(user.getId());

        // 2. Get Repos owned by Members
        List<UserRegisterTeam> members = userRegisterTeamRepository.findByTeamId(teamId);
        List<Long> memberIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());
        
        // This could be optimized with a custom query findAllByOwnerIdIn(List<Long> ids)
        // For now, we iterate or assume list isn't huge. Better: use stream to collect all.
        // Or adding findAllByOwnerIdIn to repo. Let's stick to simple loop properly or add method later if performance issue.
        // Actually, let's just fetch all member repos in one go if possible, or loop. With modest team size, loop is fine??
        // A better approach: 
        List<GithubRepository> memberRepos = new java.util.ArrayList<>();
        for(Long mid : memberIds) {
             memberRepos.addAll(githubRepositoryRepository.findAllByOwnerId(mid));
        }

        // 3. Combine
        java.util.Set<GithubRepository> allCandidates = new java.util.HashSet<>();
        allCandidates.addAll(leaderRepos);
        allCandidates.addAll(memberRepos);

        // 4. Filter out already added repos
        List<String> existingRepoIds = teamHasRepoRepository.findByTeamId(teamId).stream()
                .map(m -> m.getRepository().getId())
                .collect(Collectors.toList());
        
        return allCandidates.stream()
                .filter(r -> !existingRepoIds.contains(r.getId()))
                .map(this::toRepoDto)
                .collect(Collectors.toList());
    }

    // New: 팀 레포지토리 조회
    @Transactional(readOnly = true)
    public List<com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse> getTeamRepos(String teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        boolean isMember = isUserInTeam(teamId, user.getId());
        boolean isLeader = team.getLeader().getId().equals(user.getId());

        if (!team.getIsPublic() && !isMember && !isLeader) {
             throw new AccessDeniedException("You do not have permission to view repositories of this private team.");
        }

        List<TeamHasRepo> mappings = teamHasRepoRepository.findByTeamId(teamId);
        return mappings.stream()
                .map(m -> toRepoDto(m.getRepository()))
                .collect(Collectors.toList());
    }

    private com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse toRepoDto(GithubRepository repository) {
        return com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse.builder()
                .id(repository.getId())
                .reponame(repository.getReponame())
                .repoUrl(repository.getRepoUrl())
                .description(repository.getDescription())
                .language(repository.getLanguage())
                .size(repository.getSize())
                .stars(repository.getStars())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .pushedAt(repository.getPushedAt())
                .lastSyncAt(repository.getLastSyncAt())
                .build();
    }

    @Transactional(readOnly = true)
    public com.backend.githubanalyzer.global.dto.PageResponse<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse> getPublicTeamsPaging(org.springframework.data.domain.Pageable pageable) {
        // Sort check: user wants createdAt DESC
        org.springframework.data.domain.Page<Team> page = teamRepository.findByIsPublicTrue(pageable);
        
        List<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse> content = page.getContent().stream()
                .map(com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse::from)
                .collect(Collectors.toList());
        
        return com.backend.githubanalyzer.global.dto.PageResponse.of(content, page.hasNext());
    }
}
