package com.briefflow.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    String name,

    String company,

    @Email(message = "Email invalido")
    String email,

    String phone
) {}
