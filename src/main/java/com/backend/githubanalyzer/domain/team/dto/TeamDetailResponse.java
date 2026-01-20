package com.backend.githubanalyzer.domain.team.dto;

import com.backend.githubanalyzer.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamDetailResponse {
    @Schema(description = "Team ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String teamId;
    @Schema(description = "Team Name", example = "Dev Team A")
    private String name;
    @Schema(description = "Team Description", example = "Backend development team")
    private String description;
    @Schema(description = "Leader Username", example = "johndoe")
    private String leaderUsername;
    @Schema(description = "Leader Profile Image URL", example = "https://github.com/johndoe.png")
    private String leaderProfileUrl;
    @Schema(description = "Public Team Flag", example = "true")
    private Boolean isPublic;
    @Schema(description = "Team ID acting as Join Code (for authorized view only)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String joinCode;

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
