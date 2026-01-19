package com.backend.githubanalyzer.domain.repository.controller;

import com.backend.githubanalyzer.domain.repository.dto.ContributorResponse;
import com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse;
import com.backend.githubanalyzer.domain.repository.dto.RepositoryMetricResponse;
import com.backend.githubanalyzer.domain.sync.service.GithubPersistenceService;
import com.backend.githubanalyzer.domain.sync.service.GithubSyncService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class GithubRepositoryController {

    private final GithubPersistenceService githubPersistenceService;
    private final GithubSyncService githubSyncService;

    @GetMapping("/{repoId}")
    public ResponseEntity<ApiResponse<GithubRepositoryResponse>> getRepository(@PathVariable String repoId) {
        GithubRepositoryResponse response = githubPersistenceService.getRepositoryResponse(repoId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{repoId}/metrics")
    public ResponseEntity<ApiResponse<RepositoryMetricResponse>> getMetrics(@PathVariable String repoId) {
        RepositoryMetricResponse metrics = githubPersistenceService.getRepositoryMetrics(repoId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/{repoId}/contributors")
    public ResponseEntity<ApiResponse<List<ContributorResponse>>> getContributors(@PathVariable String repoId) {
        List<ContributorResponse> contributors = githubPersistenceService.getContributors(repoId);
        return ResponseEntity.ok(ApiResponse.success(contributors));
    }

    @PostMapping("/{repoId}/sync")
    public ResponseEntity<ApiResponse<Void>> sync(@PathVariable String repoId) {
        githubSyncService.syncRepo(repoId);
        return ResponseEntity.ok(ApiResponse.success("동기화 요청이 접수되었습니다.", null));
    }
}
