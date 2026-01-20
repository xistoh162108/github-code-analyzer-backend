package com.backend.githubanalyzer.domain.team.controller;

import com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest;
import com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse;
import com.backend.githubanalyzer.domain.team.service.TeamService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Team", description = "팀 생성, 관리 및 멤버십 API")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    // 1. 팀 생성
    @io.swagger.v3.oas.annotations.Operation(summary = "Create Team (팀 생성)", description = "새로운 팀을 생성하고 생성자를 Leader로 지정합니다.")
    @PostMapping
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse>> createTeam(
            @RequestBody TeamCreateRequest request) {
        com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse response = teamService.createTeam(request);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    // 2. 팀 수정
    @io.swagger.v3.oas.annotations.Operation(summary = "Update Team (팀 정보 수정)", description = "팀 이름, 설명, 공개 여부 등을 수정합니다. (Leader Only)")
    @PutMapping("/{teamId}")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse>> updateTeam(
            @PathVariable String teamId,
            @RequestBody com.backend.githubanalyzer.domain.team.dto.TeamUpdateRequest request) {
        User currentUser = getCurrentUser();
        com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse response = teamService.updateTeam(teamId, request,
                currentUser);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    // 3. 팀 상세 조회 (Constraints Applied)
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Team Details (팀 상세 조회)", description = "팀의 상세 정보를 조회합니다. 비공개 팀의 경우 멤버만 조회 가능할 수 있습니다.")
    @GetMapping("/{teamId}")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse>> getTeamDetails(
            @PathVariable String teamId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(
                teamService.getTeamDetails(teamId, currentUser)));
    }

    // 4. 팀 멤버 조회
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Team Members (팀 멤버 조회)", description = "팀에 소속된 멤버 목록을 조회합니다.")
    @GetMapping("/{teamId}/members")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<TeamMemberResponse>>> getTeamMembers(
            @PathVariable String teamId) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(teamId);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(members));
    }

    // New: 내 팀 조회
    @io.swagger.v3.oas.annotations.Operation(summary = "Get My Teams (내 팀 조회)", description = "내가 속한 팀 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse>>> getMyTeams() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(
                teamService.getMyTeams(currentUser.getId())));
    }

    // 5. 팀 가입 신청 (Code required for private)
    @io.swagger.v3.oas.annotations.Operation(summary = "Join Team (팀 가입 신청)", description = "팀에 가입을 신청합니다. 비공개 팀의 경우 가입 코드가 필요할 수 있습니다.")
    @PostMapping("/{teamId}/join")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse>> joinTeam(
            @PathVariable String teamId,
            @io.swagger.v3.oas.annotations.Parameter(description = "비공개 팀 가입 코드") @RequestParam(required = false) String code) {
        User currentUser = getCurrentUser();
        com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse response = teamService.joinTeam(teamId,
                currentUser, code);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    // 6. 멤버 승인 (Leader Only)
    @io.swagger.v3.oas.annotations.Operation(summary = "Approve Member (멤버 가입 승인)", description = "가입 신청한 멤버를 승인합니다. (Leader Only)")
    @PostMapping("/{teamId}/approve")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse>> approveMember(
            @PathVariable String teamId,
            @RequestParam Long userId) {
        User currentUser = getCurrentUser();
        com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse response = teamService.approveMember(teamId,
                userId, currentUser);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    // 7. 멤버 강퇴 (Leader Only)
    @io.swagger.v3.oas.annotations.Operation(summary = "Remove Member (멤버 추방)", description = "팀 멤버를 추방합니다. (Leader Only)")
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<Void>> removeMember(
            @PathVariable String teamId,
            @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        teamService.removeMember(teamId, userId, currentUser);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(null));
    }

    // 8. 내가 팀장인 팀 목록 조회
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Teams I Lead (내가 팀장인 팀 조회)", description = "내가 팀장으로 있는 팀 목록을 조회합니다.")
    @GetMapping("/leader")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<com.backend.githubanalyzer.domain.team.dto.TeamDetailResponse>>> getTeamsLeaderOf() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(
                teamService.getTeamsLeaderOf(currentUser)));
    }

    // 9. 팀에 등록 가능한 레포지토리 조회 (Leader Only)
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Available Repos (등록 가능 레포지토리 조회)", description = "팀에 등록 가능한(팀장 및 팀원 소유) 레포지토리 목록을 조회합니다. (Leader Only)")
    @GetMapping("/{teamId}/repos/available")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse>>> getAvailableRepos(
            @PathVariable String teamId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(
                teamService.getAvailableReposForTeam(teamId, currentUser)));
    }

    // 10. 팀에 레포지토리 등록 (Leader Only)
    @io.swagger.v3.oas.annotations.Operation(summary = "Add Repo to Team (팀에 레포지토리 등록)", description = "팀에 레포지토리를 등록합니다. (Leader Only)")
    @PostMapping("/{teamId}/repos")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<Void>> addRepoToTeam(
            @PathVariable String teamId,
            @RequestBody com.backend.githubanalyzer.domain.repository.dto.RepoAddRequest request) {
        User currentUser = getCurrentUser();
        teamService.addRepoToTeam(teamId, request.repoId(), currentUser);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(null));
    }

    // 11. 팀 소유 레포지토리 조회 (Visibility checked)
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Team Repos (팀 레포지토리 조회)", description = "팀에 등록된 레포지토리 목록을 조회합니다.")
    @GetMapping("/{teamId}/repos")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse>>> getTeamRepos(
            @PathVariable String teamId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(
                teamService.getTeamRepos(teamId, currentUser)));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}