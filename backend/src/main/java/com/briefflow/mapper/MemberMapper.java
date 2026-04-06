package com.briefflow.mapper;

import com.briefflow.dto.member.MemberResponseDTO;
import com.briefflow.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(target = "role", expression = "java(member.getRole().name())")
    @Mapping(target = "position", expression = "java(member.getPosition().name())")
    @Mapping(target = "createdAt", expression = "java(member.getCreatedAt().toString())")
    MemberResponseDTO toResponseDTO(Member member);
}
