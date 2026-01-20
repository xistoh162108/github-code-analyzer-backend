package com.backend.githubanalyzer.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Paginated Response Wrapper")
public class PageResponse<T> {
    
    @Schema(description = "Current page content")
    private final List<T> content;
    
    @Schema(description = "Whether there is a next page", example = "true")
    private final boolean hasNext;

    public static <T> PageResponse<T> of(List<T> content, boolean hasNext) {
        return new PageResponse<>(content, hasNext);
    }
}
