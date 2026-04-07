package com.briefflow.controller;

import com.briefflow.dto.workspace.UpdateWorkspaceRequestDTO;
import com.briefflow.dto.workspace.WorkspaceResponseDTO;
import com.briefflow.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<WorkspaceResponseDTO> getWorkspace(@RequestAttribute("workspaceId") Long workspaceId) {
        return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId));
    }

    @PutMapping
    public ResponseEntity<WorkspaceResponseDTO> updateWorkspace(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateWorkspaceRequestDTO request) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(workspaceId, userId, request));
    }
}
