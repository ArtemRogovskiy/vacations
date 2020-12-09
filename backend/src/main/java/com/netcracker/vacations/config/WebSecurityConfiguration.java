package com.netcracker.vacations.config;

import com.netcracker.vacations.domain.enums.Role;
import com.netcracker.vacations.security.JwtLoginTokenFilter;
import com.netcracker.vacations.security.JwtTokenFilter;
import com.netcracker.vacations.security.JwtTokenProvider;
import com.netcracker.vacations.service.AppUserService;
import com.netcracker.vacations.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final JwtTokenProvider jwtTokenProvider;

    private final AppUserService appUserService;

    private final JwtConfig jwtConfig;

    private final UserService userService;

    @Autowired
    public WebSecurityConfiguration(JwtTokenProvider jwtTokenProvider, AppUserService appUserService, JwtConfig jwtConfig, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appUserService = appUserService;
        this.jwtConfig = jwtConfig;
        this.userService = userService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
                .antMatchers("/api/departments").hasAuthority("ROLE_" + Role.ADMIN)
                .antMatchers("/api/users/sendMailForgot/**").permitAll()
                .antMatchers("/api/users/userByCode/**").permitAll()
                .antMatchers("/api/users/changePassword").permitAll()
                .antMatchers("/api/**").hasAnyAuthority("ROLE_" + Role.EMPLOYEE, "ROLE_" + Role.ADMIN, "ROLE_" + Role.MANAGER, "ROLE_" + Role.DIRECTOR)
                .antMatchers("/**").permitAll()
                .and()
                .addFilterBefore(new JwtLoginTokenFilter("/login", authenticationManagerBean(), jwtConfig, userService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(appUserService).passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}