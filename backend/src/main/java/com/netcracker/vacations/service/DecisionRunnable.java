package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.RequestEntity;

public class DecisionRunnable implements Runnable {
    private final RequestEntity request;

    private final RequestService service;

    DecisionRunnable(RequestEntity request, RequestService service) {
        this.request = request;
        this.service = service;
    }

    @Override
    public void run() {
        service.sendRequestDecision(request);
    }
}