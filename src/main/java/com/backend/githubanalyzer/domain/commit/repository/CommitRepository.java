package com.backend.githubanalyzer.domain.commit.repository;

import com.backend.githubanalyzer.domain.commit.entity.Commit;
import com.backend.githubanalyzer.domain.commit.entity.CommitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, CommitId> {

        List<Commit> findAllByRepositoryId(String repoId);

        List<Commit> findAllByAuthorId(Long authorId);

        java.util.Optional<Commit> findById_CommitShaAndRepositoryId(String commitSha, String repoId);

        List<Commit> findAllByRepositoryIdAndId_BranchName(String repoId, String branchName);

        List<Commit> findAllByRepositoryIdOrderByCommittedAtDesc(String repoId);

        @Query("SELECT COUNT(DISTINCT c.id.commitSha) FROM Commit c WHERE c.repository.id = :repoId")
        long countUniqueCommitsByRepositoryId(@Param("repoId") String repoId);

        @Query("SELECT COUNT(DISTINCT c.id.branchName) FROM Commit c WHERE c.repository.id = :repoId")
        long countUniqueBranchesByRepositoryId(@Param("repoId") String repoId);

        long countByAuthorId(Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.author.id = :authorId")
        Long sumTotalScoreByAuthorId(@Param("authorId") Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end")
        Long sumTotalScoreByRepoAndTime(@Param("repoId") String repoId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end")
        long countByRepoAndTime(@Param("repoId") String repoId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT DISTINCT c.author FROM Commit c WHERE c.repository.id = :repoId")
        List<com.backend.githubanalyzer.domain.user.entity.User> findDistinctAuthorByRepositoryId(
                        @Param("repoId") String repoId);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id IN :repoIds AND c.author.id = :authorId")
        long countByRepositoryIdInAndAuthorId(@Param("repoIds") List<String> repoIds, @Param("authorId") Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id IN :repoIds AND c.author.id = :authorId")
        Long sumTotalScoreByRepositoryIdInAndAuthorId(@Param("repoIds") List<String> repoIds,
                        @Param("authorId") Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.author.id = :authorId AND c.committedAt BETWEEN :start AND :end")
        Long sumTotalScoreByRepoAndTimeAndAuthor(@Param("repoId") String repoId,
                        @Param("authorId") Long authorId,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.author.id = :authorId AND c.committedAt BETWEEN :start AND :end")
        long countByRepoAndTimeAndAuthor(@Param("repoId") String repoId,
                        @Param("authorId") Long authorId,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);
}
