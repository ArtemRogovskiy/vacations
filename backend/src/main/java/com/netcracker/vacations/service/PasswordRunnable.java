package com.netcracker.vacations.service;

import com.netcracker.vacations.dto.UserDTO;

public class PasswordRunnable implements Runnable {
    private final UserDTO user;

    private final UserService service;

    PasswordRunnable(UserDTO user, UserService service) {
        this.user = user;
        this.service = service;
    }

    @Override
    public void run() {
        service.sendMailPassword(user);
    }
}


