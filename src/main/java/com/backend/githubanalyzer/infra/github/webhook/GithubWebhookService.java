package com.backend.githubanalyzer.infra.github.webhook;

import com.backend.githubanalyzer.domain.sync.service.GithubSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private final GithubSyncService githubSyncService;
    private final ObjectMapper objectMapper;

    // Placeholder for a system token if user tokens aren't available
    // In a real app, you'd store per-user tokens or use a GitHub App installation
    // token.
    private static final String SYSTEM_PAT = System.getenv("GITHUB_TOKEN");

    public void processPayload(String eventType, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);

        switch (eventType) {
            case "push":
                handlePush(root);
                break;
            case "repository":
                handleRepository(root);
                break;
            case "member":
                handleMember(root);
                break;
            default:
                log.info("Unhandled GitHub event type: {}", eventType);
        }
    }

    private void handlePush(JsonNode root) {
        String ref = root.path("ref").asText(); // e.g., refs/heads/main
        if (ref == null || !ref.startsWith("refs/heads/"))
            return;

        String branchName = ref.replace("refs/heads/", "");
        String nodeId = root.path("repository").path("node_id").asText();

        log.info("Push detected on repo {} branch {}", nodeId, branchName);

        // We need a token. Using SYSTEM_PAT as fallback.
        // Ideally, we'd find the user who owned this repo and use their token.
        githubSyncService.syncSelective(nodeId, branchName, SYSTEM_PAT);
    }

    private void handleRepository(JsonNode root) {
        String action = root.path("action").asText();
        if ("created".equals(action) || "publicized".equals(action)) {
            JsonNode repoNode = root.path("repository");
            String nodeId = repoNode.path("node_id").asText();
            String repoName = repoNode.path("name").asText();
            log.info("New repository created/publicized: {} ({})", repoName, nodeId);

            // Attempt to find the sender (user who created it) in our system
            String senderId = root.path("sender").path("id").asText();
            com.backend.githubanalyzer.domain.user.entity.User user = githubSyncService.findUserByGithubId(senderId);

            if (user != null) {
                // Mapping the JSON to DTO for the existing sync method
                try {
                    com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse repoDto = objectMapper.treeToValue(
                            repoNode, com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse.class);
                    githubSyncService.syncSingleRepo(user, repoDto, SYSTEM_PAT);
                } catch (Exception e) {
                    log.error("Failed to parse repo DTO from webhook: {}", e.getMessage());
                }
            } else {
                log.warn("Sender {} not found in system, skipping auto-sync", senderId);
            }
        }
    }

    private void handleMember(JsonNode root) {
        String action = root.path("action").asText();
        if ("added".equals(action)) {
            String login = root.path("member").path("login").asText();
            String repoNodeId = root.path("repository").path("node_id").asText();
            log.info("New member {} added to repo {}", login, repoNodeId);

            // Trigger sync for this repo to update contributions
            githubSyncService.syncSelective(repoNodeId, null, SYSTEM_PAT);
        }
    }
}
