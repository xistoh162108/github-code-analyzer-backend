package com.backend.githubanalyzer.domain.user.service;

import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
        private final UserRepository userRepository;
        private final CommitRepository commitRepository;

        @Transactional(readOnly = true)
        public UserResponse getMe(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

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
                long count = commitRepository.countByAuthorId(user.getId());
                Long sum = commitRepository.sumTotalScoreByAuthorId(user.getId());

                user.setCommitCount(count);
                user.setScore(sum != null ? sum : 0L);
                userRepository.save(user);
        }
}
