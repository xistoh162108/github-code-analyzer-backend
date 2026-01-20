package com.backend.githubanalyzer.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @Schema(description = "Company", example = "Google")
    private String company;
    @Schema(description = "Location", example = "Seoul, Korea")
    private String location;
    @Schema(description = "Notification Email", example = "user@example.com")
    private String notifyEmail;
    @Schema(description = "Sprint Notification Opt-in", example = "true")
    private Boolean notifySprint;
    @Schema(description = "Weekly Report Opt-in", example = "true")
    private Boolean notifyWeekly;
}
