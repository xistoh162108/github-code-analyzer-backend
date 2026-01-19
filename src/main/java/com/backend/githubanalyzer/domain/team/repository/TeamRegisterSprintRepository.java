package com.backend.githubanalyzer.domain.team.repository;

import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprintId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRegisterSprintRepository extends JpaRepository<TeamRegisterSprint, TeamRegisterSprintId> {
    java.util.List<TeamRegisterSprint> findByRepositoryId(String repoId);

    java.util.List<TeamRegisterSprint> findByTeamId(String teamId);

    long countBySprintId(String sprintId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(urt) FROM TeamRegisterSprint trs JOIN UserRegisterTeam urt ON trs.team.id = urt.team.id WHERE trs.sprint.id = :sprintId")
    long countParticipantsBySprintId(@org.springframework.data.repository.query.Param("sprintId") String sprintId);

    java.util.List<TeamRegisterSprint> findAllBySprintId(String sprintId);

    boolean existsBySprintIdAndTeamId(String sprintId, String teamId);

    java.util.Optional<TeamRegisterSprint> findBySprintIdAndTeamId(String sprintId, String teamId);

    @org.springframework.data.jpa.repository.Query("SELECT trs FROM TeamRegisterSprint trs JOIN UserRegisterTeam urt ON trs.team.id = urt.team.id WHERE urt.user.id = :userId")
    java.util.List<TeamRegisterSprint> findSprintsByUserId(
            @org.springframework.data.repository.query.Param("userId") Long userId);

    java.util.List<TeamRegisterSprint> findAllByTeamId(String teamId);
}
