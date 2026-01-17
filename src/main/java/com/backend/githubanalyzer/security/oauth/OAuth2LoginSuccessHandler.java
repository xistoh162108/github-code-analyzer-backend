package com.backend.githubanalyzer.security.oauth;

import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import com.backend.githubanalyzer.security.dto.JwtToken;
import com.backend.githubanalyzer.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String githubId = String.valueOf(oauth2User.getAttributes().get("id"));
        String login = (String) oauth2User.getAttributes().get("login");
        String name = (String) oauth2User.getAttributes().getOrDefault("name", login);
        String email = (String) oauth2User.getAttributes().get("email");

        // User Persistence
        Optional<User> userOptional = userRepository.findByGithubId(githubId);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setName(name);
            user.setEmail(email != null ? email : user.getEmail());
            user.setGithubLogin(login);
            userRepository.save(user);
        } else {
            // Fallback: Check if a user with this email already exists
            Optional<User> emailUserOptional = (email != null)
                    ? userRepository.findByEmail(email)
                    : Optional.empty();

            if (emailUserOptional.isPresent()) {
                user = emailUserOptional.get();
                user.setGithubId(githubId);
                user.setGithubLogin(login);
                user.setName(name);
                userRepository.save(user);
            } else {
                user = User.builder()
                        .githubId(githubId)
                        .githubLogin(login)
                        .name(name)
                        .email(email != null ? email : githubId + "@github.com")
                        .build();
                userRepository.save(user);
            }
        }

        // Issue JWT representing our internal user
        String subject = user.getEmail();
        Authentication jwtAuth = new UsernamePasswordAuthenticationToken(subject, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        JwtToken token = jwtTokenProvider.generateToken(jwtAuth);

        // Backend-only test redirect: return JWT as JSON via a specialized endpoint
        // For simplicity, we can redirect to a URL that the frontend or the user can
        // inspect
        // The user specifically asked for backend testing without frontend.
        response.sendRedirect("/api/auth/test/success?accessToken=" + token.getAccessToken() + "&refreshToken="
                + token.getRefreshToken());
    }
}