package com.netcracker.vacations.controller;

import com.netcracker.vacations.converter.RequestConverter;
import com.netcracker.vacations.domain.enums.RequestType;
import com.netcracker.vacations.domain.enums.Status;
import com.netcracker.vacations.dto.RequestDTO;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import com.netcracker.vacations.service.IntegrationService;
import com.netcracker.vacations.service.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);

    private final RequestService reqService;
    private final IntegrationService integrationService;

    @Autowired
    public RequestController(RequestService reqService, IntegrationService integrationService) {

        this.reqService = reqService;
        this.integrationService = integrationService;
    }

    @PostMapping
    public ResponseEntity<?> addRequest(@RequestBody RequestDTO request) throws Exception {
        reqService.saveRequest(request);
        if (request.getNeedToEmail()) {
            reqService.sendMailRequest(request);
        }
        return ResponseEntity.ok("Requests is sent");
    }

    @GetMapping("/active")
    public List<RequestDTO> getActiveRequests() {
        String username = SecurityExpressionMethods.currentUserLogin();
        return reqService.getActiveRequests(username).stream().map(RequestConverter::convert).collect(Collectors.toList());
    }

    @GetMapping("/resolved")
    public List<RequestDTO> getResolvedRequests() {
        String username = SecurityExpressionMethods.currentUserLogin();
        return reqService.getResolvedRequests(username).stream().map(RequestConverter::convert).collect(Collectors.toList());
    }

    @GetMapping("/managerVac")
    public boolean isManagerOnWork() {
        return reqService.isManagerOnRest();
    }

    @GetMapping("/types")
    public String[] getTypes() {
        return RequestType.getNames();
    }

    @PatchMapping("/decline")
    public void declineRequest(@RequestBody List<Integer> requests) {
        String username = SecurityExpressionMethods.currentUserLogin();
        reqService.updateRequest(Status.DECLINED, requests, username);
    }

    @PatchMapping("/approve")
    public void approveRequest(@RequestBody List<Integer> requests) throws Exception {
        String username = SecurityExpressionMethods.currentUserLogin();
        reqService.updateRequest(Status.ACCEPTED, requests, username);
        integrationService.insertEventsAfterApproval(requests);
    }

    @GetMapping("/my")
    public List<RequestDTO> getUserRequest() {
        String username = SecurityExpressionMethods.currentUserLogin();
        return reqService.getUserRequests(username).stream().map(RequestConverter::convert).collect(Collectors.toList());
    }

    @PatchMapping("/delete")
    public void deleteRequest(@RequestBody Integer id) {
        String username = SecurityExpressionMethods.currentUserLogin();
        reqService.deleteRequest(username, id);
    }
}

