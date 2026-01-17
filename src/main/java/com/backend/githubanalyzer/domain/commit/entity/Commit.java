package com.backend.githubanalyzer.domain.commit.entity;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commits")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Commit {

    @EmbeddedId
    private CommitId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repoId")
    @JoinColumn(name = "repo_id")
    private GithubRepository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] diff;

    @Column(name = "before_commit_id")
    private String beforeCommitId; // Storing parent SHA (String) as requested

    @Column(nullable = false)
    private LocalDateTime committedAt;
}
