package com.briefflow.mapper;

import com.briefflow.dto.member.MemberResponseDTO;
import com.briefflow.entity.Member;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "position", target = "position")
    @Mapping(source = "createdAt", target = "createdAt")
    MemberResponseDTO toResponseDTO(Member member);

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
