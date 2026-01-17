package com.backend.githubanalyzer.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String notifyEmail;
    private Boolean notifySprint;
    private Boolean notifyWeekly;
    private String profileUrl;
    private String location;
    private Integer publicRepos;
    private String company;
    private LocalDateTime createdAt;
}
