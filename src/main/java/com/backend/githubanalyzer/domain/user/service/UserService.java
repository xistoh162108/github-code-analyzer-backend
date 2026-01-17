package com.backend.githubanalyzer.domain.user.service;

import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
        private final UserRepository userRepository;

        public UserService(UserRepository userRepository) {
                this.userRepository = userRepository;
        }

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
                                                .build();
                        }
                }
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
}
