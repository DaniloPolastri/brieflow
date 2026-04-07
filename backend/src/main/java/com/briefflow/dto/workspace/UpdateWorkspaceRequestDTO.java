package com.briefflow.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequestDTO(
    @NotBlank(message = "Nome do workspace e obrigatorio")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    String name
) {}
