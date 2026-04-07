package com.briefflow.mapper;

import com.briefflow.dto.member.InviteInfoResponseDTO;
import com.briefflow.dto.member.InviteTokenResponseDTO;
import com.briefflow.entity.InviteToken;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface InviteMapper {

    @Mapping(source = "inviteToken.id", target = "id")
    @Mapping(source = "inviteToken.email", target = "email")
    @Mapping(source = "inviteToken.role", target = "role")
    @Mapping(source = "inviteToken.position", target = "position")
    @Mapping(source = "inviteLink", target = "inviteLink")
    @Mapping(source = "inviteToken.expiresAt", target = "expiresAt")
    InviteTokenResponseDTO toTokenResponseDTO(InviteToken inviteToken, String inviteLink);

    @Mapping(source = "inviteToken.workspace.name", target = "workspaceName")
    @Mapping(source = "inviteToken.email", target = "email")
    @Mapping(source = "inviteToken.role", target = "role")
    @Mapping(source = "inviteToken.position", target = "position")
    @Mapping(source = "inviteToken.invitedBy.name", target = "invitedByName")
    @Mapping(source = "userExists", target = "userExists")
    InviteInfoResponseDTO toInfoResponseDTO(InviteToken inviteToken, boolean userExists);

    default String mapRole(MemberRole role) {
        return role != null ? role.name() : null;
    }

    default String mapPosition(MemberPosition position) {
        return position != null ? position.name() : null;
    }

    default String mapDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
