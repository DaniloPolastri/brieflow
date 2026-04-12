package com.briefflow.controller;

import com.briefflow.entity.Client;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.exception.UnauthorizedException;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.MemberRepository;
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
    private final ClientRepository clientRepository;
    private final MemberRepository memberRepository;

    public JobSseController(JobSseService jobSseService, JwtService jwtService,
                            ClientRepository clientRepository, MemberRepository memberRepository) {
        this.jobSseService = jobSseService;
        this.jwtService = jwtService;
        this.clientRepository = clientRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long clientId, @RequestParam String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Token invalido");
        }

        Long userId = jwtService.extractUserId(token);
        Long workspaceId = jwtService.extractWorkspaceId(token);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        if (!client.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Cliente não pertence ao workspace");
        }

        memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ForbiddenException("Usuário não pertence ao workspace"));

        return jobSseService.subscribe(clientId);
    }
}
