package com.backend.githubanalyzer.domain.ranking.controller;

import com.backend.githubanalyzer.domain.ranking.dto.CommitRankResponse;
import com.backend.githubanalyzer.domain.ranking.dto.UserRankResponse;
import com.backend.githubanalyzer.domain.ranking.service.RankingService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Ranking", description = "랭킹/통계 관련 API. 커밋 및 유저 랭킹 조회")
public class RankingController {

    private final RankingService rankingService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Top Commits (커밋 랭킹 조회)", description = "지정된 조건(Scope, Period)에 따라 점수가 가장 높은 커밋 목록을 반환합니다.<br>"
            +
            "**Scope**: `GLOBAL` (전체), `SPRINT` (스프린트 안), `USER` (개인별), `TEAM` (팀 멤버별)<br>" +
            "**Period**: `WEEK` (이번 주), `MONTH` (이번 달), `SPRINT` (스프린트 기간), `ALL` (전체) 등")
    @GetMapping("/commits")
    public ResponseEntity<ApiResponse<List<CommitRankResponse>>> getCommitRankings(
            @io.swagger.v3.oas.annotations.Parameter(description = "랭킹 범위 (GLOBAL, SPRINT, TEAM, USER)", example = "GLOBAL") @RequestParam(defaultValue = "GLOBAL") String scope,

            @io.swagger.v3.oas.annotations.Parameter(description = "범위 지정 ID (SprintId, UserId, TeamId 등). scope가 GLOBAL이면 무시됨.", example = "10") @RequestParam(required = false) String id,

            @io.swagger.v3.oas.annotations.Parameter(description = "집계 기간 (ALL, YEAR, MONTH, WEEK, DAY, HOUR, SPRINT). Default: WEEK", example = "WEEK") @RequestParam(defaultValue = "WEEK") RankingService.Period period,

            @io.swagger.v3.oas.annotations.Parameter(description = "반환 개수 제한", example = "10") @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success(
                rankingService.getCommitRankings(scope, id, period, limit)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Top Users (유저 랭킹 조회)", description = "지정된 조건(Scope, Period)에 따라 점수가 가장 높은 유저 목록을 반환합니다.<br>"
            +
            "**Scope**: `GLOBAL` (전체), `SPRINT` (스프린트 참여자), `TEAM` (팀 멤버)<br>" +
            "**Period**: `ALL` (전체), `MONTH` (이번 달), `SPRINT` (스프린트 기간) 등")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserRankResponse>>> getUserRankings(
            @io.swagger.v3.oas.annotations.Parameter(description = "랭킹 범위 (GLOBAL, SPRINT, TEAM)", example = "GLOBAL") @RequestParam(defaultValue = "GLOBAL") String scope,

            @io.swagger.v3.oas.annotations.Parameter(description = "범위 지정 ID (SprintId, TeamId). scope가 GLOBAL이면 무시됨.", example = "10") @RequestParam(required = false) String id,

            @io.swagger.v3.oas.annotations.Parameter(description = "집계 기간 (ALL, YEAR, MONTH, WEEK, DAY, HOUR, SPRINT). Default: ALL", example = "ALL") @RequestParam(defaultValue = "ALL") RankingService.Period period,

            @io.swagger.v3.oas.annotations.Parameter(description = "반환 개수 제한", example = "10") @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success(
                rankingService.getUserRankings(scope, id, period, limit)));
    }
}
