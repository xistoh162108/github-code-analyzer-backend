package com.backend.githubanalyzer.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    @Schema(description = "User ID", example = "1")
    private Long id;
    @Schema(description = "Username", example = "johndoe")
    private String username;
    @Schema(description = "User Email", example = "john@example.com")
    private String email;
    @Schema(description = "Notification Email", example = "john@example.com")
    private String notifyEmail;
    @Schema(description = "Sprint Notification Opt-in", example = "true")
    private Boolean notifySprint;
    @Schema(description = "Weekly Report Opt-in", example = "true")
    private Boolean notifyWeekly;
    @Schema(description = "Profile Image URL", example = "https://github.com/johndoe.png")
    private String profileUrl;
    @Schema(description = "Location", example = "San Francisco, CA")
    private String location;
    @Schema(description = "Number of Public Repos", example = "42")
    private Integer publicRepos;
    @Schema(description = "Company", example = "Tech Corp")
    private String company;
    @Schema(description = "account Create Timestamp", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
}
