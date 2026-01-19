package com.backend.githubanalyzer.infra.github.service;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubAppService {

    @Value("${github.app.id}")
    private String appId;

    @Value("${github.app.private-key-path}")
    private String privateKeyPath;

    private final WebClient webClient;
    private final ResourceLoader resourceLoader;

    /**
     * Generates a GitHub App JWT for initial App-level authentication.
     */
    public String createGithubAppJwt() {
        try {
            PrivateKey privateKey = loadPrivateKey();
            Instant now = Instant.now();

            return Jwts.builder()
                    .header()
                    .add("alg", "RS256")
                    .and()
                    .issuedAt(Date.from(now.minus(1, ChronoUnit.MINUTES)))
                    .expiration(Date.from(now.plus(10, ChronoUnit.MINUTES)))
                    .issuer(appId)
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate GitHub App JWT", e);
            throw new RuntimeException("GitHub Authentication Failure", e);
        }
    }

    /**
     * Requests an Installation Access Token for a specific installation (user or
     * organization).
     * Cached for 55 minutes (GitHub tokens usually last 60 minutes).
     */
    @Cacheable(value = "githubInstallationTokens", key = "#installationId")
    public String getInstallationToken(String installationId) {
        log.info("Requesting new installation token for ID: {}", installationId);
        String jwt = createGithubAppJwt();

        Map<String, Object> response = webClient.post()
                .uri("https://api.github.com/app/installations/" + installationId + "/access_tokens")
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("token")) {
            return (String) response.get("token");
        }

        throw new RuntimeException("Failed to fetch GitHub Installation Token");
    }

    /**
     * Finds the installation ID for a specific user using GitHub App JWT.
     * Endpoint: GET /users/{username}/installation
     */
    public String getInstallationIdByUser(String username) {
        log.info("Looking up installation ID for user: {}", username);
        String jwt = createGithubAppJwt();

        try {
            Map<String, Object> response = webClient.get()
                    .uri("https://api.github.com/users/" + username + "/installation")
                    .header("Authorization", "Bearer " + jwt)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                return String.valueOf(response.get("id"));
            }
        } catch (Exception e) {
            log.warn("No installation found for user {}: {}", username, e.getMessage());
        }
        return null;
    }

    private PrivateKey loadPrivateKey() throws Exception {
        log.info("Loading GitHub App Private Key from: {}", privateKeyPath);

        String location = privateKeyPath;
        if (location.startsWith("/")) {
            location = "file:" + location;
        }

        Resource resource = resourceLoader.getResource(location);
        try (java.io.Reader reader = new java.io.InputStreamReader(resource.getInputStream(),
                StandardCharsets.US_ASCII);
                org.bouncycastle.openssl.PEMParser pemParser = new org.bouncycastle.openssl.PEMParser(reader)) {

            Object object = pemParser.readObject();
            org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                    .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                return converter.getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Unknown key format: " + object.getClass());
            }
        }
    }
}
