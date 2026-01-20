package com.backend.githubanalyzer.domain.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Hidden // Hide from Swagger as it's a temporary helper
public class AuthController {

    @GetMapping("/auth/callback")
    public Map<String, Object> authCallback(
            @RequestParam String accessToken,
            @RequestParam String refreshToken) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "GitHub Login Successful! (Demo Mode)");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        response.put("data", tokens);
        response.put("instruction",
                "Copy the accessToken and use it in the Authorization header (Bearer <token>) for API calls.");

        return response;
    }
}
