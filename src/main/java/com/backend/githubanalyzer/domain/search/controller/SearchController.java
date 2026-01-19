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
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "ALL") String type) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, type)));
    }
}
