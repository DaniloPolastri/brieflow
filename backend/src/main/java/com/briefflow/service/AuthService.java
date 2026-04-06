package com.briefflow.service;

import com.briefflow.dto.auth.*;

public interface AuthService {

    TokenResponseDTO register(RegisterRequestDTO request);

    TokenResponseDTO login(LoginRequestDTO request);

    TokenResponseDTO refresh(RefreshTokenRequestDTO request);

    void logout(RefreshTokenRequestDTO request);
}
