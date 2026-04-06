package com.briefflow.service.impl;

import com.briefflow.dto.auth.*;
import com.briefflow.entity.RefreshToken;
import com.briefflow.entity.User;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.UnauthorizedException;
import com.briefflow.repository.RefreshTokenRepository;
import com.briefflow.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        return generateTokenResponse(user);
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

        return generateTokenResponse(user);
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
        return generateTokenResponse(user);
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

    private TokenResponseDTO generateTokenResponse(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        UserInfoDTO userInfo = new UserInfoDTO(user.getId(), user.getName(), user.getEmail());

        return new TokenResponseDTO(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessExpirationMs(),
                userInfo
        );
    }
}
