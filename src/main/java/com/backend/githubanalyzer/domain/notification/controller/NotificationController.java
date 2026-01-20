package com.backend.githubanalyzer.domain.notification.controller;

import com.backend.githubanalyzer.domain.notification.service.NotificationService;
import com.backend.githubanalyzer.domain.notification.dto.NotificationResponse;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notification", description = "실시간 알림 API (SSE)")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @io.swagger.v3.oas.annotations.Operation(summary = "Subscribe SSE (알림 스트림 연결)", description = "실시간 알림을 수신하기 위해 SSE 스트림에 연결합니다.<br>"
            +
            "**Usage**: EventSource 또는 HTTP 클라이언트를 사용하여 연결.<br>" +
            "**Authentication**: Authorization Header (Bearer Token) 필수.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        User user = getCurrentUser();
        return notificationService.subscribe(user.getId());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Notifications (알림 목록 조회)", description = "과거 알림 내역을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        User user = getCurrentUser();
        return ResponseEntity.ok(notificationService.getNotifications(user.getId()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Mark as Read (읽음 처리)", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
