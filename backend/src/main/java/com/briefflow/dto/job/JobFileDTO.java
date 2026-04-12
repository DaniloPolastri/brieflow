package com.briefflow.dto.job;

public record JobFileDTO(
    Long id,
    String originalFilename,
    String mimeType,
    Long sizeBytes,
    String uploadedAt,
    String downloadUrl
) {}
