package com.backend.githubanalyzer.infra.github.webhook;

import com.backend.githubanalyzer.domain.sync.dto.SyncJobRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private final com.backend.githubanalyzer.infra.redis.SyncQueueProducer syncQueueProducer;
    private final ObjectMapper objectMapper;

    public void processPayload(String eventType, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String installationId = root.path("installation").path("id").asText();

        if (installationId == null || installationId.isEmpty()) {
            log.warn("Github event {} received without installation id", eventType);
            return;
        }

        switch (eventType) {
            case "push":
                handlePush(root, installationId);
                break;
            case "installation":
                handleInstallation(root, installationId);
                break;
            case "installation_repositories":
                handleInstallationRepositories(root, installationId);
                break;
            case "repository":
                handleRepository(root, installationId);
                break;
            default:
                log.info("Unhandled GitHub event type: {}", eventType);
        }
    }

    private void handlePush(JsonNode root, String installationId) {
        String ref = root.path("ref").asText();
        if (ref == null || !ref.startsWith("refs/heads/"))
            return;

        String branchName = ref.replace("refs/heads/", "");
        String nodeId = root.path("repository").path("node_id").asText();

        syncQueueProducer.pushJob(SyncJobRequest.builder()
                .type(SyncJobRequest.JobType.PUSH)
                .installationId(installationId)
                .repositoryId(nodeId)
                .branchName(branchName)
                .build());
    }

    private void handleInstallation(JsonNode root, String installationId) {
        String action = root.path("action").asText();
        String senderId = root.path("sender").path("id").asText();

        log.info("GitHub App installation {}: {} by sender {}", action, installationId, senderId);

        if ("created".equals(action)) {
            // Trigger a sync job that will also associate the installationId with the user
            syncQueueProducer.pushJob(SyncJobRequest.builder()
                    .type(SyncJobRequest.JobType.INSTALLATION)
                    .installationId(installationId)
                    .githubLogin(root.path("sender").path("login").asText())
                    .build());
        } else if ("deleted".equals(action)) {
            syncQueueProducer.pushJob(SyncJobRequest.builder()
                    .type(SyncJobRequest.JobType.UNINSTALLATION)
                    .installationId(installationId)
                    .build());
        }
    }

    private void handleInstallationRepositories(JsonNode root, String installationId) {
        String action = root.path("action").asText();
        JsonNode reposAdded = root.path("repositories_added");

        if ("added".equals(action) && reposAdded.isArray()) {
            for (JsonNode repo : reposAdded) {
                String nodeId = repo.path("node_id").asText();
                syncQueueProducer.pushJob(SyncJobRequest.builder()
                        .type(SyncJobRequest.JobType.REPO_SYNC)
                        .installationId(installationId)
                        .repositoryId(nodeId)
                        .build());
            }
        }
    }

    private void handleRepository(JsonNode root, String installationId) {
        String action = root.path("action").asText();
        String nodeId = root.path("repository").path("node_id").asText();

        if ("deleted".equals(action)) {
            syncQueueProducer.pushJob(SyncJobRequest.builder()
                    .type(SyncJobRequest.JobType.REPOSITORY_DELETED)
                    .installationId(installationId)
                    .repositoryId(nodeId)
                    .build());
        }
    }
}
