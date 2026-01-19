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

        List<Commit> findAllByAuthorIdOrderByCommittedAtDesc(Long authorId);

        List<Commit> findAllByAuthorIdAndRepositoryIdOrderByCommittedAtDesc(Long authorId, String repoId);

        java.util.List<Commit> findAllById_CommitSha(String commitSha);

        java.util.Optional<Commit> findById_CommitShaAndRepositoryId(String commitSha, String repoId);

        List<Commit> findAllByRepositoryIdAndId_BranchName(String repoId, String branchName);

        List<Commit> findAllByRepositoryIdOrderByCommittedAtDesc(String repoId);

        @Query("SELECT COUNT(DISTINCT c.id.commitSha) FROM Commit c WHERE c.repository.id = :repoId")
        long countUniqueCommitsByRepositoryId(@Param("repoId") String repoId);

        @Query("SELECT COUNT(DISTINCT c.id.branchName) FROM Commit c WHERE c.repository.id = :repoId")
        long countUniqueBranchesByRepositoryId(@Param("repoId") String repoId);

        long countByAuthorId(Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.author.id = :authorId AND c.analysisStatus = 'COMPLETED'")
        Long sumCompletedScoreByAuthorId(@Param("authorId") Long authorId);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.author.id = :authorId AND c.analysisStatus = 'COMPLETED'")
        long countCompletedByAuthorId(@Param("authorId") Long authorId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.analysisStatus = 'COMPLETED'")
        Long sumCompletedScoreByRepositoryId(@Param("repoId") String repoId);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.analysisStatus = 'COMPLETED'")
        long countCompletedByRepositoryId(@Param("repoId") String repoId);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED'")
        Long sumCompletedScoreByRepoAndTime(@Param("repoId") String repoId,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED'")
        long countCompletedByRepoAndTime(@Param("repoId") String repoId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end")
        Long sumTotalScoreByRepoAndTime(@Param("repoId") String repoId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end")
        long countByRepoAndTime(@Param("repoId") String repoId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query(value = "SELECT DATE(committed_at) as date, COUNT(*) as count FROM commits WHERE author_id = :authorId GROUP BY DATE(committed_at)", nativeQuery = true)
        List<Object[]> countCommitsByDayForUser_Native(@Param("authorId") Long authorId);

        List<Commit> findByMessageContainingIgnoreCase(String keyword);
}
