package com.backend.githubanalyzer.domain.ranking.dto;

import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommitRankResponse {
    private long rank;
    private Commit commit;

    public static CommitRankResponse of(long rank, Commit commit) {
        return CommitRankResponse.builder()
                .rank(rank)
                .commit(commit)
                .build();
    }
}
