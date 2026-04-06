package com.briefflow.dto.member;

public record MemberResponseDTO(
    Long id,
    Long userId,
    String userName,
    String userEmail,
    String role,
    String position,
    String createdAt
) {}
