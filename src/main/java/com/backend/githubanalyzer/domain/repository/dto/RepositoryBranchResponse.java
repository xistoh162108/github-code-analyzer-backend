package com.backend.githubanalyzer.domain.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryBranchResponse {
    private String name;
    private String lastCommitSha;
    private Long commitCount;
}
