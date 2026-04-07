package com.briefflow.service;

import com.briefflow.dto.member.*;

public interface MemberService {
    MembersListResponseDTO listMembers(Long workspaceId);
    void removeMember(Long workspaceId, Long userId, Long memberId);
    void updateMemberRole(Long workspaceId, Long userId, Long memberId, UpdateMemberRoleRequestDTO request);
}
