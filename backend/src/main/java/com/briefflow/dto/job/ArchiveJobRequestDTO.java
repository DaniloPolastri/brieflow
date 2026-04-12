package com.briefflow.dto.job;

import jakarta.validation.constraints.NotNull;

public record ArchiveJobRequestDTO(
    @NotNull Boolean archived
) {}
