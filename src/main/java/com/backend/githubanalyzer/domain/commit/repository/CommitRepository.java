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

        long countByAnalysisStatus(com.backend.githubanalyzer.domain.commit.entity.AnalysisStatus analysisStatus);

        List<Commit> findAllByAuthorIdOrderByCommittedAtDesc(Long authorId);

        List<Commit> findAllByAuthorIdAndRepositoryIdOrderByCommittedAtDesc(Long authorId, String repoId);

        java.util.List<Commit> findAllById_CommitSha(String commitSha);

        java.util.List<Commit> findAllById_CommitShaAndRepositoryId(String commitSha, String repoId);

        java.util.List<Commit> findAllById_CommitShaInAndRepositoryId(List<String> commitShas, String repoId);

        boolean existsById_CommitSha(String commitSha);

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

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.author.id = :authorId")
        Long sumTotalScoreByAuthorId(@Param("authorId") Long authorId);

        @Query(value = "SELECT DATE(committed_at) as date, COUNT(*) as count FROM commits WHERE author_id = :authorId GROUP BY DATE(committed_at)", nativeQuery = true)
        List<Object[]> countCommitsByDayForUser_Native(@Param("authorId") Long authorId);

        List<Commit> findByMessageContainingIgnoreCase(String keyword);

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

        @Query("SELECT SUM(c.totalScore) FROM Commit c WHERE c.repository.id = :repoId AND c.committedAt BETWEEN :start AND :end")
        Long sumTotalScoreByRepo(@Param("repoId") String repoId,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT DISTINCT c.committedAt FROM Commit c WHERE c.author.id = :authorId ORDER BY c.committedAt DESC")
        java.util.List<java.time.LocalDateTime> findDistinctCommittedAtByAuthorId(@Param("authorId") Long authorId);

        boolean existsByAuthorIdAndRepositoryIdIn(Long authorId, java.util.List<String> repositoryIds);

        // --- Ranking Queries ---

        // 1. Top Commits Global
        @Query("SELECT c FROM Commit c WHERE c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' ORDER BY c.totalScore DESC")
        List<Commit> findTopCommitsGlobal(@Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 2. Top Commits by Repos (Team/Sprint)
        @Query("SELECT c FROM Commit c WHERE c.repository.id IN :repoIds AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' ORDER BY c.totalScore DESC")
        List<Commit> findTopCommitsByRepos(@Param("repoIds") List<String> repoIds,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 3. Top Commits by User
        @Query("SELECT c FROM Commit c WHERE c.author.id = :userId AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' ORDER BY c.totalScore DESC")
        List<Commit> findTopCommitsByUser(@Param("userId") Long userId,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 4. User Ranking Global (Aggregated Score)
        @Query("SELECT c.author as user, SUM(c.totalScore) as totalScore FROM Commit c " +
                        "WHERE c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' " +
                        "GROUP BY c.author ORDER BY SUM(c.totalScore) DESC")
        List<Object[]> findUserRankingGlobal(@Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 5. User Ranking by Repos (For Team/Sprint)
        @Query("SELECT c.author as user, SUM(c.totalScore) as totalScore FROM Commit c " +
                        "WHERE c.repository.id IN :repoIds AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' "
                        +
                        "GROUP BY c.author ORDER BY SUM(c.totalScore) DESC")
        List<Object[]> findUserRankingByRepos(@Param("repoIds") List<String> repoIds,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 6. Top Commits by Authors (Team Scope)
        @Query("SELECT c FROM Commit c WHERE c.author.id IN :authorIds AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' ORDER BY c.totalScore DESC")
        List<Commit> findTopCommitsByAuthors(@Param("authorIds") List<Long> authorIds,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);

        // 7. User Ranking by Authors (Team Scope)
        @Query("SELECT c.author as user, SUM(c.totalScore) as totalScore FROM Commit c " +
                        "WHERE c.author.id IN :authorIds AND c.committedAt BETWEEN :start AND :end AND c.analysisStatus = 'COMPLETED' "
                        +
                        "GROUP BY c.author ORDER BY SUM(c.totalScore) DESC")
        List<Object[]> findUserRankingByAuthors(@Param("authorIds") List<Long> authorIds,
                        @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end,
                        org.springframework.data.domain.Pageable pageable);
        @Query("SELECT DISTINCT c.id.branchName FROM Commit c WHERE c.repository.id = :repoId")
        List<String> findDistinctBranchNamesByRepositoryId(@Param("repoId") String repoId);

        long countById_RepoIdAndId_BranchName(String repoId, String branchName);

        List<Commit> findAllByRepositoryIdAndId_BranchNameOrderByCommittedAtDesc(String repoId, String branchName);

        Commit findFirstById_RepoIdAndId_BranchNameOrderByCommittedAtDesc(String repoId, String branchName);

        // 8. Contributors Ranking by Repo (All Time)
        @Query("SELECT c.author as user, SUM(c.totalScore) as totalScore FROM Commit c " +
                        "WHERE c.repository.id = :repoId AND c.analysisStatus = 'COMPLETED' " +
                        "GROUP BY c.author ORDER BY SUM(c.totalScore) DESC")
        List<Object[]> findContributorsWithScore(@Param("repoId") String repoId);
}
