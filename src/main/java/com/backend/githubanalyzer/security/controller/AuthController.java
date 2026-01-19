package com.backend.githubanalyzer.security.controller;

import com.backend.githubanalyzer.global.dto.ApiResponse;
import com.backend.githubanalyzer.security.dto.JwtToken;
import com.backend.githubanalyzer.security.dto.TokenRefreshRequest;
import com.backend.githubanalyzer.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "OAuth2 및 JWT 토큰 관리 API")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtToken>> refresh(@RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            String username = jwtTokenProvider.getUserNameFromToken(refreshToken);
            JwtToken newTokens = jwtTokenProvider.generateTokenWithRefreshToken(username);
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", newTokens));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("유효하지 않은 리프레시 토큰입니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            try {
                jwtTokenProvider.deleteRefreshToken(username);
                log.info("User {} logged out successfully.", username);
                return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
            } catch (Exception e) {
                log.error("Error during logout for user {}: {}", username, e.getMessage());
                return ResponseEntity.internalServerError().body(ApiResponse.error("로그아웃 처리 중 오류가 발생했습니다."));
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("인증되지 않은 사용자입니다."));
    }
}
