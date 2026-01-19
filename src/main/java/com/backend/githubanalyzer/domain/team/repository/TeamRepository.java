package com.backend.githubanalyzer.domain.team.repository;

import com.backend.githubanalyzer.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, String> {

    java.util.List<Team> findAllByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);

    Optional<Team> findByName(String name);
}
