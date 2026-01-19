package com.backend.githubanalyzer.domain.commit.controller;

import com.backend.githubanalyzer.domain.commit.dto.CommitAnalysisResponse;
import com.backend.githubanalyzer.domain.commit.dto.CommitResponse;
import com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus;
import com.backend.githubanalyzer.domain.commit.service.CommitService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos/{repoId}/commits")
@RequiredArgsConstructor
public class CommitController {

    private final CommitService commitService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommitResponse>>> getCommits(@PathVariable String repoId) {
        return ResponseEntity.ok(ApiResponse.success(commitService.getCommits(repoId)));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<CommitResponse>>> getActivities(@PathVariable String repoId) {
        return getCommits(repoId);
    }

    @GetMapping("/{sha}/analysis")
    public ResponseEntity<ApiResponse<CommitAnalysisResponse>> getAnalysis(
            @PathVariable String repoId,
            @PathVariable String sha) {
        CommitAnalysisResponse analysis = commitService.getAnalysis(repoId, sha);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @GetMapping("/{sha}/status")
    public ResponseEntity<ApiResponse<AnalysisStatus>> getStatus(
            @PathVariable String repoId,
            @PathVariable String sha) {
        AnalysisStatus status = commitService.getStatus(repoId, sha);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
