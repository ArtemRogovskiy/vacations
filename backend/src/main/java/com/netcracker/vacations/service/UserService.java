package com.netcracker.vacations.service;

import com.netcracker.vacations.converter.UserConverter;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.domain.enums.Role;
import com.netcracker.vacations.dto.TeamDTO;
import com.netcracker.vacations.dto.UserDTO;
import com.netcracker.vacations.exception.MailServiceException;
import com.netcracker.vacations.exception.NoTeamException;
import com.netcracker.vacations.repository.TeamRepository;
import com.netcracker.vacations.repository.UserRepository;
import com.netcracker.vacations.security.MyUserPrincipal;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional
public class UserService {

    private final JavaMailSender mailSender;

    @Value("${spring-mail-username}")
    private String username;

    private final UserRepository userRepository;

    private final TeamRepository teamRepository;

    @Autowired
    public UserService(UserRepository userRepository, TeamRepository teamRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.mailSender = mailSender;
    }

    public List<UserEntity> getUsers() {
        List<UserEntity> response = new ArrayList<>();
        for (UserEntity entity : userRepository.findAll()) {
            response.add(entity);
        }
        return response;
    }

    public List<UserEntity> getUsersSubordinateToManager(Integer id) {
        return new ArrayList<>(userRepository.findAllByTeamManagerUsersId(id));
    }

    public UserEntity getUser(Integer id) {
        return userRepository.findById(id).get();
    }

    public List<UserEntity> getUsersFromTeam(Integer teamId) {
        return new ArrayList<>(userRepository.findAllByTeamTeamsId(teamId));
    }

    public String getUsersName() {
        String name = SecurityExpressionMethods.currentUserLogin();
        UserEntity currentUser = userRepository.findByLogin(name).get(0);
        return (currentUser.getRole().getName() + ": " + currentUser.getName() + " " + currentUser.getSurname());
    }

    public UserDTO getUserInfo() {
        String login = SecurityExpressionMethods.currentUserLogin();
        UserEntity currentUser = userRepository.findByLogin(login).get(0);
        UserDTO user = UserConverter.toDTO(currentUser);
        if (currentUser.getRole().equals(Role.MANAGER)) {
            List<String> subordinateTeamsLines = new ArrayList<>();
            List<com.netcracker.vacations.domain.TeamEntity> subordinateTeams = teamRepository.findAllByManager(currentUser);
            if (subordinateTeams.isEmpty()) {
                user.setSubordinateTeams("-");
            } else {
                for (com.netcracker.vacations.domain.TeamEntity team : subordinateTeams) {
                    subordinateTeamsLines.add(team.getName());
                }
                String teams = String.join(", ", subordinateTeamsLines);
                user.setSubordinateTeams(teams);
            }
        }
        if (currentUser.getFamilyName() == null || currentUser.getFamilyName().isEmpty()) {
            user.setFamilyName("-");
        }
        if (currentUser.getTeam() == null) {
            user.setTeamName("-");
        }
        if (currentUser.getPhoneNumber() == null || currentUser.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber("-");
        }
        if (currentUser.getDescription() == null || currentUser.getDescription().isEmpty()) {
            user.setDescription("-");
        }
        return user;
    }

    public UserDTO addUser(UserDTO userDTO) {
        userDTO.setPassword(UUID.randomUUID().toString());
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Runnable sender = new PasswordRunnable(userDTO, this);
        userRepository.save(UserConverter.toEntity(userDTO));
        executor.execute(sender);

        return userDTO;
    }

    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    public UserEntity updateUser(Integer id, UserEntity user) {
        UserEntity userEntity = userRepository.findByUsersId(id).get(0);
        BeanUtils.copyProperties(user, userEntity, "usersId", "password");
        userRepository.save(userEntity);
        return user;
    }

    @PreAuthorize("@Security.isAllowed(#username)")
    public void updatePassword(Integer id, String password, String mode, @P("username") String username) {
        UserEntity userEntity;
        if (mode.equals("UserChange")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = ((MyUserPrincipal) authentication.getPrincipal()).getUser().getLogin();
            List<UserEntity> users = userRepository.findByLogin(login);
            if (!users.isEmpty()) {
                userEntity = users.get(0);
                userEntity.setPassword(password);
            }
        } else {
            userEntity = userRepository.findByUsersId(id).get(0);
            userEntity.setPassword(password);
        }
    }

    public boolean checkPassword(String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = ((MyUserPrincipal) authentication.getPrincipal()).getUser().getLogin();
        UserEntity currentUser = userRepository.findByLogin(login).get(0);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return bcrypt.matches(password, currentUser.getPassword());
    }

    public String getUserByCode(String code) {
        if (userRepository.findByActivationCode(code).get(0) != null) {
            return userRepository.findByActivationCode(code).get(0).getLogin();
        }
        return null;
    }

    public void changePassword(List<String> userInfo) {
        String password = userInfo.get(0);
        String login = userInfo.get(1);
        if (!password.isEmpty() && !login.isEmpty()) {
            UserEntity user = userRepository.findByLogin(login).get(0);
            user.setPassword(password);
            user.setActivationCode(null);
            userRepository.save(user);
        }
    }

    public boolean sendMailForgot(String email) {
        boolean isSent = false;
        if (email != null) {
            List<UserEntity> users = userRepository.findByEmail(email);
            if (!users.isEmpty()) {
                UserEntity user = users.get(0);
                user.setActivationCode(UUID.randomUUID().toString());
                String message = "Dear " + user.getName() + " " + user.getSurname() + ",\n" + "if you can not use your old password, you can pick a new one. " +
                        "For doing this visit next link: https://absence-management.azurewebsites.net/activation/" + user.getActivationCode();
                send(email, "Changing your password.", message);
                isSent = true;
            }
        }
        return isSent;
    }

    public void restDaysRecounting(String login) {
        UserEntity user = userRepository.findByLogin(login).get(0);
        LocalDate today = LocalDate.now();
        LocalDate hireDate = user.getHireDate();
        LocalDate lastVisit = user.getLastVisit();
        long elapsedMonths;
        if (lastVisit == null) {
            elapsedMonths = ChronoUnit.MONTHS.between(hireDate, today);
        } else if (ChronoUnit.MONTHS.between(today, lastVisit) != 0) {
            elapsedMonths = ChronoUnit.MONTHS.between(hireDate, lastVisit);
        } else {
            return;
        }
        double profit = elapsedMonths * 7.0 / 3.0;
        user.setRestDays(user.getRestDays() + profit);
        user.setLastVisit(today);
        userRepository.save(user);
    }

    public void addUsersToTeam(Integer teamId, int[] usersId) {
        for (Integer user : usersId) {
            UserEntity userEntity = userRepository.findByUsersId(user).get(0);
            userEntity.setTeam(teamRepository.findByTeamsId(teamId).get(0));
            userRepository.save(userEntity);
        }
    }

    public void sendMailPassword(UserDTO user) {
        if (user.getEmail() != null) {
            String message = "Dear " + user.getName() + " " + user.getSurname() + ",\n" + "you successfully registered your account. " +
                    "Now your username is \"" + user.getLogin() + "\" and your password is \"" + user.getPassword() + "\". You can change your password on your account. For authorization visit next link: https://absence-management.azurewebsites.net/";
            send(user.getEmail(), "Account activation, password changing.", message);
        }
    }

    public void send(String emailTo, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom(username);
        mailMessage.setTo(emailTo);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        try {
            mailSender.send(mailMessage);
        } catch (MailSendException ex) {
            throw new MailServiceException(ex.getMessage());
        }
    }

    @PreAuthorize("@Security.isAllowed(#username)")
    public TeamDTO getUserTeam(@P("username") String username) {
        com.netcracker.vacations.domain.TeamEntity team = userRepository.findByLogin(username).get(0).getTeam();
        if (team == null) throw new NoTeamException("You are not a member of any team");
        return new TeamDTO().setTeamId(team.getTeamsId()).setName(team.getName());
    }

    public Integer getRestDays(String username) {
        return userRepository.findByLogin(username).get(0).getRestDays().intValue();
    }

}
