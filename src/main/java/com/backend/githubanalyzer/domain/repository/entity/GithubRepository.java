package com.backend.githubanalyzer.domain.repository.entity;

import com.backend.githubanalyzer.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

    private String description;

    private String language;

    private Long size;

    private Long stars;

    private String topics; // CSV format: "spring,security,jwt"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "pushed_at", nullable = false)
    private LocalDateTime pushedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
