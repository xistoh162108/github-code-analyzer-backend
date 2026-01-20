package com.backend.githubanalyzer.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisJobRequest implements Serializable {
    private String commitSha;
    private String repositoryId;
    @Builder.Default
    private int retryCount = 0;
    
    // Batch Context
    private String batchId;
}
