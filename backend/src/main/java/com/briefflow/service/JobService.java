package com.briefflow.service;

import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JobService {
    JobResponseDTO createJob(Long workspaceId, Long userId, JobRequestDTO request);

    List<JobListItemDTO> listJobs(Long workspaceId, Long userId,
                                  JobStatus status, JobType type, JobPriority priority,
                                  Long clientId, Long assignedCreativeId,
                                  Boolean archived, String search);

    JobResponseDTO getJob(Long workspaceId, Long userId, Long jobId);

    JobResponseDTO updateJob(Long workspaceId, Long userId, Long jobId, JobRequestDTO request);

    JobResponseDTO archiveJob(Long workspaceId, Long userId, Long jobId, boolean archived);

    JobResponseDTO uploadFile(Long workspaceId, Long userId, Long jobId, MultipartFile file);

    void deleteFile(Long workspaceId, Long userId, Long jobId, Long fileId);

    Resource downloadFile(Long workspaceId, Long userId, Long jobId, Long fileId);
}
