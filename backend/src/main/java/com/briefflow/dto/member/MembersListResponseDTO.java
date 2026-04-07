package com.briefflow.dto.member;

import java.util.List;

public record MembersListResponseDTO(
    List<MemberResponseDTO> members,
    List<InviteTokenResponseDTO> pendingInvites
) {}
