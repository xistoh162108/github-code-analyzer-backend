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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Commit Analysis", description = "커밋 조회 및 AI 분석 결과 확인 API")
public class CommitController {

    private final CommitService commitService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Commits (커밋 목록 조회)", description = "레포지토리의 최근 커밋 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommitResponse>>> getCommits(@PathVariable String repoId) {
        return ResponseEntity.ok(ApiResponse.success(commitService.getCommits(repoId)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Commits (Alias)", description = "getCommits와 동일합니다.")
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<CommitResponse>>> getActivities(@PathVariable String repoId) {
        return getCommits(repoId);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Analysis Result (AI 분석 결과 조회)", description = "특정 커밋의 AI 분석 상세 결과(코드 설명, 점수, 피드백 등)를 조회합니다.")
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Analysis Status (분석 진행 상태 조회)", description = "커밋 분석의 진행 상태(PENDING, IN_PROGRESS, COMPLETED, FAILED)를 조회합니다.")
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
