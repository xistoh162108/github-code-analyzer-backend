package com.backend.githubanalyzer.domain.team.dto;

public record TeamUpdateRequest(
        String name,
        String description
// boolean isPublic // Not including for now as requirement only explicitly
// mentions Name & Desc editability
) {
}
