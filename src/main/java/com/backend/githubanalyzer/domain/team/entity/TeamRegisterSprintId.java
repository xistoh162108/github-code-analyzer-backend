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
public class TeamRegisterSprintId implements Serializable {

    @Column(name = "sprint_id")
    private String sprintId;

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "repo_id")
    private String repoId;
}
