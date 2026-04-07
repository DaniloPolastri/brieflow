package com.briefflow.mapper;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponseDTO toResponseDTO(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Client toEntity(ClientRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ClientRequestDTO dto, @MappingTarget Client client);

    default String mapDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
