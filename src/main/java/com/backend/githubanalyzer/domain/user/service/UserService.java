package com.backend.githubanalyzer.domain.user.service;

import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.dto.UserUpdateRequest;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.commit.dto.CommitHeatmapResponse;
import com.backend.githubanalyzer.domain.contribution.repository.ContributionRepository;
import com.backend.githubanalyzer.domain.repository.dto.GithubRepositoryResponse;
import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.contribution.entity.Contribution;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
        private final UserRepository userRepository;
        private final CommitRepository commitRepository;
        private final ContributionRepository contributionRepository;

        @Transactional(readOnly = true)
        public UserResponse getMe(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with username: " + username));

                return UserResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .notifyEmail(user.getNotifyEmail())
                                .notifySprint(user.getNotifySprint())
                                .notifyWeekly(user.getNotifyWeekly())
                                .profileUrl(user.getProfileUrl())
                                .location(user.getLocation())
                                .publicRepos(user.getPublicRepos())
                                .company(user.getCompany())
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        @Transactional
        public UserResponse updateMe(String username, UserUpdateRequest request) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with username: " + username));

                if (request.getCompany() != null)
                        user.setCompany(request.getCompany());
                if (request.getLocation() != null)
                        user.setLocation(request.getLocation());
                if (request.getNotifyEmail() != null)
                        user.setNotifyEmail(request.getNotifyEmail());
                if (request.getNotifySprint() != null)
                        user.setNotifySprint(request.getNotifySprint());
                if (request.getNotifyWeekly() != null)
                        user.setNotifyWeekly(request.getNotifyWeekly());

                userRepository.save(user);
                return getMe(username);
        }

        @Transactional
        public User syncUserFromGithub(String githubId, String username, String email, String avatarUrl,
                        String location, String company, Integer publicRepos) {
                Optional<User> userOptional = userRepository.findByGithubId(githubId);
                User user;

                if (userOptional.isPresent()) {
                        user = userOptional.get();
                        user.setIsGhost(false); // Registered users are not ghosts
                        updateUserInfo(user, username, email, avatarUrl, location, company, publicRepos);
                } else {
                        Optional<User> usernameUserOptional = userRepository.findByUsername(username);
                        if (usernameUserOptional.isPresent()) {
                                user = usernameUserOptional.get();
                                user.setGithubId(githubId);
                                updateUserInfo(user, username, email, avatarUrl, location, company, publicRepos);
                        } else {
                                user = User.builder()
                                                .githubId(githubId)
                                                .username(username)
                                                .email(email != null ? email : githubId + "@github.com")
                                                .profileUrl(avatarUrl)
                                                .location(location)
                                                .company(company)
                                                .publicRepos(publicRepos)
                                                .isGhost(false)
                                                .build();
                        }
                }
                return userRepository.save(user);
        }

        public User findByGithubId(String githubId) {
                return userRepository.findByGithubId(githubId).orElse(null);
        }

        @Transactional
        public User getOrCreateGhostUser(String githubId, String username, String avatarUrl) {
                return userRepository.findByGithubId(githubId)
                                .map(user -> {
                                        if (user.getIsGhost() && avatarUrl != null && user.getProfileUrl() == null) {
                                                user.setProfileUrl(avatarUrl);
                                                return userRepository.save(user);
                                        }
                                        return user;
                                })
                                .orElseGet(() -> {
                                        User ghost = User.builder()
                                                        .githubId(githubId)
                                                        .username(username)
                                                        .email(null)
                                                        .profileUrl(avatarUrl)
                                                        .isGhost(true)
                                                        .notifySprint(false)
                                                        .notifyWeekly(false)
                                                        .build();
                                        return userRepository.save(ghost);
                                });
        }

        public User findById(Long id) {
                return userRepository.findById(id).orElse(null);
        }

        public Optional<User> findByUsername(String username) {
                return userRepository.findByUsername(username);
        }

        @Transactional
        public User save(User user) {
                return userRepository.save(user);
        }

        private void updateUserInfo(User user, String username, String email, String avatarUrl, String location,
                        String company, Integer publicRepos) {
                user.setUsername(username);
                if (email != null) {
                        user.setEmail(email);
                }
                user.setProfileUrl(avatarUrl);
                user.setLocation(location);
                user.setCompany(company);
                user.setPublicRepos(publicRepos);
        }

        @Transactional
        public void refreshUserStats(User user) {
                userRepository.findById(user.getId()).ifPresent(u -> {
                        long totalCount = commitRepository.countByAuthorId(user.getId());
                        long completedCount = commitRepository.countCompletedByAuthorId(user.getId());
                        Long totalCompletedScore = commitRepository.sumCompletedScoreByAuthorId(user.getId());

                        long avgScore = (completedCount > 0) ? (totalCompletedScore / completedCount) : 0L;

                        u.setCommitCount(totalCount);
                        u.setScore(avgScore);
                        // Formula: commit count * average score
                        u.setTotalScore(totalCount * avgScore);
                        userRepository.save(u);
                });
        }

        @Transactional
        public void deleteUser(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with username: " + username));
                // Handle cascade or manual cleanup if needed
                userRepository.delete(user);
        }

        @Transactional(readOnly = true)
        public List<CommitHeatmapResponse> getUserHeatmap(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with username: " + username));

                List<Object[]> results = commitRepository.countCommitsByDayForUser_Native(user.getId());
                return results.stream()
                                .map(row -> CommitHeatmapResponse.builder()
                                                .date(row[0].toString())
                                                .count(((Number) row[1]).longValue())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<GithubRepositoryResponse> getUserRepositories(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found with username: " + username));

                List<Contribution> contributions = contributionRepository.findAllByUserId(user.getId());
                return contributions.stream()
                                .map(Contribution::getRepository)
                                .map(this::toRepoDto)
                                .collect(Collectors.toList());
        }

        private GithubRepositoryResponse toRepoDto(GithubRepository repository) {
                return GithubRepositoryResponse.builder()
                                .id(repository.getId())
                                .reponame(repository.getReponame())
                                .repoUrl(repository.getRepoUrl())
                                .description(repository.getDescription())
                                .language(repository.getLanguage())
                                .size(repository.getSize())
                                .stars(repository.getStars())
                                .createdAt(repository.getCreatedAt())
                                .updatedAt(repository.getUpdatedAt())
                                .pushedAt(repository.getPushedAt())
                                .lastSyncAt(repository.getLastSyncAt())
                                .build();
        }
}
