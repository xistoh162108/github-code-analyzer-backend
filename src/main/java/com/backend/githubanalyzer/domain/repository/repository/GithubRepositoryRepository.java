package com.backend.githubanalyzer.domain.repository.repository;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, String> {

    Optional<GithubRepository> findByRepoUrl(String repoUrl);

    boolean existsByRepoUrl(String repoUrl);

    List<GithubRepository> findAllByOwnerId(Long ownerId);

    List<GithubRepository> findAllByReponameContainingIgnoreCase(String reponame);
}
