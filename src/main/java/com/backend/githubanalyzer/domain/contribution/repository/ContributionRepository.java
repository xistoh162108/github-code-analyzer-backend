package com.backend.githubanalyzer.domain.contribution.repository;

import com.backend.githubanalyzer.domain.contribution.entity.Contribution;
import com.backend.githubanalyzer.domain.contribution.entity.ContributionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContributionRepository extends JpaRepository<Contribution, ContributionId> {

    List<Contribution> findAllByRepositoryId(String repoId);

    List<Contribution> findAllByUserId(Long userId);

    List<Contribution> findAllByRepositoryIdOrderByRankAsc(String repoId);
}
