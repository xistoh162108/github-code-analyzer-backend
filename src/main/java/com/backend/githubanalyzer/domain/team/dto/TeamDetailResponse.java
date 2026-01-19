package com.backend.githubanalyzer.domain.team.dto;

import com.backend.githubanalyzer.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamDetailResponse {
    private String teamId;
    private String name;
    private String description;
    private String leaderUsername;
    private String leaderProfileUrl;
    private Boolean isPublic;
    // Add more fields if full details (e.g., members summary, etc.)
    private String joinCode; // teamId for private teams?

    public static TeamDetailResponse from(Team team) {
        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .leaderUsername(team.getLeader().getUsername())
                .leaderProfileUrl(team.getLeader().getProfileUrl())
                .isPublic(team.getIsPublic())
                .joinCode(team.getIsPublic() ? null : team.getId()) // Only show if authorized full view
                .build();
    }

    public static TeamDetailResponse limited(Team team) {
        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .leaderUsername(team.getLeader().getUsername())
                .leaderProfileUrl(team.getLeader().getProfileUrl())
                .isPublic(team.getIsPublic())
                .joinCode(null) // Hide for limited view
                .build();
    }
}
