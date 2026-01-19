package com.backend.githubanalyzer.domain.team.service;

import com.backend.githubanalyzer.domain.repository.entity.GithubRepository;
import com.backend.githubanalyzer.domain.repository.repository.GithubRepositoryRepository;
import com.backend.githubanalyzer.domain.commit.repository.CommitRepository;
import com.backend.githubanalyzer.domain.sprint.entity.Sprint;
import com.backend.githubanalyzer.domain.sprint.service.SprintService;
import com.backend.githubanalyzer.domain.team.entity.Team;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprint;
import com.backend.githubanalyzer.domain.team.entity.TeamRegisterSprintId;
import com.backend.githubanalyzer.domain.team.repository.TeamRegisterSprintRepository;
import com.backend.githubanalyzer.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.githubanalyzer.domain.user.entity.User;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintRegistrationService {

        private final SprintService sprintService;
        private final TeamRegisterSprintRepository teamRegisterSprintRepository;
        private final TeamRepository teamRepository;
        private final GithubRepositoryRepository githubRepositoryRepository;
        private final CommitRepository commitRepository;

        @Transactional
        public TeamRegisterSprint registerTeamToSprint(String teamId, String sprintId, String repoId, Long rank,
                        User requestingUser) {
                log.info("Attempting to register Team {} to Sprint {} with Repo {}", teamId, sprintId, repoId);

                // 1. Validate Sprint Openness (This enforces 'registration impossible if
                // isOpened=false')
                sprintService.validateSprintOpen(sprintId);

                // 2. Fetch Entities
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

                // 2. Enforce: Only Team Leader can register
                if (!team.getLeader().getId().equals(requestingUser.getId())) {
                        throw new AccessDeniedException("Only the Team Leader can register for a Sprint.");
                }

                // 3. Enforce: Banned teams cannot register
                if (sprintService.isTeamBanned(sprintId, teamId)) {
                        throw new IllegalStateException("This team has been banned from the Sprint.");
                }

                Sprint sprint = sprintService.getSprint(sprintId);
                GithubRepository repository = githubRepositoryRepository.findById(repoId)
                                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));

                // 3. Create Registration
                TeamRegisterSprintId registrationId = new TeamRegisterSprintId(sprintId, teamId, repoId);
                TeamRegisterSprint registration = TeamRegisterSprint.builder()
                                .id(registrationId)
                                .team(team)
                                .sprint(sprint)
                                .repository(repository)
                                .sprintRank(rank)
                                .build();

                TeamRegisterSprint saved = teamRegisterSprintRepository.save(registration);
                log.info("Successfully registered Team {} to Sprint {}", teamId, sprintId);
                return saved;
        }

        @Transactional
        public void refreshTeamSprintStats(TeamRegisterSprint registration) {
                String repoId = registration.getRepository().getId();
                java.time.LocalDateTime start = registration.getSprint().getStartDate();
                java.time.LocalDateTime end = registration.getSprint().getEndDate();

                Long totalScore = commitRepository.sumTotalScoreByRepoAndTime(repoId, start, end);
                long commitCount = commitRepository.countByRepoAndTime(repoId, start, end);

                registration.setScore(totalScore != null ? totalScore : 0L);
                registration.setCommitNum(commitCount);
                teamRegisterSprintRepository.save(registration);
        }
}
