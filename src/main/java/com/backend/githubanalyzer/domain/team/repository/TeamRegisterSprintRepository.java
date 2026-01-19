package com.backend.githubanalyzer.domain.team.repository;

import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprintId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRegisterSprintRepository extends JpaRepository<TeamRegisterSprint, TeamRegisterSprintId> {
    java.util.List<TeamRegisterSprint> findByRepositoryId(String repoId);

    java.util.List<TeamRegisterSprint> findByTeamId(String teamId);
}
