package com.backend.githubanalyzer.domain.repository.entity;

import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repositories", uniqueConstraints = {
        @UniqueConstraint(columnNames = "repo_url")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GithubRepository {

    @Id
    private String id; // GitHub node_id

    @Column(nullable = false)
    private String reponame;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String language;

    private Long size;

    private Long stars;

    @Column(columnDefinition = "TEXT")
    private String topics; // CSV format: "spring,security,jwt"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "pushed_at", nullable = false)
    private LocalDateTime pushedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "branch_num", nullable = false)
    @Builder.Default
    private Long branchNum = 0L;

    @Column(name = "commit_count", nullable = false)
    @Builder.Default
    private Long commitCount = 0L;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private Long score = 0L;

    @Column(name = "sync_status")
    private String syncStatus; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    private String branchesEtag;

    private String commitsEtag;

    @Column(columnDefinition = "TEXT")
    private String languages; // JSON: {"Java": 12000, "HTML": 3000}
}
