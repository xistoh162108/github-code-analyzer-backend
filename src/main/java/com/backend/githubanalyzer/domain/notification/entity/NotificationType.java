package com.backend.githubanalyzer.domain.notification.entity;

public enum NotificationType {
    ANALYSIS_COMPLETED,
    ANALYSIS_FAILED,
    TERM_INVITE, // Deprecated or Keep? Assuming TEAM_INVITE
    TEAM_INVITE,
    SPRINT_ALERT,
    // New Types
    SPRINT_START,
    SPRINT_END,
    SPRINT_JOIN,
    SPRINT_BAN,
    SPRINT_RANK_UP,
    TEAM_JOIN_APPROVED,
    TEAM_BAN,
    TEAM_UPDATE,
    ANALYSIS_SUMMARY
}
