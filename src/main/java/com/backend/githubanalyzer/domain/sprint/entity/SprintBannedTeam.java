package com.backend.githubanalyzer.domain.sprint.entity;

import com.backend.githubanalyzer.domain.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_banned_teams")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SprintBannedTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @PrePersist
    protected void onBan() {
        bannedAt = LocalDateTime.now();
    }
}
