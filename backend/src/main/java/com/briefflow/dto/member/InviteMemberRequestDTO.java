package com.briefflow.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequestDTO(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Papel e obrigatorio")
    String role,

    @NotBlank(message = "Cargo e obrigatorio")
    String position
) {}
