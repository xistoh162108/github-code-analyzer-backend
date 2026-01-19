package com.backend.githubanalyzer.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class GithubRepoResponse {
    @JsonProperty("node_id")
    private String id;
    private String name;
    private Owner owner;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("html_url")
    private String htmlUrl;
    private String description;
    private String language;
    private Long size;
    @JsonProperty("stargazers_count")
    private Long stars;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("pushed_at")
    private LocalDateTime pushedAt;

    @Getter
    @NoArgsConstructor
    public static class Owner {
        private String login;
        private Long id;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
}
