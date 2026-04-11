package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;

public record JobListItemDTO(
    Long id,
    String code,
    String title,
    JobType type,
    JobPriority priority,
    JobStatus status,
    String deadline,
    ClientSummaryDTO client,
    MemberSummaryDTO assignedCreative,
    boolean overdue
) {}
