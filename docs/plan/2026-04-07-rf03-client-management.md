# RF03 — Client Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement CRUD of agency clients with search, status filter, logo upload, and card-based listing UI.

**Architecture:** Backend follows existing layered pattern (Controller → Service → Repository → MapStruct). Frontend adds a new `clients` feature module with card-based listing (new pattern) and a form dialog (same pattern as invite dialog). File upload infrastructure (FileStorageService) is new and reusable for RF06.

**Tech Stack:** Spring Boot 3 + JPA + MapStruct + Flyway | Angular 20 + PrimeNG 19 + Tailwind v4 | Vitest + JUnit 5 + Mockito

---

## Task Summary

### Backend
- [ ] Task B1: Migration V6 + Client Entity + DTOs + Repository ⚡ PARALLEL GROUP A
- [ ] Task B2: FileStorageService + WebConfig + SecurityConfig ⚡ PARALLEL GROUP A
- [ ] Task B3: ClientMapper (depends on B1)
- [ ] Task B4: ClientService interface + ClientServiceImpl + Tests (depends on B1, B2, B3)
- [ ] Task B5: ClientController (depends on B4)

### Frontend
- [ ] Task F1: Client Model + ClientApiService + Tests (depends on B5 backend) ⚡ PARALLEL GROUP B
- [ ] Task F2: Routes + Sidebar nav item ⚡ PARALLEL GROUP B
- [ ] Task F3: ClientFormDialogComponent + Tests (depends on F1) — **invoke `frontend-design` before template**
- [ ] Task F4: ClientListComponent + Tests (depends on F1, F2, F3) — **invoke `frontend-design` before template**

Nenhuma task frontend-backend paralelizável — backend deve estar pronto antes do frontend consumir.

---

## Backend Tasks

### Task B1: Migration V6 + Client Entity + DTOs + Repository ⚡ PARALLEL GROUP A

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__create_clients_table.sql`
- Create: `backend/src/main/java/com/briefflow/entity/Client.java`
- Create: `backend/src/main/java/com/briefflow/dto/client/ClientRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/client/ClientResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/repository/ClientRepository.java`

- [ ] **Step 1: Create migration V6**

```sql
-- V6__create_clients_table.sql
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    logo_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_workspace_id ON clients(workspace_id);
CREATE INDEX idx_clients_workspace_active ON clients(workspace_id, active);
```

- [ ] **Step 2: Create Client entity**

```java
package com.briefflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String company;

    private String email;

    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Create DTOs**

```java
// ClientRequestDTO.java
package com.briefflow.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequestDTO(
    @NotBlank String name,
    String company,
    @Email String email,
    String phone
) {}
```

```java
// ClientResponseDTO.java
package com.briefflow.dto.client;

public record ClientResponseDTO(
    Long id,
    String name,
    String company,
    String email,
    String phone,
    String logoUrl,
    Boolean active,
    String createdAt
) {}
```

- [ ] **Step 4: Create ClientRepository**

```java
package com.briefflow.repository;

import com.briefflow.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    List<Client> findByWorkspaceIdAndActiveOrderByNameAsc(Long workspaceId, Boolean active);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    List<Client> searchByNameOrCompany(@Param("workspaceId") Long workspaceId,
                                       @Param("search") String search);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND c.active = :active " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    List<Client> searchByNameOrCompanyAndActive(@Param("workspaceId") Long workspaceId,
                                                @Param("search") String search,
                                                @Param("active") Boolean active);

    Optional<Client> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
```

- [ ] **Step 5: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V6__create_clients_table.sql \
       backend/src/main/java/com/briefflow/entity/Client.java \
       backend/src/main/java/com/briefflow/dto/client/ \
       backend/src/main/java/com/briefflow/repository/ClientRepository.java
git commit -m "feat: add Client entity, DTOs, repository, and V6 migration"
```

---

### Task B2: FileStorageService + WebConfig + SecurityConfig ⚡ PARALLEL GROUP A

**Files:**
- Create: `backend/src/main/java/com/briefflow/service/FileStorageService.java`
- Create: `backend/src/main/java/com/briefflow/service/impl/FileStorageServiceImpl.java`
- Create: `backend/src/main/java/com/briefflow/config/WebConfig.java`
- Modify: `backend/src/main/java/com/briefflow/config/SecurityConfig.java:49-52`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/briefflow/unit/service/FileStorageServiceImplTest.java`

- [ ] **Step 1: Write FileStorageServiceImplTest (RED)**

```java
package com.briefflow.unit.service;

import com.briefflow.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(tempDir.toString());
        fileStorageService.init();
    }

    @Test
    void should_storeFile_when_validFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "fake-image-data".getBytes());

        String result = fileStorageService.store(file, "logos", "42.png");

        assertEquals("/uploads/logos/42.png", result);
        assertTrue(Files.exists(tempDir.resolve("logos/42.png")));
    }

    @Test
    void should_overwriteExisting_when_sameFilename() throws IOException {
        Path logosDir = tempDir.resolve("logos");
        Files.createDirectories(logosDir);
        Files.writeString(logosDir.resolve("42.png"), "old-data");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "new-data".getBytes());

        fileStorageService.store(file, "logos", "42.png");

        assertEquals("new-data", Files.readString(logosDir.resolve("42.png")));
    }

    @Test
    void should_deleteFile_when_exists() throws IOException {
        Path logosDir = tempDir.resolve("logos");
        Files.createDirectories(logosDir);
        Files.writeString(logosDir.resolve("42.png"), "data");

        fileStorageService.delete("/uploads/logos/42.png");

        assertFalse(Files.exists(logosDir.resolve("42.png")));
    }

    @Test
    void should_notThrow_when_deletingNonExistentFile() {
        assertDoesNotThrow(() -> fileStorageService.delete("/uploads/logos/999.png"));
    }

    @Test
    void should_notThrow_when_deletingNullUrl() {
        assertDoesNotThrow(() -> fileStorageService.delete(null));
    }

    @Test
    void should_notThrow_when_deletingBlankUrl() {
        assertDoesNotThrow(() -> fileStorageService.delete(""));
    }

    @Test
    void should_createSubdirectory_when_notExists() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "data".getBytes());

        fileStorageService.store(file, "logos", "1.png");

        assertTrue(Files.exists(tempDir.resolve("logos/1.png")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest="com.briefflow.unit.service.FileStorageServiceImplTest" -pl backend`
Expected: FAIL — classes don't exist yet

- [ ] **Step 3: Create FileStorageService interface**

```java
package com.briefflow.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String subdirectory, String filename);
    void delete(String relativeUrl);
}
```

- [ ] **Step 4: Create FileStorageServiceImpl**

```java
package com.briefflow.service.impl;

import com.briefflow.exception.FileStorageException;
import com.briefflow.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadRootPath;

    public FileStorageServiceImpl(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadRootPath);
        } catch (IOException e) {
            throw new FileStorageException("Nao foi possivel criar o diretorio de uploads", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subdirectory, String filename) {
        try {
            Path targetDir = uploadRootPath.resolve(subdirectory).normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename).normalize();
            if (!targetPath.startsWith(uploadRootPath)) {
                throw new FileStorageException("Caminho de arquivo invalido");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + subdirectory + "/" + filename;
        } catch (IOException e) {
            throw new FileStorageException("Erro ao salvar arquivo: " + filename, e);
        }
    }

    @Override
    public void delete(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return;
        }
        try {
            String pathWithinUploads = relativeUrl.replaceFirst("^/uploads/", "");
            Path filePath = uploadRootPath.resolve(pathWithinUploads).normalize();

            if (!filePath.startsWith(uploadRootPath)) {
                throw new FileStorageException("Caminho de arquivo invalido");
            }

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Erro ao deletar arquivo", e);
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest="com.briefflow.unit.service.FileStorageServiceImplTest"`
Expected: 7 tests PASS

- [ ] **Step 6: Create WebConfig for serving upload files**

```java
package com.briefflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}
```

- [ ] **Step 7: Update SecurityConfig to permit /uploads/***

In `backend/src/main/java/com/briefflow/config/SecurityConfig.java`, add before the existing `requestMatchers`:

```java
// Add this line:
.requestMatchers("/uploads/**").permitAll()
// Before:
.requestMatchers("/api/v1/auth/**").permitAll()
```

- [ ] **Step 8: Add upload-dir config to application.yml**

Add to `application.yml`:
```yaml
app:
  file:
    upload-dir: ./uploads
```

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/briefflow/service/FileStorageService.java \
       backend/src/main/java/com/briefflow/service/impl/FileStorageServiceImpl.java \
       backend/src/main/java/com/briefflow/config/WebConfig.java \
       backend/src/main/java/com/briefflow/config/SecurityConfig.java \
       backend/src/main/resources/application.yml \
       backend/src/test/java/com/briefflow/unit/service/FileStorageServiceImplTest.java
git commit -m "feat: add FileStorageService and static upload serving"
```

---

### Task B3: ClientMapper (depends on B1)

**Files:**
- Create: `backend/src/main/java/com/briefflow/mapper/ClientMapper.java`

- [ ] **Step 1: Create ClientMapper**

```java
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
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/briefflow/mapper/ClientMapper.java
git commit -m "feat: add ClientMapper with MapStruct"
```

---

### Task B4: ClientService + ClientServiceImpl + Tests (depends on B1, B2, B3)

**Files:**
- Create: `backend/src/main/java/com/briefflow/service/ClientService.java`
- Create: `backend/src/main/java/com/briefflow/service/impl/ClientServiceImpl.java`
- Create: `backend/src/test/java/com/briefflow/unit/service/ClientServiceImplTest.java`

- [ ] **Step 1: Write ClientServiceImplTest (RED)**

Full test file with 25 tests covering: create (owner, creative forbidden), update (success, not found), getById (success, not found), list (no filters, active filter, search, search+active, blank search), toggleActive (deactivate, activate, creative forbidden), uploadLogo (png, jpeg, delete previous, exceeds 2MB, invalid MIME, empty file, creative forbidden), removeLogo (exists, not exists, creative forbidden).

See complete test code in the design spec — tests follow exact patterns from `MemberServiceImplTest` and `InviteServiceImplTest`.

```java
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

    private User ownerUser;
    private User creativeUser;
    private Workspace workspace;
    private Member ownerMember;
    private Member creativeMember;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(
                clientRepository, memberRepository, workspaceRepository,
                clientMapper, fileStorageService);

        ownerUser = createUser(1L, "Owner", "owner@test.com");
        creativeUser = createUser(2L, "Creative", "creative@test.com");
        workspace = createWorkspace(1L, "Agency");
        ownerMember = createMember(1L, ownerUser, workspace, MemberRole.OWNER);
        creativeMember = createMember(2L, creativeUser, workspace, MemberRole.CREATIVE);
    }

    // --- create ---

    @Test
    void should_createClient_when_ownerCreates() {
        ClientRequestDTO dto = new ClientRequestDTO("Acme Corp", "Acme", "acme@test.com", "11999999999");
        Client savedClient = createClient(1L, "Acme Corp", workspace);
        ClientResponseDTO expected = new ClientResponseDTO(1L, "Acme Corp", "Acme", "acme@test.com", "11999999999", null, true, "2026-01-01T10:00:00");

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(clientMapper.toEntity(dto)).thenReturn(savedClient);
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(clientMapper.toResponseDTO(savedClient)).thenReturn(expected);

        ClientResponseDTO result = clientService.create(dto, 1L, 1L);

        assertNotNull(result);
        assertEquals("Acme Corp", result.name());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void should_throwForbidden_when_creativeCreatesClient() {
        ClientRequestDTO dto = new ClientRequestDTO("Acme", null, null, null);
        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(creativeMember));

        assertThrows(ForbiddenException.class, () -> clientService.create(dto, 1L, 2L));
        verify(clientRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void should_updateClient_when_ownerUpdates() {
        ClientRequestDTO dto = new ClientRequestDTO("Acme Updated", "Acme Inc", "new@acme.com", null);
        Client existing = createClient(1L, "Acme Corp", workspace);
        ClientResponseDTO expected = new ClientResponseDTO(1L, "Acme Updated", "Acme Inc", "new@acme.com", null, null, true, "2026-01-01T10:00:00");

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(any(Client.class))).thenReturn(existing);
        when(clientMapper.toResponseDTO(existing)).thenReturn(expected);

        ClientResponseDTO result = clientService.update(1L, dto, 1L, 1L);

        assertEquals("Acme Updated", result.name());
        verify(clientMapper).updateEntity(dto, existing);
    }

    @Test
    void should_throwNotFound_when_updatingNonExistentClient() {
        ClientRequestDTO dto = new ClientRequestDTO("Acme", null, null, null);
        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.update(999L, dto, 1L, 1L));
    }

    // --- getById ---

    @Test
    void should_getClientById_when_existsInWorkspace() {
        Client client = createClient(1L, "Acme Corp", workspace);
        ClientResponseDTO expected = new ClientResponseDTO(1L, "Acme Corp", null, null, null, null, true, "2026-01-01T10:00:00");

        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientMapper.toResponseDTO(client)).thenReturn(expected);

        ClientResponseDTO result = clientService.getById(1L, 1L);

        assertEquals("Acme Corp", result.name());
    }

    @Test
    void should_throwNotFound_when_clientNotInWorkspace() {
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> clientService.getById(1L, 1L));
    }

    // --- list ---

    @Test
    void should_listAllClients_when_noFilters() {
        Client c1 = createClient(1L, "Acme", workspace);
        Client c2 = createClient(2L, "Beta", workspace);
        when(clientRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of(c1, c2));
        when(clientMapper.toResponseDTO(any())).thenReturn(mock(ClientResponseDTO.class));

        List<ClientResponseDTO> result = clientService.list(1L, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void should_listActiveClients_when_activeFilterTrue() {
        when(clientRepository.findByWorkspaceIdAndActiveOrderByNameAsc(1L, true)).thenReturn(List.of());

        clientService.list(1L, null, true);

        verify(clientRepository).findByWorkspaceIdAndActiveOrderByNameAsc(1L, true);
    }

    @Test
    void should_searchClients_when_searchProvided() {
        when(clientRepository.searchByNameOrCompany(1L, "acm")).thenReturn(List.of());

        clientService.list(1L, "acm", null);

        verify(clientRepository).searchByNameOrCompany(1L, "acm");
    }

    @Test
    void should_searchWithActiveFilter_when_bothProvided() {
        when(clientRepository.searchByNameOrCompanyAndActive(1L, "acm", true)).thenReturn(List.of());

        clientService.list(1L, "acm", true);

        verify(clientRepository).searchByNameOrCompanyAndActive(1L, "acm", true);
    }

    @Test
    void should_treatBlankSearchAsNoSearch_when_blankString() {
        when(clientRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of());

        clientService.list(1L, "   ", null);

        verify(clientRepository).findByWorkspaceIdOrderByNameAsc(1L);
    }

    // --- toggleActive ---

    @Test
    void should_deactivateClient_when_currentlyActive() {
        Client client = createClient(1L, "Acme", workspace);
        client.setActive(true);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any())).thenReturn(client);
        when(clientMapper.toResponseDTO(client)).thenReturn(mock(ClientResponseDTO.class));

        clientService.toggleActive(1L, 1L, 1L);

        assertFalse(client.getActive());
        verify(clientRepository).save(client);
    }

    @Test
    void should_activateClient_when_currentlyInactive() {
        Client client = createClient(1L, "Acme", workspace);
        client.setActive(false);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any())).thenReturn(client);
        when(clientMapper.toResponseDTO(client)).thenReturn(mock(ClientResponseDTO.class));

        clientService.toggleActive(1L, 1L, 1L);

        assertTrue(client.getActive());
    }

    @Test
    void should_throwForbidden_when_creativeTogglesClient() {
        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(creativeMember));
        assertThrows(ForbiddenException.class, () -> clientService.toggleActive(1L, 1L, 2L));
    }

    // --- uploadLogo ---

    @Test
    void should_uploadLogo_when_validPngFile() {
        Client client = createClient(1L, "Acme", workspace);
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(fileStorageService.store(file, "logos", "1.png")).thenReturn("/uploads/logos/1.png");
        when(clientRepository.save(any())).thenReturn(client);
        when(clientMapper.toResponseDTO(client)).thenReturn(mock(ClientResponseDTO.class));

        clientService.uploadLogo(1L, file, 1L, 1L);

        assertEquals("/uploads/logos/1.png", client.getLogoUrl());
        verify(fileStorageService).store(file, "logos", "1.png");
    }

    @Test
    void should_deletePreviousLogo_when_uploadingNew() {
        Client client = createClient(1L, "Acme", workspace);
        client.setLogoUrl("/uploads/logos/1.png");
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));
        when(fileStorageService.store(file, "logos", "1.png")).thenReturn("/uploads/logos/1.png");
        when(clientRepository.save(any())).thenReturn(client);
        when(clientMapper.toResponseDTO(client)).thenReturn(mock(ClientResponseDTO.class));

        clientService.uploadLogo(1L, file, 1L, 1L);

        verify(fileStorageService).delete("/uploads/logos/1.png");
    }

    @Test
    void should_throwBusiness_when_logoExceeds2MB() {
        Client client = createClient(1L, "Acme", workspace);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", new byte[3 * 1024 * 1024]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
        verify(fileStorageService, never()).store(any(), any(), any());
    }

    @Test
    void should_throwBusiness_when_logoInvalidMimeType() {
        Client client = createClient(1L, "Acme", workspace);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    @Test
    void should_throwBusiness_when_logoFileEmpty() {
        Client client = createClient(1L, "Acme", workspace);
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessException.class, () -> clientService.uploadLogo(1L, file, 1L, 1L));
    }

    @Test
    void should_throwForbidden_when_creativeUploadsLogo() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[100]);
        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(creativeMember));

        assertThrows(ForbiddenException.class, () -> clientService.uploadLogo(1L, file, 1L, 2L));
    }

    // --- removeLogo ---

    @Test
    void should_removeLogo_when_logoExists() {
        Client client = createClient(1L, "Acme", workspace);
        client.setLogoUrl("/uploads/logos/1.png");

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        clientService.removeLogo(1L, 1L, 1L);

        assertNull(client.getLogoUrl());
        verify(fileStorageService).delete("/uploads/logos/1.png");
        verify(clientRepository).save(client);
    }

    @Test
    void should_doNothing_when_removingLogoThatDoesNotExist() {
        Client client = createClient(1L, "Acme", workspace);
        client.setLogoUrl(null);

        when(memberRepository.findByUserIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(clientRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(client));

        clientService.removeLogo(1L, 1L, 1L);

        verify(fileStorageService, never()).delete(any());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void should_throwForbidden_when_creativeRemovesLogo() {
        when(memberRepository.findByUserIdAndWorkspaceId(2L, 1L)).thenReturn(Optional.of(creativeMember));
        assertThrows(ForbiddenException.class, () -> clientService.removeLogo(1L, 1L, 2L));
    }

    // --- Helpers ---

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Workspace createWorkspace(Long id, String name) {
        Workspace ws = new Workspace();
        ws.setId(id);
        ws.setName(name);
        ws.setSlug(name.toLowerCase());
        return ws;
    }

    private Member createMember(Long id, User user, Workspace ws, MemberRole role) {
        Member m = new Member();
        m.setId(id);
        m.setUser(user);
        m.setWorkspace(ws);
        m.setRole(role);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    private Client createClient(Long id, String name, Workspace ws) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        c.setWorkspace(ws);
        c.setActive(true);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest="com.briefflow.unit.service.ClientServiceImplTest"`
Expected: FAIL — ClientServiceImpl doesn't exist

- [ ] **Step 3: Create ClientService interface**

```java
package com.briefflow.service;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClientService {
    ClientResponseDTO create(ClientRequestDTO dto, Long workspaceId, Long userId);
    ClientResponseDTO update(Long clientId, ClientRequestDTO dto, Long workspaceId, Long userId);
    ClientResponseDTO getById(Long clientId, Long workspaceId);
    List<ClientResponseDTO> list(Long workspaceId, String search, Boolean active);
    ClientResponseDTO toggleActive(Long clientId, Long workspaceId, Long userId);
    ClientResponseDTO uploadLogo(Long clientId, MultipartFile file, Long workspaceId, Long userId);
    void removeLogo(Long clientId, Long workspaceId, Long userId);
}
```

- [ ] **Step 4: Create ClientServiceImpl**

Full implementation with: `requireOwnerOrManager()`, `findClientInWorkspace()`, `validateLogoFile()`, `getExtension()` helpers. CRUD methods, toggle, upload logo (validates 2MB + JPG/PNG, deletes previous), remove logo. See complete code in Task B9 of the backend architect output.

```java
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
public class ClientServiceImpl implements ClientService {

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024;
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
        requireOwnerOrManager(workspaceId, userId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace nao encontrado"));

        Client client = clientMapper.toEntity(dto);
        client.setWorkspace(workspace);
        client.setActive(true);
        client = clientRepository.save(client);
        return clientMapper.toResponseDTO(client);
    }

    @Override
    @Transactional
    public ClientResponseDTO update(Long clientId, ClientRequestDTO dto, Long workspaceId, Long userId) {
        requireOwnerOrManager(workspaceId, userId);
        Client client = findClientInWorkspace(clientId, workspaceId);
        clientMapper.updateEntity(dto, client);
        client = clientRepository.save(client);
        return clientMapper.toResponseDTO(client);
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
        List<Client> clients;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && active != null) {
            clients = clientRepository.searchByNameOrCompanyAndActive(workspaceId, search, active);
        } else if (hasSearch) {
            clients = clientRepository.searchByNameOrCompany(workspaceId, search);
        } else if (active != null) {
            clients = clientRepository.findByWorkspaceIdAndActiveOrderByNameAsc(workspaceId, active);
        } else {
            clients = clientRepository.findByWorkspaceIdOrderByNameAsc(workspaceId);
        }

        return clients.stream().map(clientMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public ClientResponseDTO toggleActive(Long clientId, Long workspaceId, Long userId) {
        requireOwnerOrManager(workspaceId, userId);
        Client client = findClientInWorkspace(clientId, workspaceId);
        client.setActive(!client.getActive());
        client = clientRepository.save(client);
        return clientMapper.toResponseDTO(client);
    }

    @Override
    @Transactional
    public ClientResponseDTO uploadLogo(Long clientId, MultipartFile file, Long workspaceId, Long userId) {
        requireOwnerOrManager(workspaceId, userId);
        Client client = findClientInWorkspace(clientId, workspaceId);
        validateLogoFile(file);

        if (client.getLogoUrl() != null) {
            fileStorageService.delete(client.getLogoUrl());
        }

        String extension = getExtension(file.getOriginalFilename());
        String filename = clientId + "." + extension;
        String logoUrl = fileStorageService.store(file, LOGO_SUBDIRECTORY, filename);

        client.setLogoUrl(logoUrl);
        client = clientRepository.save(client);
        return clientMapper.toResponseDTO(client);
    }

    @Override
    @Transactional
    public void removeLogo(Long clientId, Long workspaceId, Long userId) {
        requireOwnerOrManager(workspaceId, userId);
        Client client = findClientInWorkspace(clientId, workspaceId);

        if (client.getLogoUrl() != null) {
            fileStorageService.delete(client.getLogoUrl());
            client.setLogoUrl(null);
            clientRepository.save(client);
        }
    }

    private void requireOwnerOrManager(Long workspaceId, Long userId) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));
        if (caller.getRole() != MemberRole.OWNER && caller.getRole() != MemberRole.MANAGER) {
            throw new ForbiddenException("Apenas proprietarios e gerentes podem gerenciar clientes");
        }
    }

    private Client findClientInWorkspace(Long clientId, Long workspaceId) {
        return clientRepository.findByIdAndWorkspaceId(clientId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado"));
    }

    private void validateLogoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Arquivo vazio");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BusinessException("Arquivo excede o tamanho maximo de 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException("Tipo de arquivo nao permitido. Apenas JPG e PNG sao aceitos");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest="com.briefflow.unit.service.ClientServiceImplTest"`
Expected: 22 tests PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/briefflow/service/ClientService.java \
       backend/src/main/java/com/briefflow/service/impl/ClientServiceImpl.java \
       backend/src/test/java/com/briefflow/unit/service/ClientServiceImplTest.java
git commit -m "feat: add ClientService with CRUD, toggle, and logo upload"
```

---

### Task B5: ClientController (depends on B4)

**Files:**
- Create: `backend/src/main/java/com/briefflow/controller/ClientController.java`

- [ ] **Step 1: Create ClientController**

```java
package com.briefflow.controller;

import com.briefflow.dto.client.ClientRequestDTO;
import com.briefflow.dto.client.ClientResponseDTO;
import com.briefflow.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(
            @Valid @RequestBody ClientRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> list(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(clientService.list(workspaceId, search, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId) {
        return ResponseEntity.ok(clientService.getById(id, workspaceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.update(id, dto, workspaceId, userId));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ClientResponseDTO> toggleActive(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.toggleActive(id, workspaceId, userId));
    }

    @PostMapping("/{id}/logo")
    public ResponseEntity<ClientResponseDTO> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(clientService.uploadLogo(id, file, workspaceId, userId));
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> removeLogo(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId) {
        clientService.removeLogo(id, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Run all backend tests**

Run: `./mvnw test -Dtest="com.briefflow.unit.**"`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/briefflow/controller/ClientController.java
git commit -m "feat: add ClientController with CRUD, toggle, and logo endpoints"
```

---

## Frontend Tasks

### Task F1: Client Model + ClientApiService + Tests ⚡ PARALLEL GROUP B

**Files:**
- Create: `frontend/src/app/features/clients/models/client.model.ts`
- Create: `frontend/src/app/features/clients/services/client-api.service.ts`
- Create: `frontend/src/app/features/clients/services/client-api.service.spec.ts`

- [ ] **Step 1: Create client.model.ts**

```typescript
export interface Client {
  id: number;
  name: string;
  company: string | null;
  email: string | null;
  phone: string | null;
  logoUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface ClientRequest {
  name: string;
  company?: string;
  email?: string;
  phone?: string;
}
```

- [ ] **Step 2: Write client-api.service.spec.ts (RED)**

Full test file with 10 tests: list without params, list with search, list with active, list with both, getById, create, update, toggleActive, uploadLogo, removeLogo. Uses HttpTestingController pattern from existing services.

See complete test code in frontend architect output.

- [ ] **Step 3: Create client-api.service.ts (GREEN)**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Client, ClientRequest } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/clients`;

  list(params?: { search?: string; active?: boolean }): Observable<Client[]> {
    let httpParams = new HttpParams();
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.active !== undefined) httpParams = httpParams.set('active', String(params.active));
    return this.http.get<Client[]>(this.baseUrl, { params: httpParams });
  }

  getById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.baseUrl}/${id}`);
  }

  create(request: ClientRequest): Observable<Client> {
    return this.http.post<Client>(this.baseUrl, request);
  }

  update(id: number, request: ClientRequest): Observable<Client> {
    return this.http.put<Client>(`${this.baseUrl}/${id}`, request);
  }

  toggleActive(id: number): Observable<Client> {
    return this.http.patch<Client>(`${this.baseUrl}/${id}/toggle`, {});
  }

  uploadLogo(id: number, file: File): Observable<Client> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Client>(`${this.baseUrl}/${id}/logo`, formData);
  }

  removeLogo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/logo`);
  }
}
```

- [ ] **Step 4: Run tests**

Run: `npx ng test --watch=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/clients/
git commit -m "feat: add Client model and ClientApiService with tests"
```

---

### Task F2: Routes + Sidebar nav item ⚡ PARALLEL GROUP B

**Files:**
- Create: `frontend/src/app/features/clients/clients.routes.ts`
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/layout/sidebar/sidebar.component.ts`

- [ ] **Step 1: Create clients.routes.ts**

```typescript
import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/client-list/client-list.component').then(m => m.ClientListComponent),
  },
];

export default routes;
```

- [ ] **Step 2: Add clients route to app.routes.ts**

Add after the `dashboard` route, before `members`:

```typescript
{
  path: 'clients',
  loadChildren: () => import('./features/clients/clients.routes'),
},
```

- [ ] **Step 3: Add Clientes nav item to sidebar**

In `sidebar.component.ts`, add after Dashboard:

```typescript
readonly navItems: NavItem[] = [
  { label: 'Dashboard', icon: 'pi pi-objects-column', route: '/dashboard' },
  { label: 'Clientes', icon: 'pi pi-building', route: '/clients' },
  { label: 'Equipe', icon: 'pi pi-users', route: '/members' },
  { label: 'Configurações', icon: 'pi pi-cog', route: '/settings', roles: ['OWNER', 'MANAGER'] },
];
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/clients/clients.routes.ts \
       frontend/src/app/app.routes.ts \
       frontend/src/app/layout/sidebar/sidebar.component.ts
git commit -m "feat: add clients route and sidebar nav item"
```

---

### Task F3: ClientFormDialogComponent + Tests (depends on F1)

**IMPORTANT:** Invoke `frontend-design` skill BEFORE implementing the template.

**Files:**
- Create: `frontend/src/app/features/clients/components/client-form-dialog/client-form-dialog.component.ts`
- Create: `frontend/src/app/features/clients/components/client-form-dialog/client-form-dialog.component.html`
- Create: `frontend/src/app/features/clients/components/client-form-dialog/client-form-dialog.component.spec.ts`

- [ ] **Step 1: Write client-form-dialog.component.spec.ts (RED)**

11 tests: creation, invalid form empty name, valid form name only, invalid email, calls create on submit, excludes empty optional fields, loading state, error on failure, reset on hide, validate file type, validate file size.

See complete test code in frontend architect output.

- [ ] **Step 2: Invoke `frontend-design` for the template**

- [ ] **Step 3: Create component TS + HTML (GREEN)**

Component with: `visible` (model), `client` (input), `saved` (output). Form with name (required), company, email (with email validator), phone. Logo upload with client-side validation (type + size), preview, remove. Effect to populate form in edit mode.

See complete code in frontend architect output.

- [ ] **Step 4: Run tests**

Run: `npx ng test --watch=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/clients/components/client-form-dialog/
git commit -m "feat: add ClientFormDialogComponent with logo upload"
```

---

### Task F4: ClientListComponent + Tests (depends on F1, F2, F3)

**IMPORTANT:** Invoke `frontend-design` skill BEFORE implementing the template.

**Files:**
- Create: `frontend/src/app/features/clients/pages/client-list/client-list.component.ts`
- Create: `frontend/src/app/features/clients/pages/client-list/client-list.component.html`
- Create: `frontend/src/app/features/clients/pages/client-list/client-list.component.spec.ts`

- [ ] **Step 1: Write client-list.component.spec.ts (RED)**

14 tests: creation, load on init, default active filter, display names, show create button for OWNER, show for MANAGER, hide for CREATIVE, hide menus for CREATIVE, empty state, getInitials, getAvatarColor, toggle with confirmation, open create dialog, open edit dialog, menu items for active/inactive.

See complete test code in frontend architect output.

- [ ] **Step 2: Invoke `frontend-design` for the template**

- [ ] **Step 3: Create component TS + HTML (GREEN)**

Component with: card grid (3 cols), search bar with debounce 300ms, status filter (Todos/Ativos/Inativos), avatar with initials or logo, action menu (edit, toggle), confirmation dialog, form dialog integration.

See complete code in frontend architect output.

- [ ] **Step 4: Run tests**

Run: `npx ng test --watch=false`
Expected: All PASS

- [ ] **Step 5: Run ng build to verify production build**

Run: `npx ng build`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/clients/pages/client-list/
git commit -m "feat: add ClientListComponent with card grid and search"
```
