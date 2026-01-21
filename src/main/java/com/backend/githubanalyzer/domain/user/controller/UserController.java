package com.backend.githubanalyzer.domain.user.controller;

import com.backend.githubanalyzer.domain.commit.dto.CommitHeatmapResponse;
import com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse;
import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import com.backend.githubanalyzer.security.jwt.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@io.swagger.v3.oas.annotations.tags.Tag(name = "User & Dashboard", description = "유저 프로필 및 개인 대시보드 API")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.backend.githubanalyzer.domain.commit.service.CommitService commitService;
    private final com.backend.githubanalyzer.domain.dashboard.service.DashboardService dashboardService;
    private final com.backend.githubanalyzer.domain.user.repository.UserRepository userRepository;


    @io.swagger.v3.oas.annotations.Operation(summary = "Get My Profile (내 정보 조회)", description = "로그인한 유저의 상세 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        String username = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(username)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update My Profile (내 정보 수정)", description = "로그인한 유저의 프로필 정보를 수정합니다.")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestBody com.backend.githubanalyzer.domain.user.dto.UserUpdateRequest request) {
        String username = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", userService.updateMe(username, request)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Withdraw (회원 탈퇴)", description = "회원 탈퇴를 진행합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        String username = JwtUtil.getCurrentUsername();
        userService.deleteUser(username);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "My Heatmap (내 커밋 히트맵)", description = "로그인한 유저의 커밋 히트맵 데이터를 조회합니다.")
    @GetMapping("/me/activities/heatmap")
    public ResponseEntity<ApiResponse<List<CommitHeatmapResponse>>> heatmap() {
        String username = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserHeatmap(username)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "My Repositories (내 레포지토리 목록)", description = "로그인한 유저의 연동된 레포지토리 목록을 조회합니다.")
    @GetMapping("/me/repositories")
    public ResponseEntity<ApiResponse<List<GithubRepositoryResponse>>> repositories() {
        String username = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserRepositories(username)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "My Recent Commits (내 최근 커밋)", description = "로그인한 유저의 최근 커밋 내역을 조회합니다.")
    @GetMapping("/me/commits/recent")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse>>> recentCommits() {
        String username = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(commitService.getUserRecentCommits(username)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "User Repo Commits (특정 유저/레포 커밋 조회)", description = "특정 유저와 레포지토리의 커밋 목록을 조회합니다.")
    @GetMapping("/{userId}/repositories/{repoId}/commits")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse>>> userRepoCommits(
            @PathVariable Long userId,
            @PathVariable String repoId) {
        return ResponseEntity.ok(ApiResponse.success(commitService.getUserCommitsInRepo(userId, repoId)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "My Dashboard (내 대시보드 조회)", description = "로그인한 사용자의 개인 통계(Streak, 총 커밋 수, 최근 활동 등)를 조회합니다.")
    @GetMapping("/me/dashboard")
    public ResponseEntity<ApiResponse<com.backend.githubanalyzer.domain.dashboard.dto.DashboardStatsResponse>> getMyDashboard() {
        com.backend.githubanalyzer.domain.user.entity.User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats(currentUser.getId())));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "User Profile (유저 공개 프로필 조회)", description = "특정 유저의 공개 프로필 정보(뱃지, 티어, 기본 정보)를 조회합니다.")
    @GetMapping("/{username}/profile")
    public ResponseEntity<ApiResponse<com.backend.githubanalyzer.domain.dashboard.dto.UserProfileResponse>> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getUserProfile(username)));
    }

    private com.backend.githubanalyzer.domain.user.entity.User getCurrentUser() {
        String principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal));
    }
}
