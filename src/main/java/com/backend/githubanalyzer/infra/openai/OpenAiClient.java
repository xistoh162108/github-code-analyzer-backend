package com.backend.githubanalyzer.infra.openai;

import com.backend.githubanalyzer.infra.openai.dto.OpenAiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public Mono<OpenAiAnalysisResponse> analyzeCommit(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Mono.error(new IllegalStateException("OpenAI API Key is not configured."));
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of("type", "json_object"));

        return webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                        return objectMapper.readValue(content, OpenAiAnalysisResponse.class);
                    } catch (Exception e) {
                        log.error("Failed to parse OpenAI response", e);
                        throw new RuntimeException("AI response parsing failed", e);
                    }
                });
    }
}
