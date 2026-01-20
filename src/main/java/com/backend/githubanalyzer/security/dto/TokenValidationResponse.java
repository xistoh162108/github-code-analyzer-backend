package com.backend.githubanalyzer.security.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private String type;
    private UserInfo user;
    private String issuedAt;
    private String expiresAt;
    private String message;

    @Getter
    @Builder
    public static class UserInfo {
        private String username;
        private List<String> roles;
    }
}
