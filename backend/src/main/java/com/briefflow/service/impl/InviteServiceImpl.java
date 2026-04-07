package com.briefflow.service.impl;

import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.auth.UserInfoDTO;
import com.briefflow.dto.member.*;
import com.briefflow.entity.*;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.exception.UnauthorizedException;
import com.briefflow.mapper.AuthMapper;
import com.briefflow.mapper.InviteMapper;
import com.briefflow.repository.*;
import com.briefflow.security.JwtService;
import com.briefflow.service.InviteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InviteServiceImpl implements InviteService {

    private final MemberRepository memberRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthMapper authMapper;
    private final InviteMapper inviteMapper;
    private final String frontendUrl;

    public InviteServiceImpl(MemberRepository memberRepository,
                             InviteTokenRepository inviteTokenRepository,
                             WorkspaceRepository workspaceRepository,
                             UserRepository userRepository,
                             JwtService jwtService,
                             PasswordEncoder passwordEncoder,
                             RefreshTokenRepository refreshTokenRepository,
                             AuthMapper authMapper,
                             InviteMapper inviteMapper,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.memberRepository = memberRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authMapper = authMapper;
        this.inviteMapper = inviteMapper;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public InviteTokenResponseDTO inviteMember(Long workspaceId, Long userId, InviteMemberRequestDTO request) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (caller.getRole() != MemberRole.OWNER && caller.getRole() != MemberRole.MANAGER) {
            throw new ForbiddenException("Apenas proprietarios e gerentes podem convidar membros");
        }

        MemberRole role;
        MemberPosition position;
        try {
            role = MemberRole.valueOf(request.role());
            position = MemberPosition.valueOf(request.position());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Papel ou cargo invalido");
        }

        // Check if email already a member
        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            if (memberRepository.existsByUserIdAndWorkspaceId(existingUser.getId(), workspaceId)) {
                throw new BusinessException("Usuario ja e membro deste workspace");
            }
        });

        // Invalidate any existing pending tokens for the same email+workspace
        List<InviteToken> existingTokens = inviteTokenRepository
                .findByWorkspaceIdAndEmailAndUsedFalse(workspaceId, request.email());
        existingTokens.forEach(t -> t.setUsed(true));
        inviteTokenRepository.saveAll(existingTokens);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));

        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        InviteToken inviteToken = new InviteToken();
        inviteToken.setWorkspace(workspace);
        inviteToken.setEmail(request.email());
        inviteToken.setRole(role);
        inviteToken.setPosition(position);
        inviteToken.setToken(UUID.randomUUID().toString());
        inviteToken.setInvitedBy(inviter);
        inviteToken.setExpiresAt(LocalDateTime.now().plusHours(48));
        inviteToken = inviteTokenRepository.save(inviteToken);

        String inviteLink = frontendUrl + "/auth/accept-invite?token=" + inviteToken.getToken();

        return inviteMapper.toTokenResponseDTO(inviteToken, inviteLink);
    }

    @Override
    @Transactional
    public void cancelInvite(Long workspaceId, Long userId, Long inviteId) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (caller.getRole() != MemberRole.OWNER && caller.getRole() != MemberRole.MANAGER) {
            throw new ForbiddenException("Apenas proprietarios e gerentes podem cancelar convites");
        }

        InviteToken inviteToken = inviteTokenRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite nao encontrado"));

        if (!inviteToken.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Convite nao pertence a este workspace");
        }

        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);
    }

    @Override
    public InviteInfoResponseDTO getInviteInfo(String token) {
        InviteToken inviteToken = inviteTokenRepository.findByTokenWithDetails(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite nao encontrado"));

        if (!inviteToken.isUsable()) {
            throw new BusinessException("Convite invalido ou expirado");
        }

        boolean userExists = userRepository.findByEmail(inviteToken.getEmail()).isPresent();

        return inviteMapper.toInfoResponseDTO(inviteToken, userExists);
    }

    @Override
    @Transactional
    public TokenResponseDTO acceptInvite(String token, AcceptInviteRequestDTO request) {
        InviteToken inviteToken = inviteTokenRepository.findByTokenWithDetails(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite nao encontrado"));

        if (!inviteToken.isUsable()) {
            throw new BusinessException("Convite invalido ou expirado");
        }

        User user = userRepository.findByEmail(inviteToken.getEmail()).orElse(null);

        if (user != null) {
            // Existing user: validate password
            if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new UnauthorizedException("Credenciais invalidas");
            }
        } else {
            // New user: create account
            user = new User();
            user.setName(request.name());
            user.setEmail(inviteToken.getEmail());
            user.setPassword(passwordEncoder.encode(request.password()));
            user = userRepository.save(user);
        }

        // Check not already member
        if (memberRepository.existsByUserIdAndWorkspaceId(user.getId(), inviteToken.getWorkspace().getId())) {
            throw new BusinessException("Usuario ja e membro deste workspace");
        }

        // Create member
        Member member = new Member();
        member.setUser(user);
        member.setWorkspace(inviteToken.getWorkspace());
        member.setRole(inviteToken.getRole());
        member.setPosition(inviteToken.getPosition());
        member = memberRepository.save(member);

        // Mark token as used
        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);

        // Generate token response
        return generateTokenResponse(user, member);
    }

    private TokenResponseDTO generateTokenResponse(User user, Member member) {
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), member.getWorkspace().getId());

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
