package com.briefflow.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
    @NotBlank(message = "Refresh token e obrigatorio")
    String refreshToken
) {}
