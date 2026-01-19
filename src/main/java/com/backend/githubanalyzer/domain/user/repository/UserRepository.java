package com.backend.githubanalyzer.domain.user.repository;

import com.backend.githubanalyzer.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByGithubId(String githubId);

    List<User> findAllByUsernameContainingIgnoreCase(String username);

    // 1. Batch Notification Finders
    List<User> findAllByNotifyWeeklyTrue();

    List<User> findAllByNotifySprintTrue();

    // 2. Analytics Finders
    List<User> findByCompany(String company);

    List<User> findByLocationContaining(String location);

    // 3. Existence Check
    boolean existsByUsername(String username);

    // 4. Counts
    long countByIsGhostTrue();
}
