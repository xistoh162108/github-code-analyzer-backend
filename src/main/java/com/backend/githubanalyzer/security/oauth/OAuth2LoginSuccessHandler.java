package com.backend.githubanalyzer.security.oauth;

import com.backend.githubanalyzer.domain.sync.service.GithubSyncService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.service.UserService;
import com.backend.githubanalyzer.security.dto.JwtToken;
import com.backend.githubanalyzer.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final GithubSyncService githubSyncService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        String location = (String) attributes.get("location");
        String company = (String) attributes.get("company");
        Integer publicRepos = (Integer) attributes.get("public_repos");

        // Use Transactional service for syncing profile
        User user = userService.syncUserFromGithub(githubId, username, email, avatarUrl, location, company,
                publicRepos);

        // Fetch GitHub Access Token for synchronization
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());
        String githubAccessToken = client.getAccessToken().getTokenValue();

        // Trigger background data sync
        githubSyncService.syncAllData(user.getId(), githubAccessToken);

        // Issue JWT
        String subject = user.getEmail();
        Authentication jwtAuth = new UsernamePasswordAuthenticationToken(subject, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        JwtToken token = jwtTokenProvider.generateToken(jwtAuth);

        response.sendRedirect("/api/auth/test/success?accessToken=" + token.getAccessToken() + "&refreshToken="
                + token.getRefreshToken());
    }
}