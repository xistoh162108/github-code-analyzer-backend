package com.backend.githubanalyzer.infra.github.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GithubBranchResponse {
    private String name;
    private CommitInfo commit;

    @Getter
    @NoArgsConstructor
    public static class CommitInfo {
        private String sha;
    }
}
