package com.backend.githubanalyzer.global.controller;

import com.backend.githubanalyzer.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "System", description = "시스템 상태 확인 API")
public class HealthController {

    private final DataSource dataSource;

    @Operation(summary = "Health Check (상태 확인)", description = "서버와 데이터베이스의 연결 상태를 확인합니다.")
    @GetMapping("/api/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("server", "UP");
        status.put("timestamp", LocalDateTime.now());

        boolean dbUp = false;
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1000)) {
                dbUp = true;
            }
        } catch (Exception e) {
            // Log error if needed
        }

        status.put("database", dbUp ? "UP" : "DOWN");

        if (dbUp) {
            return ResponseEntity.ok(ApiResponse.success("System is healthy.", status));
        } else {
            return ResponseEntity.status(503).body(ApiResponse.error("Database connection failed.", status));
        }
    }
}
