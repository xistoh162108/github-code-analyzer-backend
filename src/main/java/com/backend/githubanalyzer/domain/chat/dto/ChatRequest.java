package com.backend.githubanalyzer.domain.chat.dto;

import java.util.List;

public record ChatRequest(
    List<Message> messages,
    List<CommitContext> commits,
    String selectedCommit
) {
    public record Message(String role, String content) {}
    
    public record CommitContext(
        String sha,
        String message,
        String author,
        String time,
        int additions,
        int deletions
    ) {}
}
