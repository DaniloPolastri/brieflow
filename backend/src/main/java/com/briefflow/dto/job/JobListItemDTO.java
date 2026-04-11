package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;

public record JobListItemDTO(
    Long id,
    String code,
    String title,
    String clientName,
    JobType type,
    String deadline,
    JobPriority priority,
    String assignedCreativeName,
    JobStatus status,
    Boolean isOverdue
) {}
