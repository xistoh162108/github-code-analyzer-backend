package com.backend.githubanalyzer.domain.chat.controller;

import com.backend.githubanalyzer.domain.chat.dto.ChatRequest;
import com.backend.githubanalyzer.domain.chat.dto.ChatResponse;
import com.backend.githubanalyzer.domain.chat.service.ChatService;
import com.backend.githubanalyzer.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Chat API")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Chat with AI", description = "OpenAI를 이용해 코드/커밋에 대한 질의응답을 수행합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.processChat(request)));
    }
}
