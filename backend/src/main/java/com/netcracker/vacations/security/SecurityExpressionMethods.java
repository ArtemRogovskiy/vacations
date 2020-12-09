package com.netcracker.vacations.security;

import com.netcracker.vacations.domain.TeamEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Component("Security")
public class SecurityExpressionMethods {

    private final TeamRepository teamRepository;

    @Autowired
    public SecurityExpressionMethods(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public boolean isTeamManager(Integer teamId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserEntity currentUser = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
        if (teamRepository.findByManager(currentUser).isEmpty()) return false;
        Set<Integer> currentUserTeams = teamRepository.findByManager(currentUser)
                .stream()
                .map(TeamEntity::getTeamsId)
                .collect(toSet());
        return currentUserTeams.contains(teamId);
    }

    public boolean isTeamMember(String username, Optional<Integer> teamID) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserEntity currentUser = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
        boolean isTeamMember = false;
        boolean isTeamManager;
        if (teamID.isPresent()) {
            if (currentUser.getTeam() != null)
                isTeamMember = teamID.get().equals(currentUser.getTeam().getTeamsId());
            isTeamManager = isTeamManager(teamID.get());
            return currentUser.getLogin().equals(username) && (isTeamMember || isTeamManager);
        }
        return currentUser.getLogin().equals(username);
    }

    public boolean isAllowed(Optional<String> username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserEntity currentUser = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
        return username.isPresent() && currentUser.getLogin().equals(username.get());
    }

    public static String currentUserLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final UserEntity currentUser = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
        return currentUser.getLogin();
    }
}

