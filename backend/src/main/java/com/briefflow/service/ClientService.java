package com.briefflow.service;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClientService {
    ClientResponseDTO create(ClientRequestDTO dto, Long workspaceId, Long userId);
    ClientResponseDTO update(Long clientId, ClientRequestDTO dto, Long workspaceId, Long userId);
    ClientResponseDTO getById(Long clientId, Long workspaceId, Long userId);
    List<ClientResponseDTO> list(Long workspaceId, Long userId, String search, Boolean active);
    ClientResponseDTO toggleActive(Long clientId, Long workspaceId, Long userId);
    ClientResponseDTO uploadLogo(Long clientId, MultipartFile file, Long workspaceId, Long userId);
    void removeLogo(Long clientId, Long workspaceId, Long userId);
    void assignMembers(Long clientId, List<Long> memberIds, Long workspaceId, Long userId);
    void unassignMember(Long clientId, Long memberId, Long workspaceId, Long userId);
    List<Long> getAssignedMemberIds(Long clientId, Long workspaceId);
}
