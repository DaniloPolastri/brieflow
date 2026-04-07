package com.briefflow.unit.service;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.ClientMapper;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.FileStorageService;
import com.briefflow.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private ClientMapper clientMapper;
    @Mock private FileStorageService fileStorageService;

    private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(
                clientRepository, memberRepository, workspaceRepository,
                clientMapper, fileStorageService);

        lenient().when(clientMapper.toResponseDTO(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            return new ClientResponseDTO(
                    c.getId(),
                    c.getName(),
                    c.getCompany(),
                    c.getEmail(),
                    c.getPhone(),
                    c.getLogoUrl(),
                    c.getActive(),
                    c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
            );
        });
    }

    // --- create ---

    @Test
    void should_createClient_when_ownerCreates() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        ClientRequestDTO dto = new ClientRequestDTO("Acme Corp", "Acme", "acme@test.com", "11999999999");

        Client entity = new Client();
        entity.setId(1L);
        entity.setName("Acme Corp");
        entity.setCompany("Acme");
        entity.setEmail("acme@test.com");
        entity.setPhone("11999999999");
        entity.setActive(true);
        entity.setWorkspace(workspace);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(clientMapper.toEntity(dto)).thenReturn(entity);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponseDTO result = clientService.create(dto, 1L, 1L);

        assertNotNull(result);
        assertEquals("Acme Corp", result.name());
        assertEquals(true, result.active());
        verify(clientRepository).save(entity);
    }

    @Test
    void should_throwForbidden_when_creativeCreatesClient() {
        User user = createUser(1L, "Creative", "creative@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.CREATIVE);
        ClientRequestDTO dto = new ClientRequestDTO("Acme Corp", null, null, null);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () -> clientService.create(dto, 1L, 1L));
    }

    // --- update ---

    @Test
    void should_updateClient_when_ownerUpdates() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Old Name", workspace);
        ClientRequestDTO dto = new ClientRequestDTO("New Name", "New Co", null, null);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        doAnswer(inv -> {
            client.setName("New Name");
            client.setCompany("New Co");
            return null;
        }).when(clientMapper).updateEntity(eq(dto), eq(client));
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponseDTO result = clientService.update(1L, dto, 1L, 1L);

        assertNotNull(result);
        verify(clientMapper).updateEntity(dto, client);
        verify(clientRepository).save(client);
    }

    @Test
    void should_throwNotFound_when_updatingNonExistentClient() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        ClientRequestDTO dto = new ClientRequestDTO("New Name", null, null, null);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.update(99L, dto, 1L, 1L));
    }

    // --- getById ---

    @Test
    void should_getClientById_when_existsInWorkspace() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client = createClient(1L, "Acme Corp", workspace);

        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        ClientResponseDTO result = clientService.getById(1L, 1L);

        assertNotNull(result);
        assertEquals("Acme Corp", result.name());
    }

    @Test
    void should_throwNotFound_when_clientNotInWorkspace() {
        when(clientRepository.findByIdAndWorkspaceId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.getById(99L, 1L));
    }

    // --- list ---

    @Test
    void should_listAllClients_when_noFilters() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client1 = createClient(1L, "Alpha", workspace);
        Client client2 = createClient(2L, "Beta", workspace);

        when(clientRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of(client1, client2));

        List<ClientResponseDTO> result = clientService.list(1L, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void should_listActiveClients_when_activeFilterTrue() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client = createClient(1L, "Alpha", workspace);

        when(clientRepository.findByWorkspaceIdAndActiveOrderByNameAsc(1L, true)).thenReturn(List.of(client));

        List<ClientResponseDTO> result = clientService.list(1L, null, true);

        assertEquals(1, result.size());
    }

    @Test
    void should_searchClients_when_searchProvided() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client = createClient(1L, "Acme Corp", workspace);

        when(clientRepository.searchByNameOrCompany(1L, "acme")).thenReturn(List.of(client));

        List<ClientResponseDTO> result = clientService.list(1L, "acme", null);

        assertEquals(1, result.size());
        assertEquals("Acme Corp", result.get(0).name());
    }

    @Test
    void should_searchWithActiveFilter_when_bothProvided() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client = createClient(1L, "Acme Corp", workspace);

        when(clientRepository.searchByNameOrCompanyAndActive(1L, "acme", true)).thenReturn(List.of(client));

        List<ClientResponseDTO> result = clientService.list(1L, "acme", true);

        assertEquals(1, result.size());
    }

    @Test
    void should_treatBlankSearchAsNoSearch_when_blankString() {
        Workspace workspace = createWorkspace(1L, "Agency");
        Client client = createClient(1L, "Alpha", workspace);

        when(clientRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of(client));

        List<ClientResponseDTO> result = clientService.list(1L, "   ", null);

        assertEquals(1, result.size());
        verify(clientRepository).findByWorkspaceIdOrderByNameAsc(1L);
        verify(clientRepository, never()).searchByNameOrCompany(anyLong(), anyString());
    }

    // --- toggleActive ---

    @Test
    void should_deactivateClient_when_currentlyActive() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        client.setActive(true);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponseDTO result = clientService.toggleActive(1L, 1L, 1L);

        assertFalse(result.active());
        verify(clientRepository).save(client);
    }

    @Test
    void should_activateClient_when_currentlyInactive() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        client.setActive(false);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponseDTO result = clientService.toggleActive(1L, 1L, 1L);

        assertTrue(result.active());
    }

    @Test
    void should_throwForbidden_when_creativeTogglesClient() {
        User user = createUser(1L, "Creative", "creative@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.CREATIVE);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () -> clientService.toggleActive(1L, 1L, 1L));
    }

    // --- uploadLogo ---

    @Test
    void should_uploadLogo_when_validPngFile() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(fileStorageService.store(eq(file), eq("logos"), anyString())).thenReturn("/logos/logo.png");
        when(clientRepository.save(client)).thenReturn(client);

        ClientResponseDTO result = clientService.uploadLogo(1L, file, 1L, 1L);

        assertNotNull(result);
        assertEquals("/logos/logo.png", result.logoUrl());
        verify(fileStorageService).store(eq(file), eq("logos"), anyString());
    }

    @Test
    void should_deletePreviousLogo_when_uploadingNew() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        client.setLogoUrl("/logos/old-logo.png");
        MockMultipartFile file = new MockMultipartFile(
                "logo", "new-logo.png", "image/png", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(fileStorageService.store(eq(file), eq("logos"), anyString())).thenReturn("/logos/new-logo.png");
        when(clientRepository.save(client)).thenReturn(client);

        clientService.uploadLogo(1L, file, 1L, 1L);

        verify(fileStorageService).delete("/logos/old-logo.png");
    }

    @Test
    void should_throwBusiness_when_logoExceeds2MB() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "big.png", "image/png", new byte[3 * 1024 * 1024]); // 3MB

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    @Test
    void should_throwBusiness_when_logoInvalidMimeType() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "doc.pdf", "application/pdf", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    @Test
    void should_throwBusiness_when_logoFileEmpty() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "empty.png", "image/png", new byte[0]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    @Test
    void should_throwForbidden_when_creativeUploadsLogo() {
        User user = createUser(1L, "Creative", "creative@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.CREATIVE);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    // --- removeLogo ---

    @Test
    void should_removeLogo_when_logoExists() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        client.setLogoUrl("/logos/existing-logo.png");

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        clientService.removeLogo(1L, 1L, 1L);

        verify(fileStorageService).delete("/logos/existing-logo.png");
        assertNull(client.getLogoUrl());
        verify(clientRepository).save(client);
    }

    @Test
    void should_doNothing_when_removingLogoThatDoesNotExist() {
        User user = createUser(1L, "Owner", "owner@test.com");
        Workspace workspace = createWorkspace(1L, "Agency");
        Member member = createMember(1L, user, workspace, MemberRole.OWNER);
        Client client = createClient(1L, "Acme Corp", workspace);
        client.setLogoUrl(null);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(member));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        clientService.removeLogo(1L, 1L, 1L);

        verify(fileStorageService, never()).delete(anyString());
        verify(clientRepository, never()).save(any());
    }

    // --- Helper methods ---

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Workspace createWorkspace(Long id, String name) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName(name);
        workspace.setSlug(name.toLowerCase().replaceAll("\\s+", "-"));
        return workspace;
    }

    private Member createMember(Long id, User user, Workspace workspace, MemberRole role) {
        Member member = new Member();
        member.setId(id);
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(role);
        member.setCreatedAt(LocalDateTime.now());
        return member;
    }

    private Client createClient(Long id, String name, Workspace workspace) {
        Client client = new Client();
        client.setId(id);
        client.setName(name);
        client.setActive(true);
        client.setWorkspace(workspace);
        client.setCreatedAt(LocalDateTime.now());
        return client;
    }
}
