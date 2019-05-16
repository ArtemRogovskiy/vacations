package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.RequestTypeEntity;
import com.netcracker.vacations.domain.TeamEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.domain.enums.RequestType;
import com.netcracker.vacations.domain.enums.Role;
import com.netcracker.vacations.domain.enums.Status;
import com.netcracker.vacations.dto.RequestDTO;
import com.netcracker.vacations.exception.TooManyDaysException;
import com.netcracker.vacations.exception.WrongPeriodException;
import com.netcracker.vacations.repository.*;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
@Transactional
public class RequestService {
    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    private RequestRepository requestRepository;
    private RequestTypeRepository requestTypeRepository;
    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private DepartmentRepository departmentRepository;
    private UserService userService;
    private MethodsService methodService;
    private IntegrationService integrationService;

    @Autowired
    public RequestService(RequestRepository requestRepository, RequestTypeRepository requestTypeRepository, UserRepository userRepository, TeamRepository teamRepository, DepartmentRepository departmentRepository, UserService userService, MethodsService methodService, IntegrationService integrationService) {
        this.requestRepository = requestRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.departmentRepository = departmentRepository;
        this.userService = userService;
        this.methodService = methodService;
        this.integrationService = integrationService;
    }

    public void saveRequest(RequestDTO request) throws Exception {
        RequestTypeEntity type = requestTypeRepository.findByName(request.getType()).get(0);
        Status status = Status.CONSIDER;
        if (!type.getNeedApproval()) {
            status = Status.ACCEPTED;
        }

        UserEntity user = userRepository.findByLogin(request.getUsername()).get(0);
        LocalDate begin = convertToLocalDateViaInstant(request.getStart());
        LocalDate end = convertToLocalDateViaInstant(request.getEnd());
        Period intervalPeriod = Period.between(begin, end);
        if (type.getName().equals(RequestType.VACATION.getName())) {
            if (intervalPeriod.getDays() > user.getRestDays()) {
                throw new TooManyDaysException("You took too many days. You have only " + user.getRestDays() + " vacant days");
            }
        }
        checkIfInterruptsOtherAbsence(user, begin, end);
        RequestEntity requestEntity = new RequestEntity(user, begin, end, type, status);
        requestEntity.setDescription(request.getDescription());
        requestRepository.save(requestEntity);
        if (Status.ACCEPTED.equals(status)) {
            integrationService.insertEventWithoutConfirm(requestEntity);
        }
    }

    private void checkIfInterruptsOtherAbsence(UserEntity user, LocalDate begin, LocalDate end) {
        for (RequestEntity entity : requestRepository.findAllByUser(user)) {
            if (entity.getStatus().equals(Status.ACCEPTED)
                    && ((entity.getBeginning().isBefore(begin) && entity.getEnding().isAfter(begin))
                    || (entity.getBeginning().isBefore(end) && entity.getEnding().isAfter(end)
                    || (entity.getBeginning().isAfter(begin) && entity.getEnding().isBefore(end)))))
                throw new WrongPeriodException("You already have absence on selected days");
        }
    }

    @PreAuthorize("@Security.isTeamMember(#username, null)")
    public void updateRequest(Status status, List<Integer> requests, @P("username") String username) {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size()+1);
        for (Integer id : requests) {
            RequestEntity entity = requestRepository.findById(id).get();
            entity.setStatus(status);
            if (status.equals(Status.ACCEPTED)
                    && entity.getTypeOfRequest().getName().equals(RequestType.VACATION.getName()))
                decrementRestDays(entity);
            requestRepository.save(entity);
            Runnable sender= new DecisionRunnable(toDTO(entity), this);
            executor.execute(sender);
        }
    }

    private void decrementRestDays(RequestEntity entity) {
        UserEntity user = entity.getUser();
        LocalDate begin = entity.getBeginning();
        LocalDate end = entity.getEnding();
        Period intervalPeriod = Period.between(begin, end);
        Double restDays = user.getRestDays();
        user.setRestDays(restDays - intervalPeriod.getDays() - 1);
        userRepository.save(user);
    }

    @PreAuthorize("@Security.isTeamMember(#name, null) or hasRole('DIRECTOR') or hasRole(ADMIN)")
    public List<RequestDTO> getActiveRequests(@P("name") String name) {
        List<RequestDTO> response = new ArrayList<>();
        UserEntity user = userRepository.findByLogin(name).get(0);
        if (user.getRole().equals(Role.ADMIN)) {
            for (RequestEntity entity : requestRepository.findAll()) {
                if ((entity.getStatus().equals(Status.CONSIDER))) {
                    response.add(toDTO(entity));
                }
            }
        } else if (user.getRole().equals(Role.DIRECTOR)) {
            List<TeamEntity> directorsTeams = teamRepository.findAllByDepartment(departmentRepository.findByDirector(user).get(0));
            for (RequestEntity entity : requestRepository.findAll()) {
                for (TeamEntity team : directorsTeams) {
                    if ((entity.getStatus().equals(Status.CONSIDER)) && (team.equals(entity.getUser().getTeam()))) {
                        response.add(toDTO(entity));
                        break;
                    }
                }
            }
        } else {
            List<TeamEntity> managersTeams = teamRepository.findAllByManager(user);
            for (RequestEntity entity : requestRepository.findAll()) {
                for (TeamEntity team : managersTeams) {
                    if ((entity.getStatus().equals(Status.CONSIDER)) && (team.equals(entity.getUser().getTeam()))) {
                        response.add(toDTO(entity));
                        break;
                    }
                }
            }
        }
        return response;
    }

    @PreAuthorize("@Security.isTeamMember(#name, null)")
    public List<RequestDTO> getResolvedRequests(@P("name") String name) {
        List<RequestDTO> response = new ArrayList<>();
        UserEntity user = userRepository.findByLogin(name).get(0);
        if (user.getRole().equals(Role.ADMIN)) {
            for (RequestEntity entity : requestRepository.findAll()) {
                if (!entity.getStatus().equals(Status.CONSIDER)) {
                    response.add(toDTO(entity));
                }
            }
        } else if (user.getRole().equals(Role.DIRECTOR)) {
            List<TeamEntity> directorsTeams = teamRepository.findAllByDepartment(departmentRepository.findByDirector(user).get(0));
            for (RequestEntity entity : requestRepository.findAll()) {
                for (TeamEntity team : directorsTeams) {
                    if ((!entity.getTypeOfRequest().getNeedApproval()
                            || !entity.getStatus().equals(Status.CONSIDER)) && (team.equals(entity.getUser().getTeam()))) {
                        response.add(toDTO(entity));
                        break;
                    }
                }
            }
        } else {
            List<TeamEntity> managersTeams = teamRepository.findAllByManager(user);
            for (RequestEntity entity : requestRepository.findAll()) {
                for (TeamEntity team : managersTeams) {
                    if ((!entity.getTypeOfRequest().getNeedApproval()
                            || !entity.getStatus().equals(Status.CONSIDER)) && (team.equals(entity.getUser().getTeam()))) {
                        response.add(toDTO(entity));
                        break;
                    }
                }
            }
        }
        return response;
    }

    private RequestDTO toDTO(RequestEntity entity) {
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setId(entity.getRequestsId());
        String name = entity.getUser().getName();
        String familyName = entity.getUser().getFamilyName();
        if ((name == null || name.isEmpty()) && (familyName == null || familyName.isEmpty())) {
            requestDTO.setName("-");
        } else if (familyName == null || familyName.isEmpty()) {
            requestDTO.setName(entity.getUser().getName());
        } else if (name == null || name.isEmpty()) {
            requestDTO.setName(entity.getUser().getFamilyName());
        } else {
            requestDTO.setName(entity.getUser().getName() + " " + entity.getUser().getFamilyName());
        }
        if (entity.getUser().getTeam() != null) {
            requestDTO.setTeamName(entity.getUser().getTeam().getName());
        } else {
            requestDTO.setTeamName("-");
        }
        if (entity.getUser().getTeam() != null) {
            requestDTO.setTeamName(entity.getUser().getTeam().getName());
        } else {
            requestDTO.setTeamName("-");
        }
        requestDTO.setName(entity.getUser().getName() + " " + entity.getUser().getFamilyName());
        requestDTO.setDescription(entity.getDescription());
        requestDTO.setStart(convertToDateViaSqlDate(entity.getBeginning()));
        requestDTO.setEnd(convertToDateViaSqlDate(entity.getEnding()));
        requestDTO.setType(entity.getTypeOfRequest().getName());
        requestDTO.setStatus(entity.getStatus().getName());
        return requestDTO;
    }


    public boolean isManagerOnRest() {
        String login = SecurityExpressionMethods.currentUserLogin();
        boolean answer = false;
        UserEntity user = userRepository.findByLogin(login).get(0);
        if (user.getTeam() != null) {
            UserEntity manager = user.getTeam().getManager();
            List<RequestEntity> requests = requestRepository.findAllByUser(manager);
            Date currentDate = new Date();
            Calendar calCurrent = Calendar.getInstance();
            Calendar calBegin = Calendar.getInstance();
            Calendar calEnd = Calendar.getInstance();

            calCurrent.setTime(currentDate);

            boolean sameDayBegin = calCurrent.get(Calendar.DAY_OF_YEAR) == calBegin.get(Calendar.DAY_OF_YEAR) && calCurrent.get(Calendar.YEAR) == calBegin.get(Calendar.YEAR);
            boolean sameDayEnd = calCurrent.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR) && calCurrent.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR);
            for (RequestEntity req : requests) {
                calBegin.setTime(convertToDateViaSqlDate(req.getBeginning()));
                calEnd.setTime(convertToDateViaSqlDate(req.getEnding()));
                if ((((convertToDateViaSqlDate(req.getBeginning()).before(currentDate)) || sameDayBegin) && ((((convertToDateViaSqlDate(req.getEnding()).after(currentDate)) || sameDayEnd))))) {
                    if (req.getTypeOfRequest().getInfluenceOnVr()) {
                        answer = true;
                        break;
                    }
                }
            }
        }
        return answer;
    }


    public void sendMailRequest(RequestDTO request) {
        UserEntity user = userRepository.findByLogin(request.getUsername()).get(0);
        boolean needToSend = requestTypeRepository.findByName(request.getType()).get(0).getNeedApproval();
        if (needToSend && user.getTeam() != null) {
            UserEntity director = user.getTeam().getDepartment().getDirector();
            if (director.getEmail() != null) {
                String message = String.format("Dear " + director.getName() + " " + director.getSurname() + ".\n" + request.getDescription() +
                        "||Request was sent by " + user.getName() + " " + user.getSurname() + ". Reason: " + request.getType() + " Begin date: " + request.getStart() + ". End date: " + request.getEnd() + ". " +
                        "Created on " + request.getCreation() + ". ||");
                userService.send(director.getEmail(), "Request by " + user.getName() + " " + user.getSurname() + ".", message);
            }
        }
    }

    private Date convertToDateViaSqlDate(LocalDate dateToConvert) {
        return java.sql.Date.valueOf(dateToConvert);
    }

    public LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public List<RequestDTO> getUserRequests(String username) {
        List<RequestDTO> response = new ArrayList<>();
        for (RequestEntity entity : requestRepository.findAllByUser(userRepository.findByLogin(username).get(0))) {
            response.add(toDTO(entity));
        }
        return response;
    }

    public void deleteRequest(String username, Integer id) {
        Optional<RequestEntity> entity = requestRepository.findById(id);
        if (entity.isPresent() && entity.get().getUser().getLogin().equals(username) && entity.get().getStatus().equals(Status.CONSIDER)) {
            requestRepository.deleteById(id);
        }
    }

    public void sendRequestDecision(RequestDTO request) {
        System.out.println("REQNAME " + request.getUsername());
        UserEntity user = userRepository.findByLogin(request.getUsername()).get(0);
        if (user.getEmail() != null) {
            String message = "Dear " + user.getName() + " " + user.getSurname() + ".\n" + "Your request's status is now \"" + request.getStatus() + "\". If you have any questions, please, ask your manager (or your director, if manager can't help you now). \n" +
                    "||Request was sent by " + user.getName() + " " + user.getSurname() + ". Reason: " + request.getType() + " Begin date: " + request.getStart() + ". End date: " + request.getEnd() + ".||";
            userService.send(user.getEmail(), "Decision for request by " + user.getName() + " " + user.getSurname() + ". " + request.getType() + ".", message);
        }

    }

}
