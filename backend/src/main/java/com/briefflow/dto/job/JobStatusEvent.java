package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;

public record JobStatusEvent(
    Long jobId,
    JobStatus previousStatus,
    JobStatus newStatus
) {}
