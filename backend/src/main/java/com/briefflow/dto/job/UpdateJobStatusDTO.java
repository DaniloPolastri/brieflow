package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateJobStatusDTO(
    @NotNull JobStatus status,
    boolean confirm
) {}
