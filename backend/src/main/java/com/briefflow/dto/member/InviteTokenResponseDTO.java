package com.briefflow.dto.member;

public record InviteTokenResponseDTO(
    Long id,
    String email,
    String role,
    String position,
    String inviteLink,
    String expiresAt
) {}
