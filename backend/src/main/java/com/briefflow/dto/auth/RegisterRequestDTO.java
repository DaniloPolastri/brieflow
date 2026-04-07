package com.briefflow.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    String name,

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    String password,

    @NotBlank(message = "Nome do workspace e obrigatorio")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    String workspaceName
) {}
