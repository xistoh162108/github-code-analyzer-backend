package com.backend.githubanalyzer.domain.team.entity;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_has_repo")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TeamHasRepo {

    @EmbeddedId
    private TeamHasRepoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repoId")
    @JoinColumn(name = "repo_id")
    private GithubRepository repository;
}
