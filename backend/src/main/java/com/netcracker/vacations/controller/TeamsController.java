package com.netcracker.vacations.controller;

import com.netcracker.vacations.converter.AbsenceConverter;
import com.netcracker.vacations.converter.TeamConverter;
import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.dto.AbsenceDTO;
import com.netcracker.vacations.dto.TeamDTO;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import com.netcracker.vacations.service.TeamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/teams")
public class TeamsController {

    private final TeamService teamService;

    public TeamsController(TeamService service) {
        this.teamService = service;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    @GetMapping
    public List<TeamDTO> teams(@RequestParam(name = "departmentId", required = false) Integer departmentId) {
        if (departmentId != null) {
            return teamService.getTeamsFromDepartment(departmentId).stream().map(TeamConverter::convert).collect(Collectors.toList());
        }
        return teamService.getTeams().stream().map(TeamConverter::convert).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN') or @Security.isTeamManager(#id)")
    @GetMapping("/{id}")
    public TeamDTO getTeam(@PathVariable("id") @P("id") Integer id) {
        return TeamConverter.convert(teamService.getTeam(id));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    @PostMapping("/addTeam")
    public TeamDTO addTeam(@RequestBody TeamDTO teamDTO) {
        return teamService.addTeam(teamDTO);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    @PutMapping("/{id}")
    public TeamDTO updateTeam(
            @PathVariable("id") Integer id,
            @RequestBody TeamDTO teamDTO
    ) {
        return teamService.updateTeam(id, teamDTO);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable("id") Integer id) {
        teamService.deleteTeam(id);
    }

    @GetMapping(value = {"/members", "/members/{id}"})
    public List<AbsenceDTO> getTeamMembers(@PathVariable(value = "id") Optional<Integer> teamId) {
        String username = SecurityExpressionMethods.currentUserLogin();
        List<AbsenceDTO> response = new ArrayList<>();
        if (teamId.isPresent())
            for (UserEntity user : teamService.getTeamMembers(username, teamId.get()))
                response.add(AbsenceConverter.convert(user));
        else
            for (UserEntity user : teamService.getTeamMembers(username))
                response.add(AbsenceConverter.convert(user));
        return response;
    }

    @GetMapping("/absences/{id}")
    public List<AbsenceDTO> getTeamAbsences(@PathVariable("id") @P("teamID") Integer teamID) {
        String username = SecurityExpressionMethods.currentUserLogin();
        List<AbsenceDTO> response = new ArrayList<>();
        for (RequestEntity entity : teamService.getTeamAbsences(username, teamID)) {
            response.add(AbsenceConverter.convert(entity));
        }
        return response;
    }

    @GetMapping("/my")
    public List<TeamDTO> getManagerTeams() {
        String username = SecurityExpressionMethods.currentUserLogin();
        List<TeamDTO> response = new ArrayList<>();
        for (com.netcracker.vacations.domain.TeamEntity entity: teamService.getManagerTeams(username)) {
            response.add(TeamConverter.convert(entity));
        }
        return response;
    }

}

