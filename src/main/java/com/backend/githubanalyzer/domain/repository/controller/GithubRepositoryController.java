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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Repository", description = "GitHub 레포지토리 관리 및 조회 API")
public class GithubRepositoryController {

    private final GithubPersistenceService githubPersistenceService;
    private final GithubSyncService githubSyncService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Repository Details (레포지토리 상세 조회)", description = "레포지토리의 기본 정보, 언어 통계, 설명 등을 조회합니다.")
    @GetMapping("/{repoId}")
    public ResponseEntity<ApiResponse<GithubRepositoryResponse>> getRepository(@PathVariable String repoId) {
        GithubRepositoryResponse response = githubPersistenceService.getRepositoryResponse(repoId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Repository Metrics (레포지토리 통계)", description = "커밋 수, 이슈 수, 스타 수 등 레포지토리의 주요 지표를 조회합니다.")
    @GetMapping("/{repoId}/metrics")
    public ResponseEntity<ApiResponse<RepositoryMetricResponse>> getMetrics(@PathVariable String repoId) {
        RepositoryMetricResponse metrics = githubPersistenceService.getRepositoryMetrics(repoId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Contributors (기여자 목록 조회)", description = "해당 레포지토리의 기여자 목록을 커밋 수/점수 기준으로 정렬하여 반환합니다.")
    @GetMapping("/{repoId}/contributors")
    public ResponseEntity<ApiResponse<List<ContributorResponse>>> getContributors(@PathVariable String repoId) {
        List<ContributorResponse> contributors = githubPersistenceService.getContributors(repoId);
        return ResponseEntity.ok(ApiResponse.success(contributors));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Sync Repository (동기화 요청)", description = "레포지토리의 최신 데이터를 GitHub에서 받아오도록 비동기 동기화 작업을 요청합니다.")
    @PostMapping("/{repoId}/sync")
    public ResponseEntity<ApiResponse<com.backend.githubanalyzer.domain.repository.dto.SyncStatusResponse>> sync(
            @PathVariable String repoId) {
        com.backend.githubanalyzer.domain.repository.dto.SyncStatusResponse response = githubSyncService
                .syncRepo(repoId);
        return ResponseEntity.ok(ApiResponse.success("동기화 요청이 접수되었습니다.", response));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Repository Branches (브랜치 목록 조회)", description = "레포지토리의 브랜치 목록과 각 브랜치의 커밋 수를 조회합니다.")
    @GetMapping("/{repoId}/branches")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.repository.dto.RepositoryBranchResponse>>> getBranches(
            @PathVariable String repoId) {
        return ResponseEntity.ok(ApiResponse.success(githubSyncService.getRepositoryBranches(repoId)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Commits by Branch (브랜치별 커밋 조회)", description = "특정 브랜치의 커밋 목록을 최신순으로 조회합니다.")
    @GetMapping("/{repoId}/branches/{branchName}/commits")
    public ResponseEntity<ApiResponse<List<com.backend.githubanalyzer.domain.commit.dto.CommitResponse>>> getValCommitsByBranch(
            @PathVariable String repoId,
            @PathVariable String branchName) {
        return ResponseEntity.ok(ApiResponse.success(githubSyncService.getCommitsByBranch(repoId, branchName)));
    }
}
