package com.briefflow.service.impl;

import com.briefflow.dto.member.*;
import com.briefflow.entity.*;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.MemberMapper;
import com.briefflow.repository.*;
import com.briefflow.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MemberRepository memberRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberMapper memberMapper;
    private final String frontendUrl;

    public MemberServiceImpl(MemberRepository memberRepository,
                             InviteTokenRepository inviteTokenRepository,
                             RefreshTokenRepository refreshTokenRepository,
                             MemberMapper memberMapper,
                             @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
        this.memberRepository = memberRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberMapper = memberMapper;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public MembersListResponseDTO listMembers(Long workspaceId) {
        List<Member> members = memberRepository.findByWorkspaceIdWithUser(workspaceId);
        List<InviteToken> pendingInvites = inviteTokenRepository.findByWorkspaceIdAndUsedFalse(workspaceId);

        List<MemberResponseDTO> memberDTOs = members.stream()
                .map(memberMapper::toResponseDTO)
                .toList();

        List<InviteTokenResponseDTO> inviteDTOs = pendingInvites.stream()
                .filter(InviteToken::isUsable)
                .map(t -> new InviteTokenResponseDTO(
                        t.getId(),
                        t.getEmail(),
                        t.getRole().name(),
                        t.getPosition().name(),
                        frontendUrl + "/auth/accept-invite?token=" + t.getToken(),
                        t.getExpiresAt().format(ISO_FORMATTER)
                ))
                .toList();

        return new MembersListResponseDTO(memberDTOs, inviteDTOs);
    }

    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long userId, Long memberId) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (caller.getRole() != MemberRole.OWNER && caller.getRole() != MemberRole.MANAGER) {
            throw new ForbiddenException("Apenas proprietarios e gerentes podem remover membros");
        }

        Member target = memberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException("Nao e possivel remover o proprietario");
        }

        if (target.getUser().getId().equals(userId)) {
            throw new BusinessException("Nao e possivel remover a si mesmo");
        }

        memberRepository.delete(target);

        // MVP limitation: revokes all refresh tokens for the user, not workspace-specific.
        // In multi-workspace future, tokens should be scoped to workspace.
        refreshTokenRepository.revokeAllByUserId(target.getUser().getId());
    }

    @Override
    @Transactional
    public void updateMemberRole(Long workspaceId, Long userId, Long memberId, UpdateMemberRoleRequestDTO request) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (caller.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Apenas o proprietario pode alterar papeis");
        }

        Member target = memberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (target.getUser().getId().equals(userId)) {
            throw new BusinessException("Nao e possivel alterar o proprio papel");
        }

        MemberRole role;
        try {
            role = MemberRole.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Papel invalido");
        }

        target.setRole(role);
        memberRepository.save(target);
    }
}
