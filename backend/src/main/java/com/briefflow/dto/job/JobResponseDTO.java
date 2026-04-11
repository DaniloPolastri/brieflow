package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import java.util.List;
import java.util.Map;

public record JobResponseDTO(
    Long id,
    String code,
    String title,
    JobType type,
    JobPriority priority,
    JobStatus status,
    String description,
    String deadline,
    Map<String, Object> briefingData,
    Boolean archived,
    ClientSummaryDTO client,
    MemberSummaryDTO assignedCreative,
    MemberSummaryDTO createdBy,
    List<JobFileDTO> files,
    String createdAt,
    String updatedAt,
    boolean overdue
) {}
