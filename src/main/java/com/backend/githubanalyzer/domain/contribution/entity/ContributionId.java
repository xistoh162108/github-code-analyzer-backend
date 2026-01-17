package com.backend.githubanalyzer.domain.contribution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class ContributionId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "repo_id")
    private String repoId;
}
