package com.briefflow.dto.member;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequestDTO(
    @NotBlank(message = "Papel e obrigatorio")
    String role
) {}
