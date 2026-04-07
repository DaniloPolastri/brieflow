package com.briefflow.unit.service;

import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.member.*;
import com.briefflow.entity.*;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.UnauthorizedException;
import com.briefflow.mapper.AuthMapper;
import com.briefflow.mapper.InviteMapper;
import com.briefflow.repository.*;
import com.briefflow.security.JwtService;
import com.briefflow.service.impl.InviteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceImplTest {

    @Mock private MemberRepository memberRepository;
    @Mock private InviteTokenRepository inviteTokenRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuthMapper authMapper;
    @Mock private InviteMapper inviteMapper;

    private InviteServiceImpl inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteServiceImpl(
                memberRepository, inviteTokenRepository, workspaceRepository,
                userRepository, jwtService, passwordEncoder,
                refreshTokenRepository, authMapper, inviteMapper,
                "http://localhost:4200");

        lenient().when(authMapper.toUserInfoDTO(any(User.class), any(Member.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            Member m = inv.getArgument(1);
            return new com.briefflow.dto.auth.UserInfoDTO(
                    u.getId(), u.getName(), u.getEmail(),
                    m.getWorkspace().getId(), m.getWorkspace().getName(),
                    m.getRole().name(), m.getPosition().name()
            );
        });

        lenient().when(inviteMapper.toTokenResponseDTO(any(InviteToken.class), anyString())).thenAnswer(inv -> {
            InviteToken t = inv.getArgument(0);
            String link = inv.getArgument(1);
            return new InviteTokenResponseDTO(
                    t.getId(), t.getEmail(), t.getRole().name(), t.getPosition().name(),
                    link, t.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        });

        lenient().when(inviteMapper.toInfoResponseDTO(any(InviteToken.class), anyBoolean())).thenAnswer(inv -> {
            InviteToken t = inv.getArgument(0);
            boolean exists = inv.getArgument(1);
            return new InviteInfoResponseDTO(
                    t.getWorkspace().getName(), t.getEmail(), t.getRole().name(),
                    t.getPosition().name(), t.getInvitedBy().getName(), exists
            );
        });
    }

    // --- inviteMember ---

    @Test
    void should_inviteMember_when_ownerInvites() {
        User owner = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(1L, owner, workspace, MemberRole.OWNER);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(callerMember));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(inviteTokenRepository.findByWorkspaceIdAndEmailAndUsedFalse(1L, "new@test.com")).thenReturn(List.of());
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(inviteTokenRepository.save(any(InviteToken.class))).thenAnswer(inv -> {
            InviteToken t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        InviteMemberRequestDTO request = new InviteMemberRequestDTO("new@test.com", "CREATIVE", "DESIGNER_GRAFICO");
        InviteTokenResponseDTO result = inviteService.inviteMember(1L, 1L, request);

        assertNotNull(result);
        assertEquals("new@test.com", result.email());
        assertEquals("CREATIVE", result.role());
        assertNotNull(result.inviteLink());
        assertTrue(result.inviteLink().contains("/auth/accept-invite?token="));
        verify(inviteTokenRepository).save(any(InviteToken.class));
    }

    @Test
    void should_throwForbidden_when_creativeTriesToInvite() {
        User creative = createUser(2L, "Creative", "creative@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(2L, creative, workspace, MemberRole.CREATIVE);

        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(callerMember));

        InviteMemberRequestDTO request = new InviteMemberRequestDTO("new@test.com", "CREATIVE", "DESIGNER_GRAFICO");

        assertThrows(ForbiddenException.class, () -> inviteService.inviteMember(1L, 2L, request));
    }

    @Test
    void should_throwBusiness_when_emailAlreadyMember() {
        User owner = createUser(1L, "Owner", "owner@test.com");
        User existing = createUser(3L, "Existing", "existing@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member callerMember = createMember(1L, owner, workspace, MemberRole.OWNER);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(callerMember));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existing));
        when(memberRepository.existsByUserIdAndWorkspaceId(3L, 1L)).thenReturn(true);

        InviteMemberRequestDTO request = new InviteMemberRequestDTO("existing@test.com", "CREATIVE", "DESIGNER_GRAFICO");

        assertThrows(BusinessException.class, () -> inviteService.inviteMember(1L, 1L, request));
    }

    // --- getInviteInfo ---

    @Test
    void should_getInviteInfo_when_validToken() {
        Workspace workspace = createWorkspace(1L, "Agency");
        User inviter = createUser(1L, "Owner", "owner@test.com");

        InviteToken inviteToken = new InviteToken();
        inviteToken.setId(1L);
        inviteToken.setWorkspace(workspace);
        inviteToken.setEmail("new@test.com");
        inviteToken.setRole(MemberRole.CREATIVE);
        inviteToken.setPosition(MemberPosition.DESIGNER_GRAFICO);
        inviteToken.setToken("valid-token");
        inviteToken.setInvitedBy(inviter);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));
        inviteToken.setUsed(false);

        when(inviteTokenRepository.findByTokenWithDetails("valid-token")).thenReturn(Optional.of(inviteToken));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        InviteInfoResponseDTO result = inviteService.getInviteInfo("valid-token");

        assertNotNull(result);
        assertEquals("Agency", result.workspaceName());
        assertEquals("new@test.com", result.email());
        assertEquals("CREATIVE", result.role());
        assertEquals("Owner", result.invitedByName());
        assertFalse(result.userExists());
    }

    @Test
    void should_throwBusiness_when_tokenExpiredOrUsed() {
        InviteToken inviteToken = new InviteToken();
        inviteToken.setUsed(true);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));

        when(inviteTokenRepository.findByTokenWithDetails("used-token")).thenReturn(Optional.of(inviteToken));

        assertThrows(BusinessException.class, () -> inviteService.getInviteInfo("used-token"));
    }

    // --- acceptInvite ---

    @Test
    void should_acceptInvite_when_newUser() {
        Workspace workspace = createWorkspace(1L, "Agency");
        User inviter = createUser(1L, "Owner", "owner@test.com");

        InviteToken inviteToken = new InviteToken();
        inviteToken.setId(1L);
        inviteToken.setWorkspace(workspace);
        inviteToken.setEmail("new@test.com");
        inviteToken.setRole(MemberRole.CREATIVE);
        inviteToken.setPosition(MemberPosition.DESIGNER_GRAFICO);
        inviteToken.setToken("valid-token");
        inviteToken.setInvitedBy(inviter);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));
        inviteToken.setUsed(false);

        when(inviteTokenRepository.findByTokenWithDetails("valid-token")).thenReturn(Optional.of(inviteToken));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(memberRepository.existsByUserIdAndWorkspaceId(5L, 1L)).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });
        when(inviteTokenRepository.save(any(InviteToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(5L), eq("new@test.com"), eq(1L))).thenReturn("access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AcceptInviteRequestDTO request = new AcceptInviteRequestDTO("New User", "password123");
        TokenResponseDTO result = inviteService.acceptInvite("valid-token", request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("New User", result.user().name());
        assertEquals(1L, result.user().workspaceId());
        assertEquals("CREATIVE", result.user().role());
        assertTrue(inviteToken.isUsed());
    }

    @Test
    void should_acceptInvite_when_existingUser() {
        Workspace workspace = createWorkspace(1L, "Agency");
        User existingUser = createUser(3L, "Existing", "existing@test.com");
        existingUser.setPassword("hashed-pass");
        User inviter = createUser(1L, "Owner", "owner@test.com");

        InviteToken inviteToken = new InviteToken();
        inviteToken.setId(1L);
        inviteToken.setWorkspace(workspace);
        inviteToken.setEmail("existing@test.com");
        inviteToken.setRole(MemberRole.MANAGER);
        inviteToken.setPosition(MemberPosition.SOCIAL_MEDIA);
        inviteToken.setToken("valid-token");
        inviteToken.setInvitedBy(inviter);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));
        inviteToken.setUsed(false);

        when(inviteTokenRepository.findByTokenWithDetails("valid-token")).thenReturn(Optional.of(inviteToken));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "hashed-pass")).thenReturn(true);
        when(memberRepository.existsByUserIdAndWorkspaceId(3L, 1L)).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });
        when(inviteTokenRepository.save(any(InviteToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(3L), eq("existing@test.com"), eq(1L))).thenReturn("access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AcceptInviteRequestDTO request = new AcceptInviteRequestDTO(null, "password123");
        TokenResponseDTO result = inviteService.acceptInvite("valid-token", request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("Existing", result.user().name());
        assertEquals("MANAGER", result.user().role());
        verify(userRepository, never()).save(any(User.class)); // should NOT create new user
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
