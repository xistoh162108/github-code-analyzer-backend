package com.backend.githubanalyzer.domain.commit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class CommitId implements Serializable {

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "repo_id")
    private String repoId;

    @Column(name = "branch_name")
    private String branchName;
}
