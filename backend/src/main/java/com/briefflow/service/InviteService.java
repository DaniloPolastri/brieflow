package com.briefflow.service;

import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.member.*;

public interface InviteService {
    InviteTokenResponseDTO inviteMember(Long workspaceId, Long userId, InviteMemberRequestDTO request);
    void cancelInvite(Long workspaceId, Long userId, Long inviteId);
    InviteInfoResponseDTO getInviteInfo(String token);
    TokenResponseDTO acceptInvite(String token, AcceptInviteRequestDTO request);
}
