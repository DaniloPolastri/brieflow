package com.briefflow.service.impl;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Member;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.ClientMapper;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.ClientService;
import com.briefflow.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ClientServiceImpl implements ClientService {

    private static final long MAX_LOGO_SIZE = 2L * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final String LOGO_SUBDIRECTORY = "logos";

    private final ClientRepository clientRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ClientMapper clientMapper;
    private final FileStorageService fileStorageService;

    public ClientServiceImpl(ClientRepository clientRepository,
                             MemberRepository memberRepository,
                             WorkspaceRepository workspaceRepository,
                             ClientMapper clientMapper,
                             FileStorageService fileStorageService) {
        this.clientRepository = clientRepository;
        this.memberRepository = memberRepository;
        this.workspaceRepository = workspaceRepository;
        this.clientMapper = clientMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public ClientResponseDTO create(ClientRequestDTO dto, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));

        Client client = clientMapper.toEntity(dto);
        client.setWorkspace(workspace);
        client.setActive(true);

        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClientResponseDTO update(Long clientId, ClientRequestDTO dto, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        clientMapper.updateEntity(dto, client);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO getById(Long clientId, Long workspaceId) {
        Client client = findClientInWorkspace(clientId, workspaceId);
        return clientMapper.toResponseDTO(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> list(Long workspaceId, String search, Boolean active) {
        boolean hasSearch = StringUtils.hasText(search);

        List<Client> clients;
        if (hasSearch && active != null) {
            clients = clientRepository.searchByNameOrCompanyAndActive(workspaceId, search.trim(), active);
        } else if (hasSearch) {
            clients = clientRepository.searchByNameOrCompany(workspaceId, search.trim());
        } else if (active != null) {
            clients = clientRepository.findByWorkspaceIdAndActiveOrderByNameAsc(workspaceId, active);
        } else {
            clients = clientRepository.findByWorkspaceIdOrderByNameAsc(workspaceId);
        }

        return clients.stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ClientResponseDTO toggleActive(Long clientId, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        client.setActive(!client.getActive());
        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClientResponseDTO uploadLogo(Long clientId, MultipartFile file, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        validateLogoFile(file);

        if (client.getLogoUrl() != null) {
            fileStorageService.delete(client.getLogoUrl());
        }

        String extension = getExtension(file.getOriginalFilename());
        String filename = "client-" + clientId + "-" + UUID.randomUUID() + "." + extension;
        String logoUrl = fileStorageService.store(file, LOGO_SUBDIRECTORY, filename);

        client.setLogoUrl(logoUrl);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void removeLogo(Long clientId, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        if (client.getLogoUrl() != null) {
            fileStorageService.delete(client.getLogoUrl());
            client.setLogoUrl(null);
            clientRepository.save(client);
        }
    }

    // --- Private helpers ---

    private void requireOwnerOrManager(Long userId, Long workspaceId) {
        Member member = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (member.getRole() == MemberRole.CREATIVE) {
            throw new ForbiddenException("Apenas proprietarios e gerentes podem gerenciar clientes");
        }
    }

    private Client findClientInWorkspace(Long clientId, Long workspaceId) {
        return clientRepository.findByIdAndWorkspaceId(clientId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado"));
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("O arquivo de logo nao pode estar vazio");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BusinessException("O logo deve ter no maximo 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException("Formato invalido. Apenas JPG e PNG sao permitidos");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
