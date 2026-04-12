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
    ClientSummaryDTO client,
    JobType type,
    String description,
    String deadline,
    JobPriority priority,
    MemberSummaryDTO assignedCreative,
    JobStatus status,
    Map<String, Object> briefingData,
    Boolean archived,
    List<JobFileDTO> files,
    String createdAt,
    String updatedAt,
    String createdByName,
    boolean overdue
) {}
