package com.briefflow.dto.member;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequestDTO(
    @NotNull(message = "Papel e obrigatorio")
    String role
) {}
