package com.briefflow.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequestDTO(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotNull(message = "Papel e obrigatorio")
    String role,

    @NotNull(message = "Cargo e obrigatorio")
    String position
) {}
