package com.briefflow.controller;

import com.briefflow.exception.UnauthorizedException;
import com.briefflow.security.JwtService;
import com.briefflow.service.JobSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/jobs")
public class JobSseController {

    private final JobSseService jobSseService;
    private final JwtService jwtService;

    public JobSseController(JobSseService jobSseService, JwtService jwtService) {
        this.jobSseService = jobSseService;
        this.jwtService = jwtService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long clientId, @RequestParam String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Token invalido");
        }
        return jobSseService.subscribe(clientId);
    }
}
