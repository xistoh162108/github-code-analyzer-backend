package com.backend.githubanalyzer.infra.github;

import com.backend.githubanalyzer.infra.github.dto.GithubBranchResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubCommitResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubRepoResponse;
import com.backend.githubanalyzer.infra.github.dto.GithubUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated service for GitHub API interaction.
 * Handles HTTP communication, pagination, and provides hooks for ETag
 * management.
 */
@Slf4j
@Service
public class GithubApiService {

    private final WebClient webClient;
    private final com.backend.githubanalyzer.global.monitor.MetricsService metricsService;

    public GithubApiService(WebClient.Builder webClientBuilder,
            com.backend.githubanalyzer.global.monitor.MetricsService metricsService) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com")
                .filter((request, next) -> next.exchange(request).doOnNext(response -> {
                    try {
                        String limit = response.headers().asHttpHeaders().getFirst("X-RateLimit-Limit");
                        String remaining = response.headers().asHttpHeaders().getFirst("X-RateLimit-Remaining");
                        if (limit != null && remaining != null) {
                            metricsService.updateGithubRateLimits(Long.parseLong(limit), Long.parseLong(remaining));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse GitHub rate limit headers", e);
                    }
                }))
                .build();
        this.metricsService = metricsService;
    }

    public record GithubResponse<T>(List<T> data, String etag, boolean notModified) {
    }

    public Mono<GithubResponse<GithubRepoResponse>> fetchUserRepositories(String accessToken, String etag) {
        return fetchAllPagesWrapped("/user/repos?sort=updated&per_page=100", accessToken, etag,
                GithubRepoResponse.class);
    }

    public Mono<GithubUserResponse> fetchUserByUsername(String username, String accessToken) {
        metricsService.incrementExternalRequest("github");
        return webClient.get()
                .uri("/users/{username}", username)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GithubUserResponse.class);
    }

    public Mono<GithubResponse<GithubBranchResponse>> fetchBranches(String owner, String repo, String accessToken,
            String etag) {
        return fetchAllPagesWrapped(String.format("/repos/%s/%s/branches?per_page=100", owner, repo), accessToken, etag,
                GithubBranchResponse.class);
    }

    public Mono<GithubResponse<GithubCommitResponse>> fetchCommits(String owner, String repo, String branch,
            String accessToken, LocalDateTime since, String etag) {
        String url = String.format("/repos/%s/%s/commits?sha=%s&per_page=100", owner, repo, branch);
        if (since != null) {
            String sinceStr = since.format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
            url += "&since=" + sinceStr;
        }
        return fetchAllPagesWrapped(url, accessToken, etag, GithubCommitResponse.class);
    }

    private <T> Mono<GithubResponse<T>> fetchAllPagesWrapped(String url, String accessToken, String etag,
            Class<T> clazz) {
        metricsService.incrementExternalRequest("github");
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken);

        if (etag != null) {
            request.header("If-None-Match", etag);
        }

        return request.exchangeToMono(response -> {
            if (response.statusCode().value() == 304) {
                return Mono.just(new GithubResponse<T>(List.of(), etag, true));
            }

            String newEtag = response.headers().asHttpHeaders().getFirst("ETag");
            return response.bodyToFlux(clazz)
                    .collectList()
                    .flatMap(list -> {
                        String nextUrl = extractNextUrl(response);
                        if (nextUrl != null) {
                            // Recursively fetch next pages
                            return fetchAllPagesRecursive(nextUrl, accessToken, clazz)
                                    .map(nextList -> {
                                        list.addAll(nextList);
                                        return new GithubResponse<T>(list, newEtag, false);
                                    });
                        }
                        return Mono.just(new GithubResponse<T>(list, newEtag, false));
                    });
        });
    }

    private <T> Mono<List<T>> fetchAllPagesRecursive(String url, String accessToken, Class<T> clazz) {
        metricsService.incrementExternalRequest("github");
        return webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .exchangeToMono(response -> response.bodyToFlux(clazz)
                        .collectList()
                        .flatMap(list -> {
                            String nextUrl = extractNextUrl(response);
                            if (nextUrl != null) {
                                return fetchAllPagesRecursive(nextUrl, accessToken, clazz)
                                        .map(nextList -> {
                                            list.addAll(nextList);
                                            return list;
                                        });
                            }
                            return Mono.just(list);
                        }));
    }

    public Mono<GithubCommitResponse> fetchCommitDetail(String owner, String repo, String sha, String accessToken) {
        metricsService.incrementExternalRequest("github");
        return webClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GithubCommitResponse.class);
    }

    public Mono<java.util.Map<String, Long>> fetchRepositoryLanguages(String owner, String repo, String accessToken) {
        metricsService.incrementExternalRequest("github");
        return webClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Long>>() {
                });
    }

    public Mono<Long> fetchBranchCommitCount(String owner, String repo, String branch, String accessToken) {
        metricsService.incrementExternalRequest("github");
        String url = String.format("/repos/%s/%s/commits?sha=%s&per_page=1", owner, repo, branch);
        
        return webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .exchangeToMono(response -> {
                    String linkHeader = response.headers().asHttpHeaders().getFirst("Link");
                    if (linkHeader != null) {
                        String lastPage = extractLastPage(linkHeader);
                        if (lastPage != null) {
                            try {
                                return Mono.just(Long.parseLong(lastPage));
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse last page number: {}", lastPage);
                            }
                        }
                    }
                    // If no Link header or failed to parse, fallback to 1 (if success) or 0
                    // But actually, if there is only 1 page, Link header might be missing or different.
                    // If 200 OK, at least 1 commit.
                    if (response.statusCode().is2xxSuccessful()) {
                         // We could try checking the body size, but for now safe fallback is 1 if successful response
                         // However, for precise count on small repos, we might need to check if list is empty.
                         // But for per_page=1, if we get a result, count is at least 1.
                         return Mono.just(1L);
                    }
                    return Mono.just(0L);
                });
    }

    private String extractLastPage(String linkHeader) {
        if (linkHeader == null) return null;
        // Example: <https://api.github.com/...&page=2>; rel="next", <https://api.github.com/...&page=5>; rel="last"
        Pattern pattern = Pattern.compile("[&?]page=(\\d+)[^>]*>;\\s*rel=\"last\"");
        Matcher matcher = pattern.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractNextUrl(ClientResponse response) {
        String linkHeader = response.headers().asHttpHeaders().getFirst("Link");
        if (linkHeader == null)
            return null;

        Pattern pattern = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
        Matcher matcher = pattern.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
