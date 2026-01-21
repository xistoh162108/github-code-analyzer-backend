package com.backend.githubanalyzer.infra.github.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@lombok.Setter
@NoArgsConstructor
public class GithubBranchResponse {
    private String name;
    private CommitInfo commit;

    @Getter
    @lombok.Setter
    @NoArgsConstructor
    public static class CommitInfo {
        private String sha;
    }
}
