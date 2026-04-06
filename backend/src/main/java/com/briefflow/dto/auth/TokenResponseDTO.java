package com.briefflow.dto.auth;

public record TokenResponseDTO(
    String accessToken,
    String refreshToken,
    long expiresIn,
    UserInfoDTO user
) {}
