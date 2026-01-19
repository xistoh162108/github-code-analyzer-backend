package com.backend.githubanalyzer.domain.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobRequest implements Serializable {

    public enum JobType {
        INSTALLATION,
        UNINSTALLATION,
        PUSH,
        REPOSITORY_CREATED,
        REPOSITORY_DELETED,
        REPO_SYNC
    }

    private JobType type;
    private String installationId;
    private String repositoryId;
    private String branchName;
    private String githubLogin; // Used as fallback or for user lookup
    private String payload; // Raw payload for complex multi-step processing if needed
    @Builder.Default
    private int retryCount = 0;
}
