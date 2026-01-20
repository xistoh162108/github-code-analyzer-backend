package com.backend.githubanalyzer.domain.sync.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitSyncJobRequest implements Serializable {
    private String owner;
    private String repoName;
    private String sha;
    private String branchName;
    private Long userId; // Platform user who initiated/associated
    private String repositoryId; // Database ID
    private String accessToken; // Token to use for this specific job
}
