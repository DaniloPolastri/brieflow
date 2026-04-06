package com.briefflow.mapper;

import com.briefflow.dto.workspace.WorkspaceResponseDTO;
import com.briefflow.entity.Workspace;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {
    WorkspaceResponseDTO toResponseDTO(Workspace workspace);
}
