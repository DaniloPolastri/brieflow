package com.briefflow.service.impl;

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
import com.briefflow.mapper.AuthMapper;
import com.briefflow.security.JwtService;
import com.briefflow.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           WorkspaceRepository workspaceRepository,
                           MemberRepository memberRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
    }

    @Override
    @Transactional
    public TokenResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email ja cadastrado");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        Workspace workspace = new Workspace();
        workspace.setName(request.workspaceName());

        String baseSlug = Workspace.generateSlug(request.workspaceName());
        String slug = baseSlug;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }
        workspace.setSlug(slug);

        workspace = workspaceRepository.save(workspace);

        Member member = new Member();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(MemberRole.OWNER);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);
        member = memberRepository.save(member);

        return generateTokenResponse(user, member);
    }

    private static final String DUMMY_HASH = "$2a$10$dummyhashtoequalizetimingfornonexistentusers00000000000";

    @Override
    @Transactional
    public TokenResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new UnauthorizedException("Credenciais invalidas");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Conta desativada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Credenciais invalidas");
        }

        Member member = memberRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Sua conta nao esta vinculada a nenhum workspace"));
        return generateTokenResponse(user, member);
    }

    @Override
    @Transactional
    public TokenResponseDTO refresh(RefreshTokenRequestDTO request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalido"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token invalido ou expirado");
        }

        // Rotation: revoke old, create new
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        Member member = memberRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Sua conta nao esta vinculada a nenhum workspace"));
        return generateTokenResponse(user, member);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDTO request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private TokenResponseDTO generateTokenResponse(User user, Member member) {
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), member.getWorkspace().getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        UserInfoDTO userInfo = authMapper.toUserInfoDTO(user, member);

        return new TokenResponseDTO(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessExpirationMs(),
                userInfo
        );
    }
}
