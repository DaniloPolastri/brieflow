package com.briefflow.dto.client;

public record ClientResponseDTO(
    Long id,
    String name,
    String company,
    String email,
    String phone,
    String logoUrl,
    Boolean active,
    String createdAt
) {}
