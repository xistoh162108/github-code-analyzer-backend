package com.backend.githubanalyzer.domain.team.repository;

import com.backend.githubanalyzer.domain.team.entity.TeamHasRepo;
import com.backend.githubanalyzer.domain.team.entity.TeamHasRepoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamHasRepoRepository extends JpaRepository<TeamHasRepo, TeamHasRepoId> {
    List<TeamHasRepo> findByRepositoryId(String repoId);
}
