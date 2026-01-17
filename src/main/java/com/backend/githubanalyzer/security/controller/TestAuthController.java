package com.backend.githubanalyzer.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestAuthController {

    @GetMapping("/api/auth/test/success")
    public Map<String, String> testSuccess(@RequestParam String accessToken, @RequestParam String refreshToken) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("info", "GitHub OAuth Login Successful! Use these tokens for further requests.");
        return tokens;
    }
}
