package com.backend.githubanalyzer.domain.dashboard.controller;

import com.backend.githubanalyzer.domain.dashboard.dto.DashboardStatsResponse;
import com.backend.githubanalyzer.domain.dashboard.dto.UserProfileResponse;
import com.backend.githubanalyzer.domain.dashboard.service.DashboardService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "User & Dashboard", description = "유저 프로필 및 개인 대시보드 API")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @io.swagger.v3.oas.annotations.Operation(summary = "My Dashboard (내 대시보드 조회)", description = "로그인한 사용자의 개인 통계(Streak, 총 커밋 수, 최근 활동 등)를 조회합니다.")
    @GetMapping("/me/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getMyDashboard() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats(currentUser.getId())));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "User Profile (유저 공개 프로필 조회)", description = "특정 유저의 공개 프로필 정보(뱃지, 티어, 기본 정보)를 조회합니다.")
    @GetMapping("/{username}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getUserProfile(username)));
    }

    private User getCurrentUser() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal));
    }
}
