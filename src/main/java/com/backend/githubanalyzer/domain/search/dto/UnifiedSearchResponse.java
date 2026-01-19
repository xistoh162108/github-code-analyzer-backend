package com.backend.githubanalyzer.domain.search.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class UnifiedSearchResponse {
    private List<UserSearchResult> users;
    private List<RepoSearchResult> repositories;
    private List<TeamSearchResult> teams;
    private List<SprintSearchResult> sprints;

    @Getter
    @Builder
    public static class UserSearchResult {
        private Long id;
        private String username;
        private String profileUrl;
    }

    @Getter
    @Builder
    public static class RepoSearchResult {
        private String id;
        private String reponame;
        private String description;
    }

    @Getter
    @Builder
    public static class TeamSearchResult {
        private String id;
        private String name;
        private String description;
    }

    @Getter
    @Builder
    public static class SprintSearchResult {
        private String id;
        private String name;
        private String description;
    }

    @Getter
    @Builder
    public static class CommitSearchResult {
        private String sha;
        private String message;
        private String repoId;
        private String repoName;
        private String authorName;
        private java.time.LocalDateTime committedAt;
    }

    private List<CommitSearchResult> commits;
}
