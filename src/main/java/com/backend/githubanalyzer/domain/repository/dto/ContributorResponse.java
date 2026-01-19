package com.backend.githubanalyzer.domain.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ContributorResponse {
    private String username;
    private String profileUrl;
    private String role;
    private Long rank;
    private Long score;
}
