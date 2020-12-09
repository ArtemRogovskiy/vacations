package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.TeamEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.domain.enums.Status;
import com.netcracker.vacations.dto.TeamDTO;
import com.netcracker.vacations.repository.DepartmentRepository;
import com.netcracker.vacations.repository.RequestRepository;
import com.netcracker.vacations.repository.TeamRepository;
import com.netcracker.vacations.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RequestRepository requestRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository, DepartmentRepository departmentRepository, RequestRepository requestRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.requestRepository = requestRepository;
    }

    public List<TeamEntity> getTeams() {
        return teamRepository.findAll();
    }

    public TeamEntity getTeam(Integer id) {
        if (teamRepository.findById(id).isPresent()) {
            return teamRepository.findById(id).get();
        }
        return null;
    }

    public List<TeamEntity> getTeamsFromDepartment(Integer id) {
        return teamRepository.findAllByDepartmentDepartmentsId(id);
    }

    public TeamDTO addTeam(TeamDTO teamDTO) {
        teamRepository.save(toEntity(teamDTO));
        return teamDTO;
    }

    public void deleteTeam(Integer id) {
        teamRepository.deleteById(id);
    }

    public TeamDTO updateTeam(Integer id, TeamDTO teamDTO) {
        com.netcracker.vacations.domain.TeamEntity teamEntity = teamRepository.findByTeamsId(id).get(0);
        BeanUtils.copyProperties(toEntity(teamDTO), teamEntity, "teamsId");
        teamRepository.save(teamEntity);
        return teamDTO;
    }

    private TeamEntity toEntity(TeamDTO teamDTO) {
        TeamEntity teamEntity = new TeamEntity();
        teamEntity.setTeamsId(null);
        teamEntity.setName(teamDTO.getName());
        teamEntity.setQuota(teamDTO.getQuota());

        if (teamDTO.getDepartmentId() == null) {
            teamEntity.setDepartment(null);
        } else {
            teamEntity.setDepartment(departmentRepository.findByDepartmentsId(teamDTO.getDepartmentId()).get(0));
        }

        if (teamDTO.getManagerId() == null) {
            teamEntity.setManager(null);
        } else {
            teamEntity.setManager(userRepository.findByUsersId(teamDTO.getManagerId()).get(0));
        }
        return teamEntity;
    }

    @PreAuthorize("@Security.isTeamMember(#username, #teamId)")
    public List<UserEntity> getTeamMembers(@P("username") String username, @P("teamId") Integer teamId) {
        return new ArrayList<>(userRepository.findAllByTeamTeamsId(teamId));
    }

    @PreAuthorize("@Security.isTeamMember(#username, null)")
    public List<UserEntity> getTeamMembers(@P("username") String username) {
        Integer teamsId = userRepository.findByLogin(username).get(0).getTeam().getTeamsId();
        return new ArrayList<>(userRepository.findAllByTeamTeamsId(teamsId));
    }

    @PreAuthorize("@Security.isAllowed(#username)")
    public List<TeamEntity> getManagerTeams(@P("username") String username) {
        UserEntity manager = userRepository.findByLogin(username).get(0);
        List<com.netcracker.vacations.domain.TeamEntity> teams = new ArrayList<>(teamRepository.findAllByManager(manager));
        if (manager.getTeam() != null)
            teams.add(manager.getTeam());
        return teams;
    }

    @PreAuthorize("@Security.isTeamMember(#username, #teamID)")
    public List<RequestEntity> getTeamAbsences(@P("username") String username, Integer teamID) {
        List<UserEntity> team = userRepository.findAllByTeamTeamsId(teamID);
        List<RequestEntity> requests = requestRepository.findAllByStatus(Status.ACCEPTED);

        Map<UserEntity, List<RequestEntity>> absences = new HashMap<>();
        for (RequestEntity requestEntity : requests) {
            absences.computeIfAbsent(requestEntity.getUser(), k -> new ArrayList<>()).add(requestEntity);
        }

        List<RequestEntity> result = new ArrayList<>();

        for (UserEntity user : team) {
            if (absences.get(user) != null) {
                List<RequestEntity> temp = absences.get(user);
                result.addAll(temp);
            }
        }
        return result;
    }

}
