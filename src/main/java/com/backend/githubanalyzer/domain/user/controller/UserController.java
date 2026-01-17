package com.backend.githubanalyzer.domain.user.controller;

import com.backend.githubanalyzer.domain.user.dto.UserResponse;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.security.jwt.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        String email = JwtUtil.getCurrentUsername();
        return userService.getMe(email);
    }
}
