package com.backend.githubanalyzer.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String company;
    private String location;
    private String notifyEmail;
    private Boolean notifySprint;
    private Boolean notifyWeekly;
}
