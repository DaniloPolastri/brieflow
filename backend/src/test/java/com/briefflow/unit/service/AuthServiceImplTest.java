package com.briefflow.unit.service;

import com.briefflow.dto.auth.*;
import com.briefflow.entity.Member;
import com.briefflow.entity.RefreshToken;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.UnauthorizedException;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.RefreshTokenRepository;
import com.briefflow.repository.UserRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.security.JwtService;
import com.briefflow.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void should_register_when_validRequest() {
        RegisterRequestDTO request = new RegisterRequestDTO("John", "john@test.com", "password123", "Workspace John");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Workspace John");
        workspace.setSlug("workspace-john");
        when(workspaceRepository.existsBySlug("workspace-john")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);

        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(1L);
            m.setWorkspace(workspace);
            return m;
        });

        when(jwtService.generateAccessToken(eq(1L), eq("john@test.com"), eq(1L))).thenReturn("access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponseDTO result = authService.register(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals("John", result.user().name());
        assertEquals(1L, result.user().workspaceId());
        assertEquals("Workspace John", result.user().workspaceName());
        assertEquals("OWNER", result.user().role());
        assertEquals("DIRETOR_DE_ARTE", result.user().position());
        verify(userRepository).save(any(User.class));
        verify(workspaceRepository).save(any(Workspace.class));
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void should_throwBusinessException_when_emailAlreadyExists() {
        RegisterRequestDTO request = new RegisterRequestDTO("John", "john@test.com", "password123", "Workspace John");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void should_login_when_validCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("john@test.com", "password123");

        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@test.com");
        user.setPassword("hashed");
        user.setActive(true);

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Agency");

        Member member = new Member();
        member.setId(1L);
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(MemberRole.OWNER);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(memberRepository.findFirstByUserId(1L)).thenReturn(Optional.of(member));
        when(jwtService.generateAccessToken(eq(1L), eq("john@test.com"), eq(1L))).thenReturn("access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponseDTO result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("John", result.user().name());
        assertEquals(1L, result.user().workspaceId());
        assertEquals("OWNER", result.user().role());
    }

    @Test
    void should_throwUnauthorized_when_invalidPassword() {
        LoginRequestDTO request = new LoginRequestDTO("john@test.com", "wrongpassword");

        User user = new User();
        user.setPassword("hashed");
        user.setActive(true);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void should_throwUnauthorized_when_userNotFound() {
        LoginRequestDTO request = new LoginRequestDTO("unknown@test.com", "password123");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void should_throwUnauthorized_when_userIsInactive() {
        LoginRequestDTO request = new LoginRequestDTO("john@test.com", "password123");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void should_throwForbidden_when_loginWithNoWorkspace() {
        LoginRequestDTO request = new LoginRequestDTO("john@test.com", "password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("john@test.com");
        user.setPassword("hashed");
        user.setActive(true);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(memberRepository.findFirstByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> authService.login(request));
    }

    @Test
    void should_refreshToken_when_validRefreshToken() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("old-refresh-token");

        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@test.com");

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Agency");

        Member member = new Member();
        member.setId(1L);
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(MemberRole.OWNER);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-refresh-token");
        oldToken.setUser(user);
        oldToken.setRevoked(false);
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(oldToken));
        when(memberRepository.findFirstByUserId(1L)).thenReturn(Optional.of(member));
        when(jwtService.generateAccessToken(eq(1L), eq("john@test.com"), eq(1L))).thenReturn("new-access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponseDTO result = authService.refresh(request);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertTrue(oldToken.isRevoked());
        assertEquals(1L, result.user().workspaceId());
    }

    @Test
    void should_throwUnauthorized_when_refreshTokenNotFound() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("nonexistent");
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request));
    }

    @Test
    void should_throwUnauthorized_when_refreshTokenIsRevoked() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("revoked-token");

        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request));
    }

    @Test
    void should_throwUnauthorized_when_refreshTokenIsExpired() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("expired-token");

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request));
    }

    @Test
    void should_revokeToken_when_logout() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("token-to-revoke");

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));

        authService.logout(request);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
