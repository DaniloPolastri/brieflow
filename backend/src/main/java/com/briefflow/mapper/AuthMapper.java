package com.briefflow.mapper;

import com.briefflow.dto.auth.UserInfoDTO;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.name", target = "name")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "member.workspace.id", target = "workspaceId")
    @Mapping(source = "member.workspace.name", target = "workspaceName")
    @Mapping(source = "member.role", target = "role")
    @Mapping(source = "member.position", target = "position")
    UserInfoDTO toUserInfoDTO(User user, Member member);

    default String mapRole(MemberRole role) {
        return role != null ? role.name() : null;
    }

    default String mapPosition(MemberPosition position) {
        return position != null ? position.name() : null;
    }
}
