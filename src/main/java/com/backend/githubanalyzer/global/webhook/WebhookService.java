package com.backend.githubanalyzer.global.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient webClient;

    @Value("${app.webhook-url:}")
    private String webhookUrl;

    public record WebhookResult(boolean success, String url, String errorMessage) {
    }

    public WebhookResult sendTeamRepoUrl(String teamName, String repoUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook URL is not configured. Skipping notification for team: {}", teamName);
            return new WebhookResult(false, null, "Webhook URL not configured");
        }

        Map<String, String> payload = Map.of(
                "teamName", teamName,
                "repositoryUrl", repoUrl,
                "event", "team_sprint_registration_approved");

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5)); // Synchronous wait for result

            log.info("Successfully sent webhook for team: {}", teamName);
            return new WebhookResult(true, webhookUrl, null);
        } catch (Exception e) {
            log.error("Failed to send webhook for team: {}. Error: {}", teamName, e.getMessage());
            return new WebhookResult(false, webhookUrl, e.getMessage());
        }
    }
}
