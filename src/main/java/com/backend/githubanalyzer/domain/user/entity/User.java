package com.backend.githubanalyzer.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // GitHub Login ID

    @Column(nullable = false, unique = true)
    private String email;

    private String notifyEmail;

    @Column(nullable = false)
    @Builder.Default
    private Boolean notifySprint = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean notifyWeekly = true;

    private String profileUrl; // GitHub Avatar URL

    private String location;

    private Integer publicRepos;

    private String company;

    @Column(nullable = false, unique = true)
    private String githubId; // Keep numerical ID for internal tracking if needed

    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.notifyEmail == null) {
            this.notifyEmail = this.email;
        }
    }
}
