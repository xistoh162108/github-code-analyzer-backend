package com.backend.githubanalyzer.domain.commit.repository;

import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.entity.CommitId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, CommitId> {

    List<Commit> findAllByRepositoryId(String repoId);

    List<Commit> findAllByAuthorId(Long authorId);

    List<Commit> findAllByRepositoryIdAndId_BranchName(String repoId, String branchName);

    List<Commit> findAllByRepositoryIdOrderByCommittedAtDesc(String repoId);
}
