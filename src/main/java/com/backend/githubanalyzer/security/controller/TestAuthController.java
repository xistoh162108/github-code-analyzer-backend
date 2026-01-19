package com.backend.githubanalyzer.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Authentication Test", description = "OAuth2 로그인 성공 시 토큰 확인용 API")
public class TestAuthController {

    @Operation(summary = "로그인 성공 콜백", description = "GitHub 로그인 성공 후 토큰을 화면에 표시합니다.")
    @GetMapping("/api/auth/test/success")
    public Map<String, String> testSuccess(@RequestParam String accessToken, @RequestParam String refreshToken) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("info", "GitHub OAuth Login Successful! Use these tokens for further requests.");
        return tokens;
    }
}
