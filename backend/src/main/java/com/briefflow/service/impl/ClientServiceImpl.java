package com.briefflow.service.impl;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.ClientMember;
import com.briefflow.entity.Member;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.ClientMapper;
import com.briefflow.repository.ClientMemberRepository;
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
    private final ClientMemberRepository clientMemberRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ClientMapper clientMapper;
    private final FileStorageService fileStorageService;

    public ClientServiceImpl(ClientRepository clientRepository,
                             ClientMemberRepository clientMemberRepository,
                             MemberRepository memberRepository,
                             WorkspaceRepository workspaceRepository,
                             ClientMapper clientMapper,
                             FileStorageService fileStorageService) {
        this.clientRepository = clientRepository;
        this.clientMemberRepository = clientMemberRepository;
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
        normalizeBlankFields(client);

        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClientResponseDTO update(Long clientId, ClientRequestDTO dto, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        clientMapper.updateEntity(dto, client);
        normalizeBlankFields(client);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO getById(Long clientId, Long workspaceId, Long userId) {
        Client client = findClientInWorkspace(clientId, workspaceId);

        Member member = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        if (member.getRole() == MemberRole.CREATIVE
                && !clientMemberRepository.existsByClientIdAndMemberId(clientId, member.getId())) {
            throw new ForbiddenException("Voce nao tem acesso a este cliente");
        }

        return clientMapper.toResponseDTO(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> list(Long workspaceId, Long userId, String search, Boolean active) {
        Member member = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        List<Client> clients;

        if (member.getRole() == MemberRole.CREATIVE) {
            clients = listForCreative(member.getId(), workspaceId, search, active);
        } else {
            clients = listForOwnerOrManager(workspaceId, search, active);
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

    @Override
    @Transactional
    public void assignMembers(Long clientId, List<Long> memberIds, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        findClientInWorkspace(clientId, workspaceId);

        // Validate all members exist in workspace before modifying
        for (Long memberId : memberIds) {
            memberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado: " + memberId));
        }

        // Sync: delete all existing, then re-create from incoming list
        clientMemberRepository.deleteByClientId(clientId);

        for (Long memberId : memberIds) {
            ClientMember clientMember = new ClientMember();
            clientMember.setClientId(clientId);
            clientMember.setMemberId(memberId);
            clientMemberRepository.save(clientMember);
        }
    }

    @Override
    @Transactional
    public void unassignMember(Long clientId, Long memberId, Long workspaceId, Long userId) {
        requireOwnerOrManager(userId, workspaceId);
        findClientInWorkspace(clientId, workspaceId);
        if (memberRepository.findByIdAndWorkspaceId(memberId, workspaceId).isEmpty()) {
            throw new ResourceNotFoundException("Membro nao encontrado");
        }

        clientMemberRepository.deleteByClientIdAndMemberId(clientId, memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getAssignedMemberIds(Long clientId, Long workspaceId) {
        findClientInWorkspace(clientId, workspaceId);
        return clientMemberRepository.findByClientId(clientId).stream()
                .map(ClientMember::getMemberId)
                .toList();
    }

    // --- Private helpers ---

    private List<Client> listForOwnerOrManager(Long workspaceId, String search, Boolean active) {
        boolean hasSearch = StringUtils.hasText(search);

        if (hasSearch && active != null) {
            return clientRepository.searchByNameOrCompanyAndActive(workspaceId, search.trim(), active);
        } else if (hasSearch) {
            return clientRepository.searchByNameOrCompany(workspaceId, search.trim());
        } else if (active != null) {
            return clientRepository.findByWorkspaceIdAndActiveOrderByNameAsc(workspaceId, active);
        } else {
            return clientRepository.findByWorkspaceIdOrderByNameAsc(workspaceId);
        }
    }

    private List<Client> listForCreative(Long memberId, Long workspaceId, String search, Boolean active) {
        boolean hasSearch = StringUtils.hasText(search);

        if (hasSearch && active != null) {
            return clientRepository.searchByMemberIdAndNameOrCompanyAndActive(memberId, workspaceId, search.trim(), active);
        } else if (hasSearch) {
            return clientRepository.searchByMemberIdAndNameOrCompany(memberId, workspaceId, search.trim());
        } else if (active != null) {
            return clientRepository.findByMemberIdAndWorkspaceIdAndActive(memberId, workspaceId, active);
        } else {
            return clientRepository.findByMemberIdAndWorkspaceId(memberId, workspaceId);
        }
    }

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

    private void normalizeBlankFields(Client client) {
        if (client.getEmail() != null && client.getEmail().isBlank()) {
            client.setEmail(null);
        }
        if (client.getPhone() != null && client.getPhone().isBlank()) {
            client.setPhone(null);
        }
        if (client.getCompany() != null && client.getCompany().isBlank()) {
            client.setCompany(null);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
