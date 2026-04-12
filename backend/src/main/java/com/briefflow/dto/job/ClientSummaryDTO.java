package com.briefflow.dto.job;

public record ClientSummaryDTO(
    Long id,
    String name,
    String company,
    String logoUrl
) {}
