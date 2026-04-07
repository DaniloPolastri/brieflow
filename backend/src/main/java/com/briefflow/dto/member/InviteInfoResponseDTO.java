package com.briefflow.dto.member;

public record InviteInfoResponseDTO(
    String workspaceName,
    String email,
    String role,
    String position,
    String invitedByName,
    boolean userExists
) {}
