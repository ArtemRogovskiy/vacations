package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.repository.UserRepository;
import com.netcracker.vacations.security.MyUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);

    private final UserRepository repository;

    @Autowired
    public AppUserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = repository.findByLogin(username).get(0);
        if (user == null) {
            logger.info("Username: " + username + " not found");
            throw new UsernameNotFoundException("Username: " + username + " not found");
        }
        return new MyUserPrincipal(user);
    }

}
