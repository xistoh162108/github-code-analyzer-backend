package com.backend.githubanalyzer.global.exception;

import com.backend.githubanalyzer.global.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        // "User not found" often comes as IllegalArgumentException in this project
        // Mapping it to 404/400 depending on semantic, but 401/404 is better for User
        // lookup failure.
        // For general IllegalArgument, 400 is standard.
        // Given the context of "getCurrentUser" failing, let's treat it as a Bad
        // Request or Unauthorized if we could distinguish.
        // Simple approach: 400 Bad Request with message.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        e.printStackTrace(); // Keep internal logs
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal Server Error"));
    }
}
