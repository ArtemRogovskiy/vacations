package com.netcracker.vacations.controller;

import com.netcracker.vacations.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService service;

    @Autowired
    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @GetMapping("/occupiedForDiscuss")
    public List<List<String>> getOccupiedDaysForDiscuss() {
        return service.getVacationsPerDay("Occupied", "Discuss");
    }

    @GetMapping("/busyForDiscuss")
    public List<List<String>> getBusyDaysForDiscuss() {
        return service.getVacationsPerDay("Busy", "Discuss");
    }

    @GetMapping("/occupiedForSend")
    public List<List<String>> getOccupiedDaysForSend() {
        return service.getVacationsPerDay("Occupied", "Send");
    }

    @GetMapping("/busyForSend")
    public List<List<String>> getBusyDaysForSend() {
        return service.getVacationsPerDay("Busy", "Send");
    }

    @GetMapping("/vacdays")
    public Map<String, List<String>> getVacDays() {
        return service.getVacations();
    }

}