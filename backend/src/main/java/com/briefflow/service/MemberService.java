package com.briefflow.service;

import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.member.*;

public interface MemberService {
    MembersListResponseDTO listMembers(Long workspaceId);
    InviteTokenResponseDTO inviteMember(Long workspaceId, Long userId, InviteMemberRequestDTO request);
    void removeMember(Long workspaceId, Long userId, Long memberId);
    void updateMemberRole(Long workspaceId, Long userId, Long memberId, UpdateMemberRoleRequestDTO request);
    InviteInfoResponseDTO getInviteInfo(String token);
    TokenResponseDTO acceptInvite(String token, AcceptInviteRequestDTO request);
}
