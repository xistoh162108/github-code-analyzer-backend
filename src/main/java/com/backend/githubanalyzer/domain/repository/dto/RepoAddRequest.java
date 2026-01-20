package com.backend.githubanalyzer.domain.repository.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RepoAddRequest(
        @Schema(description = "Repository ID to add", example = "MDEwOlJlcG9zaXRvcnkzMDc0NDQ4NDI=")
        String repoId
) {}
