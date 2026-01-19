package com.backend.githubanalyzer.domain.team.repository;

import com.backend.githubanalyzer.domain.team.entity.UserRegisterTeam;
import com.backend.githubanalyzer.domain.team.entity.UserRegisterTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRegisterTeamRepository extends JpaRepository<UserRegisterTeam, UserRegisterTeamId> {
    List<UserRegisterTeam> findByTeamId(String teamId);

    List<UserRegisterTeam> findByTeamIdAndStatus(String teamId, String status);

    long countByTeamId(String teamId);
}
