package com.netcracker.vacations.controller;

import com.netcracker.vacations.service.IntegrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("callback")
public class IntegrationCallbackController {

    private final IntegrationService integrationService;

    public IntegrationCallbackController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Value("${redirect}")
    private String redirect;

    @GetMapping
    public RedirectView callback(@RequestParam(name = "code") String code) throws Exception {
        integrationService.insertEvents(code);
        return new RedirectView(redirect);
    }
}

