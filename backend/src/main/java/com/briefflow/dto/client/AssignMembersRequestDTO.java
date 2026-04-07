package com.briefflow.dto.client;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignMembersRequestDTO(
    @NotNull(message = "Lista de membros e obrigatoria")
    List<Long> memberIds
) {}
