package com.backend.githubanalyzer.security.controller;

import com.backend.githubanalyzer.global.dto.ApiResponse;
import com.backend.githubanalyzer.security.dto.JwtToken;
import com.backend.githubanalyzer.security.dto.TokenRefreshRequest;
import com.backend.githubanalyzer.security.dto.TokenValidationRequest;
import com.backend.githubanalyzer.security.dto.TokenValidationResponse;
import com.backend.githubanalyzer.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "OAuth2 및 JWT 토큰 관리 API. (로그인: /oauth2/authorization/github)")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Token Validation (토큰 검증)", description = "Access Token 또는 Refresh Token의 유효성을 검증하고 정보를 반환합니다.")
    @PostMapping("/api/auth/validate-token")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestBody TokenValidationRequest request) {
        String token = request.getToken();

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("토큰이 제공되지 않았습니다."));
        }

        boolean isValid = false;
        String type = "unknown";
        TokenValidationResponse.UserInfo user = null;
        String message = "유효한 토큰입니다.";
        String issuedAt = null;
        String expiresAt = null;

        try {
            isValid = jwtTokenProvider.validateToken(token);
            Claims claims = jwtTokenProvider.parseClaims(token);

            if (claims != null) {
                type = (String) claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE);
                String username = claims.getSubject();

                List<String> roles = new java.util.ArrayList<>();
                Object auth = claims.get(JwtTokenProvider.CLAIM_AUTH);
                if (auth != null) {
                    roles = java.util.Arrays.asList(auth.toString().split(","));
                }

                user = TokenValidationResponse.UserInfo.builder()
                        .username(username)
                        .roles(roles)
                        .build();

                if (claims.getIssuedAt() != null)
                    issuedAt = claims.getIssuedAt().toString();
                if (claims.getExpiration() != null)
                    expiresAt = claims.getExpiration().toString();

                if (JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(type)) {
                    boolean refreshValid = jwtTokenProvider.validateRefreshToken(token);
                    if (!refreshValid) {
                        isValid = false;
                        message = "리프레시 토큰이 화이트리스트에 없습니다 (이미 사용되었거나 삭제됨).";
                    }
                }
            } else {
                isValid = false;
                message = "토큰을 파싱할 수 없습니다.";
            }

        } catch (ExpiredJwtException e) {
            isValid = false;
            message = "토큰이 만료되었습니다.";
        } catch (SecurityException | MalformedJwtException e) {
            isValid = false;
            message = "잘못된 토큰 형식 또는 서명입니다.";
        } catch (Exception e) {
            isValid = false;
            message = "토큰 검증 중 오류가 발생했습니다: " + e.getMessage();
        }

        TokenValidationResponse response = TokenValidationResponse.builder()
                .valid(isValid)
                .type(type)
                .user(user)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .message(message)
                .build();

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("Success", response));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Invalid token status", response));
        }
    }

    @Operation(summary = "Refresh Token (토큰 갱신)", description = "만료된 Access Token을 Refresh Token을 통해 갱신합니다.")
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ApiResponse<JwtToken>> refresh(@RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            String username = jwtTokenProvider.getUserNameFromToken(refreshToken);
            JwtToken newTokens = jwtTokenProvider.generateTokenWithRefreshToken(username);
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", newTokens));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("유효하지 않은 리프레시 토큰입니다."));
    }

    @Operation(summary = "Logout (로그아웃)", description = "서버에서 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/api/auth/logout")
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

    @GetMapping("/auth/callback")
    @io.swagger.v3.oas.annotations.Hidden
    public java.util.Map<String, Object> authCallback(
            @RequestParam String accessToken,
            @RequestParam String refreshToken) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", "success");
        response.put("message", "GitHub Login Successful! (Demo Mode)");

        java.util.Map<String, String> tokens = new java.util.HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        response.put("data", tokens);
        response.put("instruction",
                "Copy the accessToken and use it in the Authorization header (Bearer <token>) for API calls.");

        return response;
    }
}
