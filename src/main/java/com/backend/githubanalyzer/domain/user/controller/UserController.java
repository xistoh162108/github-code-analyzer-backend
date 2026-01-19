package com.backend.githubanalyzer.domain.user.controller;

import com.backend.githubanalyzer.domain.commit.dto.CommitHeatmapResponse;
import com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse;
import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import com.backend.githubanalyzer.security.jwt.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final com.backend.githubanalyzer.domain.commit.service.CommitService commitService;

    public UserController(UserService userService,
            com.backend.githubanalyzer.domain.commit.service.CommitService commitService) {
        this.userService = userService;
        this.commitService = commitService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        String email = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(email)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestBody com.backend.githubanalyzer.domain.user.dto.UserUpdateRequest request) {
        String email = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", userService.updateMe(email, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        String email = JwtUtil.getCurrentUsername();
        userService.deleteUser(email);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    @GetMapping("/me/activities/heatmap")
    public ResponseEntity<ApiResponse<List<CommitHeatmapResponse>>> heatmap() {
        String email = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserHeatmap(email)));
    }

    @GetMapping("/me/repositories")
    public ResponseEntity<ApiResponse<List<GithubRepositoryResponse>>> repositories() {
        String email = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserRepositories(email)));
    }

    @GetMapping("/me/commits/recent")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse>>> recentCommits() {
        String email = JwtUtil.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(commitService.getUserRecentCommits(email)));
    }

    @GetMapping("/{userId}/repositories/{repoId}/commits")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse>>> userRepoCommits(
            @PathVariable Long userId,
            @PathVariable String repoId) {
        return ResponseEntity.ok(ApiResponse.success(commitService.getUserCommitsInRepo(userId, repoId)));
    }
}
