package com.briefflow.dto.member;

import jakarta.validation.constraints.Size;

public record AcceptInviteRequestDTO(
    String name,
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    String password
) {}
