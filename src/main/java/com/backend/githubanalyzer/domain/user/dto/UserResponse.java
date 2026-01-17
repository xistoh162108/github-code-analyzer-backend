package com.backend.githubanalyzer.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
