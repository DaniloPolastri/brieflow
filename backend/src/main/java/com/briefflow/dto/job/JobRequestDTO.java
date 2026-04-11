package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record JobRequestDTO(
    @NotNull Long clientId,
    Long assignedCreativeId,
    @NotBlank @Size(max = 255) String title,
    @NotNull JobType type,
    @NotNull JobPriority priority,
    String description,
    LocalDate deadline,
    @NotNull Map<String, Object> briefingData
) {}
