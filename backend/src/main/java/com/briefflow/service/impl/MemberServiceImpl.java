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
import com.briefflow.mapper.MemberMapper;
import com.briefflow.repository.*;
import com.briefflow.security.JwtService;
import com.briefflow.service.MemberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class MemberServiceImpl implements MemberService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MemberRepository memberRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberMapper memberMapper;
    private final String frontendUrl;

    public MemberServiceImpl(MemberRepository memberRepository,
                             InviteTokenRepository inviteTokenRepository,
                             WorkspaceRepository workspaceRepository,
                             UserRepository userRepository,
                             JwtService jwtService,
                             PasswordEncoder passwordEncoder,
                             RefreshTokenRepository refreshTokenRepository,
                             MemberMapper memberMapper,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.memberRepository = memberRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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

        return new InviteTokenResponseDTO(
                inviteToken.getId(),
                inviteToken.getEmail(),
                inviteToken.getRole().name(),
                inviteToken.getPosition().name(),
                inviteLink,
                inviteToken.getExpiresAt().format(ISO_FORMATTER)
        );
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

        // Revoke all refresh tokens so removed user gets logged out on next refresh
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

        return new InviteInfoResponseDTO(
                inviteToken.getWorkspace().getName(),
                inviteToken.getEmail(),
                inviteToken.getRole().name(),
                inviteToken.getPosition().name(),
                inviteToken.getInvitedBy().getName(),
                userExists
        );
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

        UserInfoDTO userInfo = new UserInfoDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                member.getWorkspace().getId(),
                member.getWorkspace().getName(),
                member.getRole().name(),
                member.getPosition().name()
        );

        return new TokenResponseDTO(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessExpirationMs(),
                userInfo
        );
    }
}
