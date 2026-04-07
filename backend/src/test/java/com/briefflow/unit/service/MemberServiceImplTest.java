package com.briefflow.unit.service;

import com.briefflow.dto.member.*;
import com.briefflow.entity.*;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.mapper.MemberMapper;
import com.briefflow.repository.*;
import com.briefflow.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock private MemberRepository memberRepository;
    @Mock private InviteTokenRepository inviteTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MemberMapper memberMapper;

    private MemberServiceImpl memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(
                memberRepository, inviteTokenRepository, refreshTokenRepository,
                memberMapper, "http://localhost:4200");

        lenient().when(memberMapper.toResponseDTO(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            return new MemberResponseDTO(
                    m.getId(),
                    m.getUser().getId(),
                    m.getUser().getName(),
                    m.getUser().getEmail(),
                    m.getRole().name(),
                    m.getPosition().name(),
                    m.getCreatedAt().toString()
            );
        });
    }

    // --- listMembers ---

    @Test
    void should_listMembers_when_workspaceHasMembers() {
        User user = createUser(1L, "John", "john@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");

        Member member = new Member();
        member.setId(1L);
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(MemberRole.OWNER);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);
        member.setCreatedAt(LocalDateTime.now());

        when(memberRepository.findByWorkspaceIdWithUser(1L)).thenReturn(List.of(member));
        when(inviteTokenRepository.findByWorkspaceIdAndUsedFalse(1L)).thenReturn(List.of());

        MembersListResponseDTO result = memberService.listMembers(1L);

        assertNotNull(result);
        assertEquals(1, result.members().size());
        assertEquals("John", result.members().get(0).userName());
        assertEquals("OWNER", result.members().get(0).role());
        assertEquals(0, result.pendingInvites().size());
    }

    // --- removeMember ---

    @Test
    void should_removeMember_when_ownerRemoves() {
        User owner = createUser(1L, "Owner", "owner@test.com");
        User target = createUser(2L, "Target", "target@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(1L, owner, workspace, MemberRole.OWNER);
        Member targetMember = createMember(2L, target, workspace, MemberRole.CREATIVE);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(callerMember));
        when(memberRepository.findByIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(targetMember));

        memberService.removeMember(1L, 1L, 2L);

        verify(memberRepository).delete(targetMember);
    }

    @Test
    void should_throwForbidden_when_removingOwner() {
        User owner = createUser(1L, "Owner", "owner@test.com");
        User manager = createUser(2L, "Manager", "manager@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(2L, manager, workspace, MemberRole.MANAGER);
        Member ownerMember = createMember(1L, owner, workspace, MemberRole.OWNER);

        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(callerMember));
        when(memberRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));

        assertThrows(ForbiddenException.class, () -> memberService.removeMember(1L, 2L, 1L));
    }

    @Test
    void should_throwBusiness_when_removingSelf() {
        User manager = createUser(2L, "Manager", "manager@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member managerMember = createMember(3L, manager, workspace, MemberRole.MANAGER);

        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(managerMember));
        when(memberRepository.findByIdAndWorkspaceId(3L, 1L)).thenReturn(Optional.of(managerMember));

        assertThrows(BusinessException.class, () -> memberService.removeMember(1L, 2L, 3L));
    }

    // --- updateMemberRole ---

    @Test
    void should_updateMemberRole_when_ownerUpdates() {
        User owner = createUser(1L, "Owner", "owner@test.com");
        User target = createUser(2L, "Target", "target@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(1L, owner, workspace, MemberRole.OWNER);
        Member targetMember = createMember(2L, target, workspace, MemberRole.CREATIVE);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(callerMember));
        when(memberRepository.findByIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMemberRoleRequestDTO request = new UpdateMemberRoleRequestDTO("MANAGER");
        memberService.updateMemberRole(1L, 1L, 2L, request);

        assertEquals(MemberRole.MANAGER, targetMember.getRole());
        verify(memberRepository).save(targetMember);
    }

    @Test
    void should_throwForbidden_when_managerTriesToUpdateRole() {
        User manager = createUser(2L, "Manager", "manager@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(2L, manager, workspace, MemberRole.MANAGER);

        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(callerMember));

        UpdateMemberRoleRequestDTO request = new UpdateMemberRoleRequestDTO("CREATIVE");

        assertThrows(ForbiddenException.class, () -> memberService.updateMemberRole(1L, 2L, 3L, request));
    }

    // --- Helper methods ---

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Workspace createWorkspace(Long id, String name) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName(name);
        workspace.setSlug(name.toLowerCase().replaceAll("\\s+", "-"));
        return workspace;
    }

    private Member createMember(Long id, User user, Workspace workspace, MemberRole role) {
        Member member = new Member();
        member.setId(id);
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(role);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);
        member.setCreatedAt(LocalDateTime.now());
        return member;
    }
}
