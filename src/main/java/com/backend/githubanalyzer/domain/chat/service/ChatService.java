package com.backend.githubanalyzer.domain.chat.service;

import com.backend.githubanalyzer.domain.chat.dto.ChatRequest;
import com.backend.githubanalyzer.domain.chat.dto.ChatResponse;
import com.backend.githubanalyzer.infra.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAiClient;

    public ChatResponse processChat(ChatRequest request) {
        String systemPrompt = constructSystemPrompt(request);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (request.messages() != null) {
            messages.addAll(request.messages().stream()
                    .map(msg -> Map.of("role", msg.role(), "content", msg.content()))
                    .collect(Collectors.toList()));
        }

        try {
            String aiResponse = openAiClient.chat(messages).block();
            return new ChatResponse(aiResponse);
        } catch (Exception e) {
            log.error("Chat processing failed", e);
            throw new IllegalStateException("Failed to process chat request: " + e.getMessage());
        }
    }

    private String constructSystemPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert software engineer assistant analyzing a GitHub repository.\n");
        sb.append("You have access to the following commit history context:\n\n");

        if (request.commits() != null && !request.commits().isEmpty()) {
            sb.append("Recent Commits:\n");
            for (ChatRequest.CommitContext commit : request.commits()) {
                String sha = commit.sha() != null ? commit.sha() : "Unknown";
                String shortSha = sha.length() > 7 ? sha.substring(0, 7) : sha;
                sb.append(String.format("- [%s] %s (Author: %s, Time: %s) +%d/-%d\n",
                        shortSha,
                        commit.message(),
                        commit.author(),
                        commit.time(),
                        commit.additions(),
                        commit.deletions()));
            }
            sb.append("\n");
        }

        if (request.selectedCommit() != null) {
            sb.append("The user is currently focusing on commit: ").append(request.selectedCommit()).append("\n");
        }
        
        sb.append("Answer the user's questions based on this context. Be concise and technical. Format your response in Markdown.");
        return sb.toString();
    }
}
