package com.backend.githubanalyzer.domain.ranking.dto;

import com.backend.githubanalyzer.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRankResponse {
    private long rank;
    private String username;
    private String profileUrl;
    private long totalScore;

    public static UserRankResponse of(long rank, User user, long totalScore) {
        return UserRankResponse.builder()
                .rank(rank)
                .username(user.getUsername())
                .profileUrl(user.getProfileUrl())
                .totalScore(totalScore)
                .build();
    }
}
