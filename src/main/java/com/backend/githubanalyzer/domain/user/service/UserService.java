package com.backend.githubanalyzer.domain.user.service;

import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
        private final UserRepository userRepository;

        public UserService(UserRepository userRepository) {
                this.userRepository = userRepository;
        }

        public UserResponse getMe(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
                return UserResponse.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .createdAt(user.getCreatedAt())
                                .build();
        }
}
