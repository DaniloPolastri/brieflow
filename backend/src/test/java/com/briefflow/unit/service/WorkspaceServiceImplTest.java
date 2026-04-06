package com.briefflow.unit.service;

import com.briefflow.dto.workspace.UpdateWorkspaceRequestDTO;
import com.briefflow.dto.workspace.WorkspaceResponseDTO;
import com.briefflow.entity.Member;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.WorkspaceMapper;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @BeforeEach
    void setUp() {
        lenient().when(workspaceMapper.toResponseDTO(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            return new WorkspaceResponseDTO(w.getId(), w.getName(), w.getSlug());
        });
    }

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
    void should_updateWorkspace_when_callerIsOwner() {
        Member caller = new Member();
        caller.setRole(MemberRole.OWNER);

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Old Name");
        workspace.setSlug("old-name");

        when(memberRepository.findByUserIdAndWorkspaceId(10L, 1L)).thenReturn(Optional.of(caller));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, 10L, request);

        assertNotNull(result);
        assertEquals("New Name", result.name());
        verify(workspaceRepository).save(workspace);
    }

    @Test
    void should_updateWorkspace_when_callerIsManager() {
        Member caller = new Member();
        caller.setRole(MemberRole.MANAGER);

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Old Name");
        workspace.setSlug("old-name");

        when(memberRepository.findByUserIdAndWorkspaceId(20L, 1L)).thenReturn(Optional.of(caller));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        WorkspaceResponseDTO result = workspaceService.updateWorkspace(1L, 20L, request);

        assertNotNull(result);
        assertEquals("New Name", result.name());
    }

    @Test
    void should_throwForbidden_when_callerIsCreative() {
        Member caller = new Member();
        caller.setRole(MemberRole.CREATIVE);

        when(memberRepository.findByUserIdAndWorkspaceId(30L, 1L)).thenReturn(Optional.of(caller));

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        assertThrows(ForbiddenException.class, () -> workspaceService.updateWorkspace(1L, 30L, request));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void should_throwNotFound_when_callerNotMember() {
        when(memberRepository.findByUserIdAndWorkspaceId(99L, 1L)).thenReturn(Optional.empty());

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        assertThrows(ResourceNotFoundException.class, () -> workspaceService.updateWorkspace(1L, 99L, request));
    }

    @Test
    void should_throwNotFound_when_updatingNonExistentWorkspace() {
        Member caller = new Member();
        caller.setRole(MemberRole.OWNER);

        when(memberRepository.findByUserIdAndWorkspaceId(10L, 99L)).thenReturn(Optional.of(caller));
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateWorkspaceRequestDTO request = new UpdateWorkspaceRequestDTO("New Name");
        assertThrows(ResourceNotFoundException.class, () -> workspaceService.updateWorkspace(99L, 10L, request));
    }
}
