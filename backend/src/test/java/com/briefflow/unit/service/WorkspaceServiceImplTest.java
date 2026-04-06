package com.briefflow.unit.service;

import com.briefflow.dto.workspace.UpdateWorkspaceRequestDTO;
import com.briefflow.dto.workspace.WorkspaceResponseDTO;
import com.briefflow.entity.Workspace;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @Test
    void should_getWorkspace_when_exists() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Agency One");
        workspace.setSlug("agency-one");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));

        WorkspaceResponseDTO result = workspaceService.getWorkspace(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Agency One", result.name());
        assertEquals("agency-one", result.slug());
    }

    @Test
    void should_throwNotFound_when_workspaceNotExists() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workspaceService.getWorkspace(99L));
    }

    @Test
    void should_updateWorkspace_when_validRequest() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Old Name");
        workspace.setSlug("old-name");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, request);

        assertNotNull(result);
        assertEquals("New Name", result.name());
        verify(workspaceRepository).save(workspace);
    }

    @Test
    void should_throwNotFound_when_updatingNonExistentWorkspace() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        assertThrows(ResourceNotFoundException.class, () -> workspaceService.updateWorkspace(99L, request));
    }
}
