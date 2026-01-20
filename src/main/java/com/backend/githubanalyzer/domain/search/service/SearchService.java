package com.backend.githubanalyzer.domain.search.service;

import com.backend.githubanalyzer.domain.search.dto.UnifiedSearchResponse;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRepository;
import com.backend.githubanalyzer.domain.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
        private final UserRepository userRepository;
        private final GithubRepositoryRepository repoRepository;
        private final TeamRepository teamRepository;
        private final SprintRepository sprintRepository;
        private final com.backend.githubanalyzer.domain.commit.repository.CommitRepository commitRepository;

        @Transactional(readOnly = true)
        public UnifiedSearchResponse search(String query, String type) {
                UnifiedSearchResponse.UnifiedSearchResponseBuilder builder = UnifiedSearchResponse.builder();

                System.out.println("[SearchService] Querying for: " + query + " (type: " + type + ")");

                if (type == null || type.equals("ALL") || type.equals("USER")) {
                        var users = userRepository.findAllByUsernameContainingIgnoreCase(query);
                        System.out.println("[SearchService] Found " + users.size() + " users");
                        builder.users(users.stream()
                                        .map(u -> UnifiedSearchResponse.UserSearchResult.builder()
                                                        .id(u.getId())
                                                        .username(u.getUsername())
                                                        .profileUrl(u.getProfileUrl())
                                                        .build())
                                        .collect(Collectors.toList()));
                }

                if (type == null || type.equals("ALL") || type.equals("REPO") || type.equals("REPOSITORY")) {
                        var repos = repoRepository.findAllByReponameContainingIgnoreCase(query);
                        System.out.println("[SearchService] Found " + repos.size() + " repositories");
                        builder.repositories(repos.stream()
                                        .map(r -> UnifiedSearchResponse.RepoSearchResult.builder()
                                                        .id(r.getId())
                                                        .reponame(r.getReponame())
                                                        .description(r.getDescription())
                                                        .build())
                                        .collect(Collectors.toList()));
                }

                if (type == null || type.equals("ALL") || type.equals("TEAM")) {
                        builder.teams(teamRepository.findAllByNameContainingIgnoreCase(query).stream()
                                        .map(t -> UnifiedSearchResponse.TeamSearchResult.builder()
                                                        .id(t.getId())
                                                        .name(t.getName())
                                                        .description(t.getDescription())
                                                        .build())
                                        .collect(Collectors.toList()));
                }

                if (type == null || type.equals("ALL") || type.equals("SPRINT")) {
                        builder.sprints(sprintRepository.findAllByNameContainingIgnoreCase(query).stream()
                                        .map(s -> UnifiedSearchResponse.SprintSearchResult.builder()
                                                        .id(s.getId())
                                                        .name(s.getName())
                                                        .description(s.getDescription())
                                                        .build())
                                        .collect(Collectors.toList()));
                }

                if (type == null || type.equals("ALL") || type.equals("COMMIT")) {
                        builder.commits(commitRepository.findByMessageContainingIgnoreCase(query).stream()
                                        .map(c -> UnifiedSearchResponse.CommitSearchResult.builder()
                                                        .sha(c.getId().getCommitSha())
                                                        .message(c.getMessage())
                                                        .repoId(c.getRepository().getId())
                                                        .repoName(c.getRepository().getReponame())
                                                        .authorName(c.getAuthor().getUsername())
                                                        .committedAt(c.getCommittedAt())
                                                        .build())
                                        .collect(Collectors.toList()));
                }

                return builder.build();
        }
}
