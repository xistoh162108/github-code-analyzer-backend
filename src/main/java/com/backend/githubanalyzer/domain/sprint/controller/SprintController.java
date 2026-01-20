package com.backend.githubanalyzer.domain.sprint.controller;

import com.backend.githubanalyzer.domain.sprint.dto.SprintCreateRequest;
import com.backend.githubanalyzer.domain.sprint.dto.SprintRegisterRequest;
import com.backend.githubanalyzer.domain.sprint.dto.SprintResponse;
import com.backend.githubanalyzer.domain.sprint.service.SprintService;

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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Sprint", description = "스프린트 및 챌린지 관리 API")
public class SprintController {

    private final SprintService sprintService;
    private final UserRepository userRepository;

    @io.swagger.v3.oas.annotations.Operation(summary = "Create Sprint (스프린트 생성)", description = "새로운 스프린트를 생성합니다.")
    @PostMapping
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintResponse>> createSprint(
            @RequestBody SprintCreateRequest request) {
        com.backend.githubanalyzer.domain.sprint.dto.SprintResponse response = sprintService.createSprint(request);
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "List Public Sprints (공개 스프린트 조회)", description = "현재 참여 가능한 공개 스프린트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<SprintResponse>>> getSprints() {
        return ResponseEntity
                .ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(sprintService.getPublicSprints()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "List My Sprints (내 스프린트 조회)", description = "내가 참여 중이거나 생성한 스프린트 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<SprintResponse>>> getMySprints() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse
                .success(sprintService.getMyParticipatingSprints(currentUser.getId())));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Sprint Rankings (스프린트 랭킹)", description = "특정 스프린트의 팀 또는 개인 랭킹을 조회합니다.")
    @GetMapping("/{sprintId}/ranking")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<List<?>>> getSprintRankings(
            @PathVariable String sprintId,
            @io.swagger.v3.oas.annotations.Parameter(description = "랭킹 타입 (TEAM, INDIVIDUAL). Default: TEAM") @RequestParam(required = false, defaultValue = "TEAM") String type) {

        if ("INDIVIDUAL".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse
                    .success(sprintService.getSprintIndividualRankings(sprintId)));
        }
        return ResponseEntity.ok(
                com.backend.githubanalyzer.global.dto.ApiResponse.success(sprintService.getSprintRankings(sprintId)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update Sprint (스프린트 수정)", description = "스프린트 정보를 수정합니다. 매니저만 가능합니다.")
    @PutMapping("/{sprintId}")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintResponse>> updateSprint(
            @PathVariable String sprintId,
            @RequestBody SprintCreateRequest request) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse
                .success(sprintService.updateSprint(sprintId, request, currentUser.getId())));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Register Team (스프린트 참가 신청)", description = "팀 단위로 스프린트에 참가를 신청합니다. 팀장만 가능합니다.")
    @PostMapping("/{sprintId}/registration")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse>> registerTeamToSprint(
            @PathVariable String sprintId,
            @RequestBody SprintRegisterRequest request) {

        User currentUser = getCurrentUser();
        com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse response = sprintService
                .registerTeamToSprint(sprintId, request.teamId(), request.repoId(), currentUser.getId());
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(response));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Approve/Reject Team (참가 승인/거절)", description = "참가 신청한 팀을 승인하거나 거절합니다. (비공개 스프린트 매니저 기능)")
    @PostMapping("/{sprintId}/registrations/{teamId}/approve")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse>> approveTeam(
            @PathVariable String sprintId,
            @PathVariable String teamId,
            @io.swagger.v3.oas.annotations.Parameter(description = "true: 승인, false: 거절") @RequestParam boolean approve) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse
                .success(sprintService.approveTeamRegistration(sprintId, teamId, currentUser.getId(), approve)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Ban Team (팀 강퇴)", description = "특정 팀을 스프린트에서 강퇴시킵니다. (매니저 기능)")
    @PostMapping("/{sprintId}/registrations/{teamId}/ban")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintRegistrationResponse>> banTeam(
            @PathVariable String sprintId,
            @PathVariable String teamId) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse
                .success(sprintService.banTeam(sprintId, teamId, currentUser)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get Basic Sprint Info (스프린트 정보 조회)", description = "ID로 스프린트의 기본 정보를 조회합니다. (비공개 스프린트 존재 여부 확인용)")
    @GetMapping("/{sprintId}/info")
    public ResponseEntity<com.backend.githubanalyzer.global.dto.ApiResponse<com.backend.githubanalyzer.domain.sprint.dto.SprintInfoResponse>> getSprintInfo(
            @PathVariable String sprintId) {
        return ResponseEntity.ok(com.backend.githubanalyzer.global.dto.ApiResponse.success(sprintService.getSprintInfo(sprintId)));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
