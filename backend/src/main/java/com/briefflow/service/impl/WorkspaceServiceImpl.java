package com.briefflow.service.impl;

import com.briefflow.dto.workspace.UpdateWorkspaceRequestDTO;
import com.briefflow.dto.workspace.WorkspaceResponseDTO;
import com.briefflow.entity.Workspace;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public WorkspaceResponseDTO getWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));
        return toDTO(workspace);
    }

    @Override
    @Transactional
    public WorkspaceResponseDTO updateWorkspace(Long workspaceId, UpdateWorkspaceRequestDTO request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));
        workspace.setName(request.name());
        workspace = workspaceRepository.save(workspace);
        return toDTO(workspace);
    }

    private WorkspaceResponseDTO toDTO(Workspace workspace) {
        return new WorkspaceResponseDTO(workspace.getId(), workspace.getName(), workspace.getSlug());
    }
}
