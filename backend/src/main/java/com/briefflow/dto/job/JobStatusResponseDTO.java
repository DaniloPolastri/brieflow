package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;

public record JobStatusResponseDTO(
    Long id,
    String code,
    JobStatus previousStatus,
    JobStatus newStatus,
    boolean skippedSteps,
    boolean applied
) {}
