package com.backend.githubanalyzer.domain.notification.dto;

import com.backend.githubanalyzer.domain.notification.entity.Notification;
import com.backend.githubanalyzer.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "알림 응답 DTO")
public class NotificationResponse {
    @io.swagger.v3.oas.annotations.media.Schema(description = "알림 ID", example = "1")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(description = "알림 유형 (SPRINT_*, TEAM_*, ANALYSIS_SUMMARY 등)", example = "SPRINT_JOIN")
    private NotificationType type;

    @io.swagger.v3.oas.annotations.media.Schema(description = "알림 메시지", example = "Your team 'Alpha' has joined the sprint 'Q1 Sprint'.")
    private String message;

    @io.swagger.v3.oas.annotations.media.Schema(description = "읽음 여부", example = "false")
    private boolean isRead;

    @io.swagger.v3.oas.annotations.media.Schema(description = "생성 일시", example = "2024-01-20T10:00:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
