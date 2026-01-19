package com.backend.githubanalyzer.domain.contribution.entity;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contributions")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Contribution {

    @EmbeddedId
    private ContributionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repoId")
    @JoinColumn(name = "repo_id")
    private GithubRepository repository;

    @Column(name = "contribution_rank")
    private Long rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type")
    private ContributionType contributionType;
}
