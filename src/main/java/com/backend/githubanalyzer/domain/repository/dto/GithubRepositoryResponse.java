package com.backend.githubanalyzer.domain.repository.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class GithubRepositoryResponse {
    private String id;
    private String reponame;
    private String repoUrl;
    private String description;
    private String language;
    private Long size;
    private Long stars;
    private List<String> topics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pushedAt;
}
