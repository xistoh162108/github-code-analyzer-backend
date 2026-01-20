package com.backend.githubanalyzer.domain.sprint.repository;

import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByIsPrivateFalse();

    List<Sprint> findAllByNameContainingIgnoreCase(String name);

    List<Sprint> findByManagerId(Long managerId);
}
