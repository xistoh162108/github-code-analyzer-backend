package com.backend.githubanalyzer.infra.github.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Controller
@RequestMapping("/api/auth/github/installation")
@RequiredArgsConstructor
@Tag(name = "GitHub Installation", description = "GitHub App 설치 관련 엔드포인트")
public class GithubInstallationController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Operation(summary = "GitHub App 설치 리다이렉트 핸들러", description = "GitHub App 설치 후 GitHub에서 리다이렉트되는 목적지입니다.")
    @GetMapping
    public void handleInstallationRedirect(
            @RequestParam("installation_id") String installationId,
            @RequestParam("setup_action") String setupAction,
            HttpServletResponse response) throws IOException {

        log.info("Received GitHub App installation redirect. ID: {}, Action: {}", installationId, setupAction);

        String redirectUrl = String.format("%s/auth/callback?type=installation&installation_id=%s&setup_action=%s",
                frontendUrl, installationId, setupAction);

        log.info("Redirecting user to frontend: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
