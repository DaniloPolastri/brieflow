package com.briefflow.dto.auth;

public record UserInfoDTO(
    Long id,
    String name,
    String email,
    Long workspaceId,
    String workspaceName,
    String role,
    String position
) {}
