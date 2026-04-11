package com.briefflow.controller;

import com.briefflow.dto.job.ArchiveJobRequestDTO;
import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import com.briefflow.service.JobService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> create(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody JobRequestDTO request) {
        JobResponseDTO dto = jobService.createJob(workspaceId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<JobListItemDTO>> list(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) JobPriority priority,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long assignedCreativeId,
            @RequestParam(required = false, defaultValue = "false") Boolean archived,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(jobService.listJobs(
                workspaceId, userId, status, type, priority, clientId, assignedCreativeId, archived, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> get(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(workspaceId, userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDTO> update(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody JobRequestDTO request) {
        return ResponseEntity.ok(jobService.updateJob(workspaceId, userId, id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<JobResponseDTO> archive(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ArchiveJobRequestDTO request) {
        return ResponseEntity.ok(jobService.archiveJob(workspaceId, userId, id, request.archived()));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobResponseDTO> uploadFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.uploadFile(workspaceId, userId, id, file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @PathVariable Long fileId) {
        jobService.deleteFile(workspaceId, userId, id, fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @PathVariable Long fileId) {
        Resource r = jobService.downloadFile(workspaceId, userId, id, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.getFilename() + "\"")
                .body(r);
    }
}
