package com.briefflow.controller;

import com.briefflow.dto.member.*;
import com.briefflow.service.InviteService;
import com.briefflow.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;
    private final InviteService inviteService;

    public MemberController(MemberService memberService, InviteService inviteService) {
        this.memberService = memberService;
        this.inviteService = inviteService;
    }

    @GetMapping
    public ResponseEntity<MembersListResponseDTO> listMembers(@RequestAttribute("workspaceId") Long workspaceId) {
        return ResponseEntity.ok(memberService.listMembers(workspaceId));
    }

    @PostMapping("/invite")
    public ResponseEntity<InviteTokenResponseDTO> inviteMember(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody InviteMemberRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inviteService.inviteMember(workspaceId, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        memberService.removeMember(workspaceId, userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/invite/{id}")
    public ResponseEntity<Void> cancelInvite(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        inviteService.cancelInvite(workspaceId, userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> updateMemberRole(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRoleRequestDTO request) {
        memberService.updateMemberRole(workspaceId, userId, id, request);
        return ResponseEntity.noContent().build();
    }
}
