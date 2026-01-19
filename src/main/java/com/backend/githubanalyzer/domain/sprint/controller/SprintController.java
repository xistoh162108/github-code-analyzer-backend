package com.backend.githubanalyzer.domain.sprint.controller;

import com.backend.githubanalyzer.domain.sprint.dto.SprintCreateRequest;
import com.backend.githubanalyzer.domain.sprint.dto.SprintRegisterRequest;
import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import com.backend.githubanalyzer.domain.sprint.service.SprintService;
import com.backend.githubanalyzer.domain.team.service.SprintRegistrationService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;
    private final SprintRegistrationService sprintRegistrationService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> createSprint(@RequestBody SprintCreateRequest request) {
        String sprintId = sprintService.createSprint(request);
        return ResponseEntity.ok(sprintId);
    }

    @GetMapping
    public ResponseEntity<List<SprintResponse>> getSprints() {
        return ResponseEntity.ok(sprintService.getPublicSprints());
    }

    @GetMapping("/{sprintId}/ranking")
    public ResponseEntity<List<?>> getSprintRankings(
            @PathVariable String sprintId,
            @RequestParam(required = false, defaultValue = "TEAM") String type) {

        if ("INDIVIDUAL".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(sprintService.getSprintIndividualRankings(sprintId));
        }
        return ResponseEntity.ok(sprintService.getSprintRankings(sprintId));
    }

    @PostMapping("/{sprintId}/registration")
    public ResponseEntity<Void> registerTeamToSprint(
            @PathVariable String sprintId,
            @RequestBody SprintRegisterRequest request) {

        User currentUser = getCurrentUser();

        sprintRegistrationService.registerTeamToSprint(
                request.teamId(),
                sprintId,
                request.repoId(),
                0L,
                currentUser);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
