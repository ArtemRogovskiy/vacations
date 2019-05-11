package com.netcracker.vacations.controller;

import com.netcracker.vacations.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private CalendarService service;

    @Autowired
    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @GetMapping("/occupiedForDiscuss")
    public List<List<String>> getOccupiedDaysForDiscuss(@RequestParam String name) {
        return service.getVacationsPerDay("Occupied", "Discuss", name);
    }

    @GetMapping("/busyForDiscuss")
    public List<List<String>> getBusyDaysForDiscuss(@RequestParam String name) {
        return service.getVacationsPerDay("Busy", "Discuss", name);
    }

    @GetMapping("/occupiedForSend")
    public List<List<String>> getOccupiedDaysForSend(@RequestParam String name) {
        return service.getVacationsPerDay("Occupied", "Send", name);
    }

    @GetMapping("/busyForSend")
    public List<List<String>> getBusyDaysForSend(@RequestParam String name) {
        return service.getVacationsPerDay("Busy", "Send", name);
    }


    @PreAuthorize("@Security.isAllowed(#name)")
    @GetMapping("/accepted")
    public List<String> getAccepted(@RequestParam @P("name") String name) {
        return service.getVacations("Accepted", name);
    }

    @PreAuthorize("@Security.isAllowed(#name)")
    @GetMapping("/declined")
    public List<String> getDeclined(@RequestParam @P("name") String name) {
        return service.getVacations("Declined", name);
    }

    @PreAuthorize("@Security.isAllowed(#name)")
    @GetMapping("/consider")
    public List<String> getConsider(@RequestParam @P("name") String name) {
        return service.getVacations("Consider", name);
    }

}