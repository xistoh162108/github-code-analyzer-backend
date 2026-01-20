package com.backend.githubanalyzer.global.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient webClient;

    @Value("${app.webhook-url:}")
    private String webhookUrl;

    public void sendTeamRepoUrl(String teamName, String repoUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook URL is not configured. Skipping notification for team: {}", teamName);
            return;
        }

        Map<String, String> payload = Map.of(
                "teamName", teamName,
                "repositoryUrl", repoUrl,
                "event", "team_sprint_registration_approved");

        webClient.post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Successfully sent webhook for team: {}", teamName))
                .doOnError(error -> log.error("Failed to send webhook for team: {}. Error: {}", teamName,
                        error.getMessage()))
                .subscribe(); // Fire and forget (Async)
    }
}
