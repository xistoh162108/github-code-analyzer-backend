package com.backend.githubanalyzer.domain.team.controller;

import com.backend.githubanalyzer.domain.team.dto.TeamCreateRequest;
import com.backend.githubanalyzer.domain.team.dto.TeamMemberResponse;
import com.backend.githubanalyzer.domain.team.service.TeamService;
import com.backend.githubanalyzer.domain.user.entity.User;
import com.backend.githubanalyzer.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    // 1. 팀 생성
    @PostMapping
    public ResponseEntity<String> createTeam(@RequestBody TeamCreateRequest request) {
        // 레포지토리가 등록되면 기여자가 자동으로 멤버가 되는 로직이 Service에 있다면,
        // 여기서는 팀 자체를 생성하는 역할만 수행합니다.
        String teamId = teamService.createTeam(request);
        return ResponseEntity.ok(teamId);
    }

    // 2. 팀 멤버 조회 (자동으로 등록된 기여자들 조회)
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable String teamId) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(teamId);
        return ResponseEntity.ok(members);
    }
}