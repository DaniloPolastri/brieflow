package com.briefflow.service.impl;

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
import com.briefflow.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceMapper workspaceMapper;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository,
                                MemberRepository memberRepository,
                                WorkspaceMapper workspaceMapper) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.workspaceMapper = workspaceMapper;
    }

    @Override
    public WorkspaceResponseDTO getWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));
        return workspaceMapper.toResponseDTO(workspace);
    }

    @Override
    @Transactional
    public WorkspaceResponseDTO updateWorkspace(Long workspaceId, Long userId, UpdateWorkspaceRequestDTO request) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (caller.getRole() != MemberRole.OWNER && caller.getRole() != MemberRole.MANAGER) {
            throw new ForbiddenException("Apenas proprietarios e gestores podem editar o workspace");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));
        workspace.setName(request.name());

        String baseSlug = Workspace.generateSlug(request.name());
        String slug = baseSlug;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(slug)) {
            // Skip if the existing slug belongs to this same workspace
            Workspace existing = workspaceRepository.findBySlug(slug).orElse(null);
            if (existing != null && existing.getId().equals(workspaceId)) {
                break;
            }
            slug = baseSlug + "-" + suffix++;
        }
        workspace.setSlug(slug);

        workspace = workspaceRepository.save(workspace);
        return workspaceMapper.toResponseDTO(workspace);
    }
}
