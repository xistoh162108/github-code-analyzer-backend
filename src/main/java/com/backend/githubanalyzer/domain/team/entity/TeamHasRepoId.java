package com.backend.githubanalyzer.domain.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TeamHasRepoId implements Serializable {

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "repo_id")
    private String repoId;
}
