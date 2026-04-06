package com.briefflow.service;

import com.briefflow.dto.workspace.UpdateWorkspaceRequestDTO;
import com.briefflow.dto.workspace.WorkspaceResponseDTO;

public interface WorkspaceService {
    WorkspaceResponseDTO getWorkspace(Long workspaceId);
    WorkspaceResponseDTO updateWorkspace(Long workspaceId, Long userId, UpdateWorkspaceRequestDTO request);
}
