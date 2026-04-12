package com.briefflow.dto.job;

import com.briefflow.enums.MemberRole;

public record MemberSummaryDTO(
    Long id,
    Long userId,
    String name,
    String email,
    MemberRole role
) {}
