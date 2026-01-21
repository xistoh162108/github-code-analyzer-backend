package com.backend.githubanalyzer.domain.search.controller;

import com.backend.githubanalyzer.domain.search.dto.UnifiedSearchResponse;
import com.backend.githubanalyzer.domain.search.service.SearchService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Search", description = "통합 검색 API")
public class SearchController {
    private final SearchService searchService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Integrated Search (통합 검색)", description = "사용자, 레포지토리, 팀 등을 한번에 검색합니다.<br>"
            +
            "**Type**: `ALL` (전체), `USER` (유저), `REPOSITORY` (레포), `TEAM` (팀)")
    @GetMapping
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "ALL") String type) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, type)));
    }
}
