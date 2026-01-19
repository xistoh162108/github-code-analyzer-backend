package com.backend.githubanalyzer.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class GithubCommitResponse {
    private String sha;
    private CommitDetail commit;
    private GithubUser author;
    private List<GithubFileResponse> files;
    private List<ParentInfo> parents;

    @Getter
    @NoArgsConstructor
    public static class ParentInfo {
        private String sha;
        private String url;
        @JsonProperty("html_url")
        private String htmlUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class CommitDetail {
        private String message;
        private AuthorInfo author;
    }

    @Getter
    @NoArgsConstructor
    public static class AuthorInfo {
        private String name;
        private LocalDateTime date;
    }

    @Getter
    @NoArgsConstructor
    public static class GithubUser {
        private String login;
        private Long id;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class GithubFileResponse {
        private String filename;
        private String patch;
    }
}
