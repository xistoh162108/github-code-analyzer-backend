package com.backend.githubanalyzer.domain.team.entity;

import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_register_team")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserRegisterTeam {

    @EmbeddedId
    private UserRegisterTeamId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "role")
    private String role; // MEMBER, CONTRIBUTOR

    @Column(name = "status")
    private String status; // PENDING, APPROVED

    @Column(name = "in_team_rank")
    private Long inTeamRank;
}
