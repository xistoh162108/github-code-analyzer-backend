package com.backend.githubanalyzer.domain.sprint.repository;

import com.backend.githubanalyzer.domain.sprint.entity.SprintBannedTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SprintBannedTeamRepository extends JpaRepository<SprintBannedTeam, Long> {
    Optional<SprintBannedTeam> findBySprintIdAndTeamId(String sprintId, String teamId);

    boolean existsBySprintIdAndTeamId(String sprintId, String teamId);
}
