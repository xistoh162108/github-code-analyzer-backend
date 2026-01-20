package com.backend.githubanalyzer.security.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenValidationRequest {
    private String token;
}
