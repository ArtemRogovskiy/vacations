package com.netcracker.vacations.controller;

import com.netcracker.vacations.converter.UserConverter;
import com.netcracker.vacations.dto.UserDTO;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import com.netcracker.vacations.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserService service;

    public UsersController(UserService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserDTO> users(@RequestParam(name = "teamId", required = false) Integer teamId,
                               @RequestParam(name = "managerId", required = false) Integer managerId) {
        if (teamId != null) {
            return service.getUsersFromTeam(teamId).stream().map(UserConverter::toDTO).collect(Collectors.toList());
        } else if (managerId != null) {
            return service.getUsersSubordinateToManager(managerId).stream().map(UserConverter::toDTO).collect(Collectors.toList());
        }
        return service.getUsers().stream().map(UserConverter::toDTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable("id") Integer id) {
        return UserConverter.toDTO(service.getUser(id));
    }

    @GetMapping("/name")
    public String getUsersName() {
        return service.getUsersName();
    }

    @GetMapping("/info")
    public UserDTO getUserInfo() {
        return service.getUserInfo();
    }

    @GetMapping("/checkPassword")
    public boolean checkPassword(@RequestParam String password) {
        return service.checkPassword(password);
    }

    @GetMapping("/userByCode/{code}")
    public String getUserByCode(@PathVariable("code") String code) {
        return service.getUserByCode(code);
    }

    @GetMapping("/sendMailForgot")
    public boolean sendMailForgot(@RequestParam String email) {
        return service.sendMailForgot(email);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addUser")
    public UserDTO addUser(@RequestBody UserDTO userDTO) {
        return service.addUser(userDTO);
    }

    @PatchMapping("/changePassword")
    public void changePassword(@RequestBody List<String> userInfo) {
        service.changePassword(userInfo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserDTO updateUser(
            @PathVariable("id") Integer id,
            @RequestBody UserDTO userDTO
    ) {
        return UserConverter.toDTO(service.updateUser(id, UserConverter.toEntity(userDTO)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/password/{id}")
    public void updatePassword(
            @PathVariable("id") Integer id,
            @RequestBody String password
    ) {
        String username = SecurityExpressionMethods.currentUserLogin();
        service.updatePassword(id, password, "AdminChange", username);
    }

    @PutMapping("/passwordByUser")
    public void updatePassword(@RequestBody String password) {
        String username = SecurityExpressionMethods.currentUserLogin();
        service.updatePassword(0, password, "UserChange", username);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable("id") Integer id) {
        service.deleteUser(id);
    }

    @GetMapping("/team")
    public ResponseEntity<?> getUserTeam() {
        String username = SecurityExpressionMethods.currentUserLogin();
        return ResponseEntity.accepted().body(service.getUserTeam(username));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/addToTeam/{id}")
    public void addUsersToTeam(@PathVariable("id") Integer id, @RequestBody int[] usersId) {
        service.addUsersToTeam(id, usersId);
    }

    @GetMapping("/restdays")
    public ResponseEntity<?> getRestDays() {
        String username = SecurityExpressionMethods.currentUserLogin();
        return ResponseEntity.ok(service.getRestDays(username));
    }
}
