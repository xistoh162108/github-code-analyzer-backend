package com.backend.githubanalyzer.domain.team.entity;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_register_sprint", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "sprint_id", "team_id" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TeamRegisterSprint {

    @EmbeddedId
    private TeamRegisterSprintId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sprintId")
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repoId")
    @JoinColumn(name = "repo_id")
    private GithubRepository repository;

    @Column(name = "sprint_rank")
    private Long sprintRank;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private Long score = 0L;

    @Column(name = "commit_num", nullable = false)
    @Builder.Default
    private Long commitNum = 0L;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "APPROVED"; // PENDING, APPROVED, REJECTED, BANNED
}
