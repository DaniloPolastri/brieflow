package com.briefflow.controller;

import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.member.AcceptInviteRequestDTO;
import com.briefflow.dto.member.InviteInfoResponseDTO;
import com.briefflow.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invite")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<InviteInfoResponseDTO> getInviteInfo(@PathVariable String token) {
        return ResponseEntity.ok(inviteService.getInviteInfo(token));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<TokenResponseDTO> acceptInvite(
            @PathVariable String token,
            @Valid @RequestBody AcceptInviteRequestDTO request) {
        return ResponseEntity.ok(inviteService.acceptInvite(token, request));
    }
}
