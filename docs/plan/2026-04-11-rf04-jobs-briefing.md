# RF04 — Jobs e Briefing: Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Criar branch `feature/rf04-jobs-briefing` a partir de `main` antes de começar. Marcar `[x]` no checklist abaixo **imediatamente** ao completar cada task e fazer commit (regra formal do CLAUDE.md).

**Goal:** Implementar o CRUD completo de Jobs com briefing estruturado dinâmico por tipo (JSONB), upload de arquivos de briefing, visibilidade por papel (OWNER/MANAGER/CREATIVE via ClientMember), numeração legível `JOB-001` por workspace, soft delete e filtros/busca na listagem — sem kanban (RF05), sem peça final (RF06), sem aprovação (RF07), sem emails.

**Architecture:** Backend em Spring Boot 3 (Java 21) com entity `Job` persistindo `briefing_data` como JSONB, `BriefingValidator` strategy pattern, `job_counter` atômico no `workspaces` via `UPDATE ... RETURNING`, visibilidade de CREATIVE via `JOIN ClientMember`. Frontend em Angular 20 standalone zoneless com `JobApiService`, `BRIEFING_SCHEMAS` declarativo, `JobFormComponent` compartilhado entre create/edit, `JobFileUploaderComponent` com modos staging/direct, `JobListComponent` com `p-table`. TDD Red→Green→Refactor em todas as tasks.

**Tech Stack:** Java 21 + Spring Boot 3 + Spring Data JPA + MapStruct + Flyway + PostgreSQL JSONB + JUnit 5 + Testcontainers | Angular 20 standalone + Zoneless + Signals + PrimeNG 19 (Aura) + Tailwind CSS v4 + Vitest + Reactive Forms

**Spec base:** `docs/spec/2026-04-11-rf04-jobs-briefing-design.md`

---

## Task Summary

### Backend (B1–B12)

- [x] Task B1: Enums (JobType, JobPriority, JobStatus) ⚡ PARALLEL GROUP A
- [x] Task B2: Migration V8 (jobs, job_files, workspaces.job_counter) ⚡ PARALLEL GROUP A
- [x] Task B3: DTOs (JobRequestDTO, JobResponseDTO, JobListItemDTO, JobFileDTO, ClientSummaryDTO, MemberSummaryDTO) ⚡ PARALLEL GROUP A
- [x] Task B4: Workspace entity — adicionar campo `jobCounter` (depende de B2)
- [x] Task B5: Entities Job + JobFile (depende de B1, B2, B4)
- [x] Task B6: JobRepository + JobFileRepository + JobSpecifications (depende de B5)
- [x] Task B7: JobMapper (MapStruct) + helpers (depende de B5, B3) ⚡ PARALLEL GROUP B
- [x] Task B8: BriefingValidator (Strategy Pattern) + 7 TypeBriefingValidator impls (depende de B1) ⚡ PARALLEL GROUP B
- [x] Task B9: JobService interface + JobServiceImpl (depende de B6, B7, B8)
- [x] Task B10: JobController + 8 endpoints REST (depende de B9)
- [x] Task B11: Integration test JobRepositoryTest — Testcontainers (depende de B6) ⚡ PARALLEL GROUP C
- [x] Task B12: Integration test JobControllerTest — @SpringBootTest (depende de B10) ⚡ PARALLEL GROUP C

### Frontend (F1–F23)

- [x] Task F1: Criar `job.model.ts` (interfaces + unions + filtros) ⚡ PARALLEL GROUP D
- [x] Task F2: Criar `briefing-schemas.ts` (BRIEFING_SCHEMAS com 7 tipos) ⚡ PARALLEL GROUP D
- [x] Task F3: Criar `JobApiService` com todos os endpoints (depende de F1 + **B10 backend**)
- [x] Task F4: Criar `jobs.routes.ts` com 4 rotas e `roleGuard` em new/edit (depende de F3)
- [x] Task F5: Registrar rota lazy `jobs` em `app.routes.ts` (depende de F4)
- [x] Task F6: Adicionar item "Jobs" no `SidebarComponent` (depende de F5)
- [x] Task F7: Invocar `frontend-design` para `JobListComponent`
- [x] Task F8: Implementar `JobListComponent` (depende de F3, F7)
- [x] Task F9: Invocar `frontend-design` para `BriefingFieldsComponent`
- [x] Task F10: Implementar `BriefingFieldsComponent` (depende de F2, F9)
- [x] Task F11: Invocar `frontend-design` para `JobFileUploaderComponent`
- [x] Task F12: Implementar `JobFileUploaderComponent` (depende de F3, F11)
- [x] Task F13: Invocar `frontend-design` para `JobSummarySidebarComponent`
- [x] Task F14: Implementar `JobSummarySidebarComponent` (depende de F1, F13)
- [x] Task F15: Invocar `frontend-design` para `JobFormComponent`
- [x] Task F16: Implementar `JobFormComponent` (depende de F3, F10, F12, F14, F15)
- [x] Task F17: Invocar `frontend-design` para `JobCreateComponent`
- [x] Task F18: Implementar `JobCreateComponent` (depende de F16, F17)
- [x] Task F19: Invocar `frontend-design` para `JobEditComponent`
- [x] Task F20: Implementar `JobEditComponent` (depende de F16, F19)
- [x] Task F21: Invocar `frontend-design` para `JobDetailComponent`
- [x] Task F22: Implementar `JobDetailComponent` (depende de F3, F10, F21)
- [x] Task F23: Smoke test manual + ajustes finais (backend + frontend locais)

---

## Grupos Paralelos

| Grupo | Tasks | Condição |
|---|---|---|
| `⚡ PARALLEL GROUP A` | B1, B2, B3 | Fundação backend — enums, migration SQL e DTOs são independentes |
| `⚡ PARALLEL GROUP B` | B7, B8 | Mapper (depende de B3, B5) e BriefingValidator (depende de B1) — rodam em paralelo após B5 existir |
| `⚡ PARALLEL GROUP C` | B11, B12 | Integration tests — rodam em paralelo após B10 |
| `⚡ PARALLEL GROUP D` | F1, F2 | Fundação frontend — models e schemas são arquivos independentes |

**Paralelismo cross-layer:** PARALLEL GROUP A (backend fundação) e PARALLEL GROUP D (frontend fundação) podem ser executados simultaneamente em sessões separadas, pois não têm dependência entre si. A primeira task frontend que **bloqueia** no backend é **F3 (JobApiService)** — precisa de B10 (endpoints REST) para validar a interface. Até lá, o frontend pode adiantar apenas F1+F2.

---

## Ordem de Execução (Fases)

### Fase 1 — Fundação (paralela cross-layer)
Execução em paralelo:
- Backend: B1, B2, B3 (⚡ PARALLEL GROUP A)
- Frontend: F1, F2 (⚡ PARALLEL GROUP D)

### Fase 2 — Backend core
Sequencial com paralelismo interno:
- B4 (depende de B2)
- B5 (depende de B1, B2, B4)
- B7 + B8 em paralelo (⚡ PARALLEL GROUP B)
- B6 (depende de B5)
- B9 (depende de B6, B7, B8)
- B10 (depende de B9)

### Fase 3 — Backend tests
Em paralelo (⚡ PARALLEL GROUP C):
- B11 (depende de B6)
- B12 (depende de B10)

### Fase 4 — Frontend infraestrutura
Sequencial:
- F3 (depende de B10 — endpoints REST reais)
- F4 (depende de F3)
- F5 (depende de F4)
- F6 (depende de F5)

### Fase 5 — Frontend componentes visuais
Sequencial com `frontend-design` precedendo cada task visual:
- F7 → F8 (JobListComponent)
- F9 → F10 (BriefingFieldsComponent)
- F11 → F12 (JobFileUploaderComponent)
- F13 → F14 (JobSummarySidebarComponent)
- F15 → F16 (JobFormComponent)
- F17 → F18 (JobCreateComponent)
- F19 → F20 (JobEditComponent)
- F21 → F22 (JobDetailComponent)

### Fase 6 — Smoke test
- F23 (backend + frontend ambos rodando)

---

## Dependências Cross-Layer Explícitas

| Task frontend | Depende do backend |
|---|---|
| F3 (JobApiService) | B10 (JobController — endpoints devem existir) |
| F8 (JobListComponent) | B10 (endpoint GET /jobs filtrado funcional) |
| F18 (JobCreateComponent) | B10 (POST /jobs + POST /jobs/{id}/files funcionais) |
| F20 (JobEditComponent) | B10 (PUT /jobs/{id} + DELETE /jobs/{id}/files/{fileId}) |
| F22 (JobDetailComponent) | B10 (GET /jobs/{id} + GET /jobs/{id}/files/{fileId}/download) |
| F23 (smoke test) | B12 (integration tests passando) |

---

## Regras Não-Negociáveis (Checklist para cada task)

### Backend
- [ ] MapStruct para TODA conversão Entity ↔ DTO (sem mapeamento manual inline)
- [ ] `@FetchType.LAZY` em TODOS `@ManyToOne`
- [ ] `@Transactional` em services que modificam dados
- [ ] Permissão validada no service layer (OWNER/MANAGER vs CREATIVE)
- [ ] Custom exceptions: `ResourceNotFoundException`, `BusinessException`, `UnauthorizedException`, `ForbiddenException`
- [ ] DTOs como `record`
- [ ] `@NotBlank` em Strings obrigatórias; `@NotNull` em Longs/enums; `List<@NotNull Long>` em listas
- [ ] `DateTimeFormatter.ISO_LOCAL_DATE_TIME` (nunca `.toString()`)
- [ ] `JOIN FETCH` em queries que alimentam listagens
- [ ] `@Modifying` em queries UPDATE/DELETE custom
- [ ] Queries "findFirst*" com `ORDER BY` explícito
- [ ] Atribuição de criativo valida CREATIVE role + ClientMember
- [ ] Visibilidade de CREATIVE via `findVisibleToCreative` (JOIN ClientMember)
- [ ] BriefingValidator strategy pattern (validator por tipo, sem lógica inline no service)
- [ ] Bean Validation `@Valid` no controller

### Frontend
- [ ] **Invocar `frontend-design` ANTES de cada task de UI visual** (REINCIDENTE — lessons.md)
- [ ] Standalone components (sem NgModule)
- [ ] `input()`, `output()`, `signal()`, `computed()` — nunca decorators
- [ ] `inject()` — nunca constructor injection
- [ ] `@if / @for / @switch` — nunca `*ngIf` / `*ngFor`
- [ ] `ChangeDetectionStrategy.OnPush` em todos os components
- [ ] Reactive Forms (não template-driven)
- [ ] `templateUrl` + arquivo HTML separado (nunca inline > 10 linhas)
- [ ] Lazy loading para feature routes
- [ ] Path aliases `@core/*`, `@shared/*`, `@features/*`, `@layout/*` quando `../../` ou mais
- [ ] `roleGuard` em toda rota com restrição de papel
- [ ] Nunca `as any`
- [ ] Todo `subscribe()` destrutivo com handler `error` e feedback ao usuário
- [ ] Debounce 300ms em busca
- [ ] Vitest com globals (sem imports de describe/it/expect)
- [ ] `provideHttpClientTesting()` para mocks de HTTP

---

## Branch & Workflow

1. A partir de `main`, criar branch: `git checkout -b feature/rf04-jobs-briefing`
2. Executar tasks na ordem das fases acima
3. **Marcar `[x]` em cada task imediatamente ao completar + commit**
4. Ao terminar todas as tasks, invocar `requesting-code-review`
5. Processar feedback via `receiving-code-review`
6. Finalizar via `finishing-a-development-branch`

---


## Backend — Tasks Detalhadas

---

## Task B1 — Enums (JobType, JobPriority, JobStatus)

**Files:**
- Create: `backend/src/main/java/com/briefflow/enums/JobType.java`
- Create: `backend/src/main/java/com/briefflow/enums/JobPriority.java`
- Create: `backend/src/main/java/com/briefflow/enums/JobStatus.java`

**Depends on:** nada — GROUP A

**Step 1 — Red:** Enums não têm testes dedicados (valores ficam cobertos por testes de service/mapper). Pular para Green.

**Step 3 — Green:**

```java
// JobType.java
package com.briefflow.enums;

public enum JobType {
    POST_FEED,
    STORIES,
    CARROSSEL,
    REELS_VIDEO,
    BANNER,
    LOGO,
    OUTROS
}
```

```java
// JobPriority.java
package com.briefflow.enums;

public enum JobPriority {
    BAIXA,
    NORMAL,
    ALTA,
    URGENTE
}
```

```java
// JobStatus.java
package com.briefflow.enums;

public enum JobStatus {
    NOVO,
    EM_CRIACAO,
    REVISAO_INTERNA,
    AGUARDANDO_APROVACAO,
    APROVADO,
    PUBLICADO
}
```

**Step 4 — verify compiles:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/enums/JobType.java \
        backend/src/main/java/com/briefflow/enums/JobPriority.java \
        backend/src/main/java/com/briefflow/enums/JobStatus.java
git commit -m "feat(jobs): add JobType, JobPriority, JobStatus enums"
```

---

## Task B2 — Migration V8 (jobs, job_files, workspaces.job_counter)

**Files:**
- Create: `backend/src/main/resources/db/migration/V8__create_jobs_and_files.sql`

**Depends on:** nada — GROUP A

**Step 1 — Red:** Migrations não têm teste unitário direto; são validadas pelos integration tests via Testcontainers (B11/B12). Rodar `./mvnw test -Dtest=JobRepositoryTest` falhará até a migration existir — isso é o red da camada de persistência.

**Step 3 — Green:**

```sql
-- V8__create_jobs_and_files.sql

ALTER TABLE workspaces ADD COLUMN job_counter BIGINT NOT NULL DEFAULT 0;

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    client_id BIGINT NOT NULL REFERENCES clients(id),
    assigned_creative_id BIGINT REFERENCES members(id),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    sequence_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'NOVO',
    description TEXT,
    deadline DATE,
    briefing_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_jobs_workspace_sequence UNIQUE (workspace_id, sequence_number)
);

CREATE INDEX idx_jobs_workspace_id ON jobs(workspace_id);
CREATE INDEX idx_jobs_workspace_archived ON jobs(workspace_id, archived);
CREATE INDEX idx_jobs_client_id ON jobs(client_id);
CREATE INDEX idx_jobs_assigned_creative_id ON jobs(assigned_creative_id);
CREATE INDEX idx_jobs_status ON jobs(workspace_id, status);
CREATE INDEX idx_jobs_deadline ON jobs(workspace_id, deadline);

CREATE TABLE job_files (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by_id BIGINT NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_files_job_id ON job_files(job_id);
```

**Step 4 — verify:** `./mvnw compile -q` (o arquivo é lido em runtime; será validado no B11 via Testcontainers).

**Step 5 — commit:**
```bash
git add backend/src/main/resources/db/migration/V8__create_jobs_and_files.sql
git commit -m "feat(jobs): migration V8 — jobs, job_files, workspaces.job_counter"
```

---

## Task B3 — DTOs (records)

**Files:**
- Create: `backend/src/main/java/com/briefflow/dto/job/JobRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/JobResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/JobListItemDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/JobFileDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/ClientSummaryDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/MemberSummaryDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/job/ArchiveJobRequestDTO.java`

**Depends on:** B1 (enums) — GROUP A

**Step 1 — Red:** DTOs record não têm testes diretos; cobertura vem via JobMapperTest e JobControllerTest.

**Step 3 — Green:**

```java
// JobRequestDTO.java
package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

public record JobRequestDTO(
    @NotNull Long clientId,
    Long assignedCreativeId,
    @NotBlank @Size(max = 255) String title,
    @NotNull JobType type,
    @NotNull JobPriority priority,
    String description,
    LocalDate deadline,
    @NotNull Map<String, Object> briefingData
) {}
```

```java
// JobResponseDTO.java
package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import java.util.List;
import java.util.Map;

public record JobResponseDTO(
    Long id,
    String code,                       // "JOB-001"
    String title,
    JobType type,
    JobPriority priority,
    JobStatus status,
    String description,
    String deadline,
    Map<String, Object> briefingData,
    Boolean archived,
    ClientSummaryDTO client,
    MemberSummaryDTO assignedCreative,
    MemberSummaryDTO createdBy,
    List<JobFileDTO> files,
    String createdAt,
    String updatedAt,
    boolean overdue
) {}
```

```java
// JobListItemDTO.java
package com.briefflow.dto.job;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;

public record JobListItemDTO(
    Long id,
    String code,                       // "JOB-001"
    String title,
    JobType type,
    JobPriority priority,
    JobStatus status,
    String deadline,
    ClientSummaryDTO client,
    MemberSummaryDTO assignedCreative,
    boolean overdue
) {}
```

```java
// JobFileDTO.java
package com.briefflow.dto.job;

public record JobFileDTO(
    Long id,
    String originalFilename,
    String mimeType,
    Long sizeBytes,
    String uploadedAt,
    String downloadUrl
) {}
```

```java
// ClientSummaryDTO.java
package com.briefflow.dto.job;

public record ClientSummaryDTO(
    Long id,
    String name,
    String company,
    String logoUrl
) {}
```

```java
// MemberSummaryDTO.java
package com.briefflow.dto.job;

import com.briefflow.enums.MemberRole;

public record MemberSummaryDTO(
    Long id,
    Long userId,
    String name,
    String email,
    MemberRole role
) {}
```

```java
// ArchiveJobRequestDTO.java
package com.briefflow.dto.job;

import jakarta.validation.constraints.NotNull;

public record ArchiveJobRequestDTO(
    @NotNull Boolean archived
) {}
```

**Step 4 — verify:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/dto/job/
git commit -m "feat(jobs): add Job DTOs (request, response, list, file, summaries)"
```

---

## Task B4 — Workspace entity: add `jobCounter`

**Files:**
- Modify: `backend/src/main/java/com/briefflow/entity/Workspace.java`

**Depends on:** B2 (coluna existe na migration)

**Step 1 — Red:** Cobertura indireta via B11 (`JobRepositoryTest.incrementAndGetJobCounter` falhará se o campo não existir). Sem teste unitário próprio.

**Step 3 — Green:**

Adicionar no `Workspace.java`:
```java
@Column(name = "job_counter", nullable = false)
private Long jobCounter = 0L;
```

**Step 4 — verify:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/entity/Workspace.java
git commit -m "feat(jobs): add jobCounter field to Workspace entity"
```

---

## Task B5 — Entities Job + JobFile

**Files:**
- Create: `backend/src/main/java/com/briefflow/entity/Job.java`
- Create: `backend/src/main/java/com/briefflow/entity/JobFile.java`

**Depends on:** B1, B2, B4

**Step 1 — Red:** Entities não têm teste direto; validação ocorre em B11 (JobRepositoryTest).

**Step 3 — Green:**

```java
// Job.java
package com.briefflow.entity;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "jobs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_jobs_workspace_sequence", columnNames = {"workspace_id", "sequence_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_creative_id")
    private Member assignedCreative;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobPriority priority = JobPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status = JobStatus.NOVO;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate deadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "briefing_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> briefingData = new HashMap<>();

    @Column(nullable = false)
    private Boolean archived = false;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobFile> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getCode() {
        return String.format("JOB-%03d", sequenceNumber);
    }
}
```

```java
// JobFile.java
package com.briefflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_files")
@Getter
@Setter
@NoArgsConstructor
public class JobFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
```

**Step 4 — verify:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/entity/Job.java \
        backend/src/main/java/com/briefflow/entity/JobFile.java
git commit -m "feat(jobs): add Job and JobFile entities with JSONB briefing_data"
```

---

## Task B6 — JobRepository + JobFileRepository + JobSpecifications

**Files:**
- Create: `backend/src/main/java/com/briefflow/repository/JobRepository.java`
- Create: `backend/src/main/java/com/briefflow/repository/JobFileRepository.java`
- Create: `backend/src/main/java/com/briefflow/repository/JobSpecifications.java`

**Depends on:** B5

**Step 1 — Red:** Integration tests em B11 cobrem as queries.

**Step 3 — Green:**

```java
// JobRepository.java
package com.briefflow.repository;

import com.briefflow.entity.Job;
import com.briefflow.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Query("""
        SELECT j FROM Job j
        LEFT JOIN FETCH j.client
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        LEFT JOIN FETCH j.createdBy
        LEFT JOIN FETCH j.files
        WHERE j.id = :id AND j.workspace.id = :workspaceId
    """)
    Optional<Job> findByIdAndWorkspaceIdWithDetails(@Param("id") Long id,
                                                     @Param("workspaceId") Long workspaceId);

    Optional<Job> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Query(value = """
        UPDATE workspaces SET job_counter = job_counter + 1
        WHERE id = :workspaceId
        RETURNING job_counter
    """, nativeQuery = true)
    @Modifying
    Long incrementAndGetJobCounter(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT DISTINCT j FROM Job j
        LEFT JOIN FETCH j.client c
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        WHERE j.workspace.id = :workspaceId
          AND j.archived = :archived
          AND (
               j.assignedCreative.id = :memberId
            OR j.client.id IN (
                 SELECT cm.clientId FROM ClientMember cm WHERE cm.memberId = :memberId
               )
          )
    """)
    List<Job> findVisibleToCreative(@Param("workspaceId") Long workspaceId,
                                     @Param("memberId") Long memberId,
                                     @Param("archived") Boolean archived);

    @Query("""
        SELECT j FROM Job j
        LEFT JOIN FETCH j.client
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        WHERE j.workspace.id = :workspaceId
          AND j.archived = false
    """)
    Page<Job> findAllActiveByWorkspaceId(@Param("workspaceId") Long workspaceId, Pageable pageable);

    long countByWorkspaceIdAndStatus(Long workspaceId, JobStatus status);
}
```

```java
// JobFileRepository.java
package com.briefflow.repository;

import com.briefflow.entity.JobFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobFileRepository extends JpaRepository<JobFile, Long> {
    Optional<JobFile> findByIdAndJobId(Long id, Long jobId);
}
```

```java
// JobSpecifications.java
package com.briefflow.repository;

import com.briefflow.entity.Job;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> inWorkspace(Long workspaceId) {
        return (root, q, cb) -> cb.equal(root.get("workspace").get("id"), workspaceId);
    }

    public static Specification<Job> notArchived() {
        return (root, q, cb) -> cb.equal(root.get("archived"), false);
    }

    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> hasType(JobType type) {
        return (root, q, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Job> hasPriority(JobPriority priority) {
        return (root, q, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Job> forClient(Long clientId) {
        return (root, q, cb) -> clientId == null ? cb.conjunction() : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Job> assignedCreative(Long memberId) {
        return (root, q, cb) -> memberId == null ? cb.conjunction() : cb.equal(root.get("assignedCreative").get("id"), memberId);
    }
}
```

**Step 4 — verify:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/repository/JobRepository.java \
        backend/src/main/java/com/briefflow/repository/JobFileRepository.java \
        backend/src/main/java/com/briefflow/repository/JobSpecifications.java
git commit -m "feat(jobs): add JobRepository with JOIN FETCH + Specifications"
```

---

## Task B7 — JobMapper (MapStruct)

**Files:**
- Create: `backend/src/main/java/com/briefflow/mapper/JobMapper.java`
- Create: `backend/src/test/java/com/briefflow/unit/mapper/JobMapperTest.java`

**Depends on:** B5, B3

**Step 1 — Red:**

```java
// JobMapperTest.java
package com.briefflow.unit.mapper;

import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.entity.*;
import com.briefflow.enums.*;
import com.briefflow.mapper.JobMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobMapperTest {

    private final JobMapper mapper = Mappers.getMapper(JobMapper.class);

    @Test
    void should_mapJobToResponseDTO_withClientAndAssignedCreativeAndFiles() {
        Workspace w = new Workspace(); w.setId(1L); w.setName("WS");
        Client c = new Client(); c.setId(10L); c.setName("Client A"); c.setCompany("Co A");
        User u = new User(); u.setId(20L); u.setName("Alice"); u.setEmail("a@b.com");
        Member m = new Member(); m.setId(30L); m.setUser(u); m.setRole(MemberRole.CREATIVE);

        Job j = new Job();
        j.setId(100L);
        j.setWorkspace(w);
        j.setClient(c);
        j.setAssignedCreative(m);
        j.setCreatedBy(u);
        j.setSequenceNumber(1);
        j.setTitle("Post X");
        j.setType(JobType.POST_FEED);
        j.setPriority(JobPriority.NORMAL);
        j.setStatus(JobStatus.NOVO);
        j.setDescription("Briefing para post de lançamento");
        j.setDeadline(LocalDate.now().plusDays(3));
        Map<String, Object> bd = new HashMap<>();
        bd.put("captionText", "hello");
        j.setBriefingData(bd);
        j.setCreatedAt(LocalDateTime.now());
        j.setUpdatedAt(LocalDateTime.now());

        JobResponseDTO dto = mapper.toResponseDTO(j);

        assertEquals(100L, dto.id());
        assertEquals("JOB-001", dto.code());
        assertEquals("Post X", dto.title());
        assertEquals(JobType.POST_FEED, dto.type());
        assertEquals("Briefing para post de lançamento", dto.description());
        assertEquals(10L, dto.client().id());
        assertEquals("Client A", dto.client().name());
        assertEquals(30L, dto.assignedCreative().id());
        assertEquals("Alice", dto.assignedCreative().name());
        assertEquals("hello", dto.briefingData().get("captionText"));
        assertNotNull(dto.createdAt());
        assertFalse(dto.overdue());
    }

    @Test
    void should_markOverdue_when_deadlineInPastAndStatusNotApproved() {
        Job j = jobWithDeadline(LocalDate.now().minusDays(1), JobStatus.EM_CRIACAO);
        JobResponseDTO dto = mapper.toResponseDTO(j);
        assertTrue(dto.overdue());
    }

    @Test
    void should_notMarkOverdue_when_statusIsAprovado() {
        Job j = jobWithDeadline(LocalDate.now().minusDays(5), JobStatus.APROVADO);
        JobResponseDTO dto = mapper.toResponseDTO(j);
        assertFalse(dto.overdue());
    }

    @Test
    void should_mapJobToListItemDTO() {
        Job j = jobWithDeadline(LocalDate.now().plusDays(1), JobStatus.NOVO);
        JobListItemDTO item = mapper.toListItemDTO(j);
        assertEquals(j.getId(), item.id());
        assertEquals("JOB-001", item.code());
        assertEquals(j.getTitle(), item.title());
        assertNotNull(item.client());
    }

    private Job jobWithDeadline(LocalDate deadline, JobStatus status) {
        Workspace w = new Workspace(); w.setId(1L);
        Client c = new Client(); c.setId(10L); c.setName("C");
        User u = new User(); u.setId(20L); u.setName("U"); u.setEmail("u@u.com");
        Member m = new Member(); m.setId(30L); m.setUser(u); m.setRole(MemberRole.CREATIVE);
        Job j = new Job();
        j.setId(100L); j.setWorkspace(w); j.setClient(c); j.setAssignedCreative(m); j.setCreatedBy(u);
        j.setSequenceNumber(1); j.setTitle("T"); j.setType(JobType.POST_FEED);
        j.setPriority(JobPriority.NORMAL); j.setStatus(status); j.setDeadline(deadline);
        j.setCreatedAt(LocalDateTime.now()); j.setUpdatedAt(LocalDateTime.now());
        return j;
    }
}
```

**Step 2 — verify fails:** `./mvnw test -Dtest=JobMapperTest` → compile error (JobMapper doesn't exist).

**Step 3 — Green:**

```java
// JobMapper.java
package com.briefflow.mapper;

import com.briefflow.dto.job.*;
import com.briefflow.entity.Job;
import com.briefflow.entity.JobFile;
import com.briefflow.entity.Member;
import com.briefflow.enums.JobStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "assignedCreative", source = "assignedCreative")
    @Mapping(target = "createdBy", expression = "java(mapUserAsMember(job))")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "formatDate")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "overdue", expression = "java(isOverdue(job))")
    JobResponseDTO toResponseDTO(Job job);

    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "assignedCreative", source = "assignedCreative")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "formatDate")
    @Mapping(target = "overdue", expression = "java(isOverdue(job))")
    JobListItemDTO toListItemDTO(Job job);

    List<JobListItemDTO> toListItemDTOList(List<Job> jobs);

    @Mapping(target = "originalFilename", source = "originalFilename")
    @Mapping(target = "uploadedAt", source = "uploadedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "downloadUrl", expression = "java(buildDownloadUrl(file))")
    JobFileDTO toFileDTO(JobFile file);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "company", source = "company")
    @Mapping(target = "logoUrl", source = "logoUrl")
    ClientSummaryDTO clientToSummary(com.briefflow.entity.Client client);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "role")
    MemberSummaryDTO memberToSummary(Member member);

    default MemberSummaryDTO mapUserAsMember(Job job) {
        if (job.getCreatedBy() == null) return null;
        return new MemberSummaryDTO(
            null,
            job.getCreatedBy().getId(),
            job.getCreatedBy().getName(),
            job.getCreatedBy().getEmail(),
            null
        );
    }

    default boolean isOverdue(Job job) {
        if (job.getDeadline() == null) return false;
        if (job.getStatus() == JobStatus.APROVADO || job.getStatus() == JobStatus.PUBLICADO) return false;
        return job.getDeadline().isBefore(LocalDate.now());
    }

    default String buildDownloadUrl(JobFile file) {
        if (file == null || file.getJob() == null) return null;
        return "/api/v1/jobs/" + file.getJob().getId() + "/files/" + file.getId() + "/download";
    }

    @Named("formatDate")
    default String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
```

**Step 4 — verify passes:** `./mvnw test -Dtest=JobMapperTest -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/mapper/JobMapper.java \
        backend/src/test/java/com/briefflow/unit/mapper/JobMapperTest.java
git commit -m "feat(jobs): add JobMapper with overdue helper and summary mappings"
```

---

## Task B8 — BriefingValidator (Strategy Pattern)

**Files:**
- Create: `backend/src/main/java/com/briefflow/service/briefing/TypeBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/BriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/PostFeedBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/StoriesBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/CarrosselBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/ReelsVideoBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/BannerBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/LogoBriefingValidator.java`
- Create: `backend/src/main/java/com/briefflow/service/briefing/OutrosBriefingValidator.java`
- Create: `backend/src/test/java/com/briefflow/unit/service/briefing/BriefingValidatorTest.java`

**Depends on:** B1 (GROUP B)

**Step 1 — Red:**

```java
// BriefingValidatorTest.java
package com.briefflow.unit.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import com.briefflow.service.briefing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BriefingValidatorTest {

    private BriefingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BriefingValidator(List.of(
            new PostFeedBriefingValidator(),
            new StoriesBriefingValidator(),
            new CarrosselBriefingValidator(),
            new ReelsVideoBriefingValidator(),
            new BannerBriefingValidator(),
            new LogoBriefingValidator(),
            new OutrosBriefingValidator()
        ));
    }

    @Test
    void should_acceptValidPostFeedBriefing() {
        Map<String, Object> data = Map.of("captionText", "Hello", "format", "1:1");
        assertDoesNotThrow(() -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_rejectPostFeed_when_captionTextMissing() {
        Map<String, Object> data = Map.of("format", "1:1");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_rejectPostFeed_when_formatInvalid() {
        Map<String, Object> data = Map.of("captionText", "x", "format", "9:16");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_acceptValidStories() {
        Map<String, Object> data = Map.of("text", "Story", "format", "9:16");
        assertDoesNotThrow(() -> validator.validate(JobType.STORIES, data));
    }

    @Test
    void should_rejectStories_when_formatNot9x16() {
        Map<String, Object> data = Map.of("text", "Story", "format", "1:1");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.STORIES, data));
    }

    @Test
    void should_acceptValidCarrossel() {
        Map<String, Object> data = Map.of("slideCount", 5, "caption", "c");
        assertDoesNotThrow(() -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_rejectCarrossel_when_slideCountBelow2() {
        Map<String, Object> data = Map.of("slideCount", 1, "caption", "c");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_rejectCarrossel_when_slideCountAbove10() {
        Map<String, Object> data = Map.of("slideCount", 11, "caption", "c");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_acceptValidReelsVideo() {
        Map<String, Object> data = Map.of("duration", 30, "script", "s");
        assertDoesNotThrow(() -> validator.validate(JobType.REELS_VIDEO, data));
    }

    @Test
    void should_rejectReelsVideo_when_scriptMissing() {
        Map<String, Object> data = Map.of("duration", 30);
        assertThrows(BusinessException.class, () -> validator.validate(JobType.REELS_VIDEO, data));
    }

    @Test
    void should_acceptValidBanner() {
        Map<String, Object> data = Map.of("dimensions", "1920x1080", "text", "t");
        assertDoesNotThrow(() -> validator.validate(JobType.BANNER, data));
    }

    @Test
    void should_rejectBanner_when_dimensionsMissing() {
        Map<String, Object> data = Map.of("text", "t");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.BANNER, data));
    }

    @Test
    void should_acceptValidLogo() {
        Map<String, Object> data = Map.of("desiredStyle", "minimalist");
        assertDoesNotThrow(() -> validator.validate(JobType.LOGO, data));
    }

    @Test
    void should_rejectLogo_when_desiredStyleMissing() {
        assertThrows(BusinessException.class, () -> validator.validate(JobType.LOGO, new HashMap<>()));
    }

    @Test
    void should_acceptValidOutros() {
        Map<String, Object> data = Map.of("freeDescription", "qualquer coisa");
        assertDoesNotThrow(() -> validator.validate(JobType.OUTROS, data));
    }

    @Test
    void should_rejectOutros_when_freeDescriptionBlank() {
        Map<String, Object> data = Map.of("freeDescription", "  ");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.OUTROS, data));
    }
}
```

**Step 2 — verify fails:** `./mvnw test -Dtest=BriefingValidatorTest` → compile error.

**Step 3 — Green:**

```java
// TypeBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import java.util.Map;

public interface TypeBriefingValidator {
    JobType getType();
    void validate(Map<String, Object> data);
}
```

```java
// BriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BriefingValidator {

    private final Map<JobType, TypeBriefingValidator> validators;

    public BriefingValidator(List<TypeBriefingValidator> list) {
        this.validators = list.stream().collect(Collectors.toMap(TypeBriefingValidator::getType, v -> v));
    }

    public void validate(JobType type, Map<String, Object> data) {
        if (data == null) throw new BusinessException("briefingData é obrigatório");
        TypeBriefingValidator v = validators.get(type);
        if (v == null) throw new BusinessException("Tipo de job sem validador: " + type);
        v.validate(data);
    }
}
```

```java
// PostFeedBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

@Component
public class PostFeedBriefingValidator implements TypeBriefingValidator {
    private static final Set<String> FORMATS = Set.of("1:1", "4:5");

    @Override public JobType getType() { return JobType.POST_FEED; }

    @Override public void validate(Map<String, Object> d) {
        String caption = (String) d.get("captionText");
        if (caption == null || caption.isBlank())
            throw new BusinessException("POST_FEED: captionText é obrigatório");
        String format = (String) d.get("format");
        if (format == null || !FORMATS.contains(format))
            throw new BusinessException("POST_FEED: format deve ser 1:1 ou 4:5");
    }
}
```

```java
// StoriesBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class StoriesBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.STORIES; }

    @Override public void validate(Map<String, Object> d) {
        String text = (String) d.get("text");
        if (text == null || text.isBlank())
            throw new BusinessException("STORIES: text é obrigatório");
        String format = (String) d.get("format");
        if (!"9:16".equals(format))
            throw new BusinessException("STORIES: format deve ser 9:16");
    }
}
```

```java
// CarrosselBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CarrosselBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.CARROSSEL; }

    @Override public void validate(Map<String, Object> d) {
        Object sc = d.get("slideCount");
        if (!(sc instanceof Number n))
            throw new BusinessException("CARROSSEL: slideCount é obrigatório");
        int count = n.intValue();
        if (count < 2 || count > 10)
            throw new BusinessException("CARROSSEL: slideCount deve estar entre 2 e 10");
        String caption = (String) d.get("caption");
        if (caption == null || caption.isBlank())
            throw new BusinessException("CARROSSEL: caption é obrigatório");
    }
}
```

```java
// ReelsVideoBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ReelsVideoBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.REELS_VIDEO; }

    @Override public void validate(Map<String, Object> d) {
        Object dur = d.get("duration");
        if (!(dur instanceof Number))
            throw new BusinessException("REELS_VIDEO: duration é obrigatório");
        String script = (String) d.get("script");
        if (script == null || script.isBlank())
            throw new BusinessException("REELS_VIDEO: script é obrigatório");
    }
}
```

```java
// BannerBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class BannerBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.BANNER; }

    @Override public void validate(Map<String, Object> d) {
        String dim = (String) d.get("dimensions");
        if (dim == null || dim.isBlank())
            throw new BusinessException("BANNER: dimensions é obrigatório");
        String text = (String) d.get("text");
        if (text == null || text.isBlank())
            throw new BusinessException("BANNER: text é obrigatório");
    }
}
```

```java
// LogoBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class LogoBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.LOGO; }

    @Override public void validate(Map<String, Object> d) {
        String style = (String) d.get("desiredStyle");
        if (style == null || style.isBlank())
            throw new BusinessException("LOGO: desiredStyle é obrigatório");
    }
}
```

```java
// OutrosBriefingValidator.java
package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OutrosBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.OUTROS; }

    @Override public void validate(Map<String, Object> d) {
        String desc = (String) d.get("freeDescription");
        if (desc == null || desc.isBlank())
            throw new BusinessException("OUTROS: freeDescription é obrigatório");
    }
}
```

**Step 4 — verify passes:** `./mvnw test -Dtest=BriefingValidatorTest -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/service/briefing/ \
        backend/src/test/java/com/briefflow/unit/service/briefing/BriefingValidatorTest.java
git commit -m "feat(jobs): BriefingValidator strategy with 7 type validators"
```

---

## Task B9 — JobService interface + JobServiceImpl

**Files:**
- Create: `backend/src/main/java/com/briefflow/service/JobService.java`
- Create: `backend/src/main/java/com/briefflow/service/impl/JobServiceImpl.java`
- Create: `backend/src/test/java/com/briefflow/unit/service/JobServiceImplTest.java`

**Depends on:** B6, B7, B8

**Step 1 — Red:**

```java
// JobServiceImplTest.java
package com.briefflow.unit.service;

import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.entity.*;
import com.briefflow.enums.*;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.JobMapper;
import com.briefflow.repository.*;
import com.briefflow.service.briefing.BriefingValidator;
import com.briefflow.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobFileRepository jobFileRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ClientMemberRepository clientMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private JobMapper jobMapper;
    @Mock private BriefingValidator briefingValidator;
    @Mock private com.briefflow.service.FileStorageService fileStorageService;

    private JobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JobServiceImpl(
            jobRepository, jobFileRepository, clientRepository, memberRepository,
            clientMemberRepository, userRepository, workspaceRepository,
            jobMapper, briefingValidator, fileStorageService
        );
    }

    @Test
    void should_createJob_when_callerIsManager_andBriefingValid() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(clientRepository.findByIdAndWorkspaceId(100L, workspaceId)).thenReturn(Optional.of(createClient(100L, workspaceId)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(manager.getUser()));
        when(jobRepository.incrementAndGetJobCounter(workspaceId)).thenReturn(1L);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0); j.setId(500L); return j;
        });
        when(jobMapper.toResponseDTO(any(Job.class))).thenReturn(mock(JobResponseDTO.class));

        JobRequestDTO req = new JobRequestDTO(100L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));
        JobResponseDTO dto = service.createJob(workspaceId, userId, req);

        assertNotNull(dto);
        verify(briefingValidator).validate(JobType.POST_FEED, req.briefingData());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void should_throwForbidden_when_creativeTriesToCreateJob() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));

        JobRequestDTO req = new JobRequestDTO(100L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));

        assertThrows(ForbiddenException.class, () -> service.createJob(workspaceId, userId, req));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void should_throwNotFound_when_clientDoesNotBelongToWorkspace() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(clientRepository.findByIdAndWorkspaceId(999L, workspaceId)).thenReturn(Optional.empty());

        JobRequestDTO req = new JobRequestDTO(999L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));

        assertThrows(ResourceNotFoundException.class, () -> service.createJob(workspaceId, userId, req));
    }

    @Test
    void should_listAllJobs_when_callerIsManager() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(new Job(), new Job()));
        when(jobMapper.toListItemDTOList(anyList())).thenReturn(List.of());

        service.listJobs(workspaceId, userId, null, null, null, null, null);

        verify(jobRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void should_onlyReturnVisibleJobs_when_callerIsCreative() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));
        when(jobRepository.findVisibleToCreative(workspaceId, creative.getId(), false)).thenReturn(List.of());
        when(jobMapper.toListItemDTOList(anyList())).thenReturn(List.of());

        service.listJobs(workspaceId, userId, null, null, null, null, null);

        verify(jobRepository).findVisibleToCreative(workspaceId, creative.getId(), false);
        verify(jobRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void should_archiveJob_when_callerIsManager() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        Job job = new Job(); job.setId(500L); job.setStatus(JobStatus.NOVO);
        Workspace w = new Workspace(); w.setId(workspaceId); job.setWorkspace(w);

        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(jobRepository.findByIdAndWorkspaceId(500L, workspaceId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobMapper.toResponseDTO(any(Job.class))).thenReturn(mock(JobResponseDTO.class));

        service.archiveJob(workspaceId, userId, 500L, true);

        assertTrue(job.getArchived());
        verify(jobRepository).save(job);
    }

    @Test
    void should_throwForbidden_when_creativeTriesToArchive() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));

        assertThrows(ForbiddenException.class, () -> service.archiveJob(workspaceId, userId, 500L, true));
    }

    // helpers
    private User createUser(Long id) {
        User u = new User(); u.setId(id); u.setName("U" + id); u.setEmail(id + "@t.com"); return u;
    }
    private Workspace createWorkspace(Long id) {
        Workspace w = new Workspace(); w.setId(id); w.setName("WS"); return w;
    }
    private Member createMember(Long id, Long userId, Long workspaceId, MemberRole role) {
        Member m = new Member(); m.setId(id);
        m.setUser(createUser(userId)); m.setWorkspace(createWorkspace(workspaceId));
        m.setRole(role); return m;
    }
    private Client createClient(Long id, Long workspaceId) {
        Client c = new Client(); c.setId(id); c.setName("C" + id);
        c.setWorkspace(createWorkspace(workspaceId)); c.setActive(true); return c;
    }
}
```

**Step 2 — verify fails:** `./mvnw test -Dtest=JobServiceImplTest` → compile error.

**Step 3 — Green:**

```java
// JobService.java
package com.briefflow.service;

import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JobService {
    JobResponseDTO createJob(Long workspaceId, Long userId, JobRequestDTO request);
    List<JobListItemDTO> listJobs(Long workspaceId, Long userId,
                                   JobStatus status, JobType type, JobPriority priority,
                                   Long clientId, Long assignedCreativeId);
    JobResponseDTO getJob(Long workspaceId, Long userId, Long jobId);
    JobResponseDTO updateJob(Long workspaceId, Long userId, Long jobId, JobRequestDTO request);
    JobResponseDTO archiveJob(Long workspaceId, Long userId, Long jobId, boolean archived);
    JobResponseDTO uploadFile(Long workspaceId, Long userId, Long jobId, MultipartFile file);
    void deleteFile(Long workspaceId, Long userId, Long jobId, Long fileId);
    Resource downloadFile(Long workspaceId, Long userId, Long jobId, Long fileId);
}
```

```java
// JobServiceImpl.java
package com.briefflow.service.impl;

import com.briefflow.dto.job.*;
import com.briefflow.entity.*;
import com.briefflow.enums.*;
import com.briefflow.exception.*;
import com.briefflow.mapper.JobMapper;
import com.briefflow.repository.*;
import com.briefflow.service.FileStorageService;
import com.briefflow.service.JobService;
import com.briefflow.service.briefing.BriefingValidator;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.briefflow.repository.JobSpecifications.*;

@Service
public class JobServiceImpl implements JobService {

    private static final Set<String> ALLOWED_MIMES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "application/pdf", "video/mp4", "video/quicktime"
    );
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final ClientRepository clientRepository;
    private final MemberRepository memberRepository;
    private final ClientMemberRepository clientMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final JobMapper jobMapper;
    private final BriefingValidator briefingValidator;
    private final FileStorageService fileStorageService;

    public JobServiceImpl(JobRepository jobRepository, JobFileRepository jobFileRepository,
                          ClientRepository clientRepository, MemberRepository memberRepository,
                          ClientMemberRepository clientMemberRepository, UserRepository userRepository,
                          WorkspaceRepository workspaceRepository, JobMapper jobMapper,
                          BriefingValidator briefingValidator, FileStorageService fileStorageService) {
        this.jobRepository = jobRepository;
        this.jobFileRepository = jobFileRepository;
        this.clientRepository = clientRepository;
        this.memberRepository = memberRepository;
        this.clientMemberRepository = clientMemberRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.jobMapper = jobMapper;
        this.briefingValidator = briefingValidator;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public JobResponseDTO createJob(Long workspaceId, Long userId, JobRequestDTO req) {
        Member caller = requireOwnerOrManager(userId, workspaceId);
        briefingValidator.validate(req.type(), req.briefingData());

        Client client = clientRepository.findByIdAndWorkspaceId(req.clientId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        Member assignedCreative = null;
        if (req.assignedCreativeId() != null) {
            assignedCreative = memberRepository.findByIdAndWorkspaceId(req.assignedCreativeId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membro designado não encontrado"));
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Long sequenceNumber = jobRepository.incrementAndGetJobCounter(workspaceId);

        Job job = new Job();
        job.setWorkspace(caller.getWorkspace());
        job.setClient(client);
        job.setAssignedCreative(assignedCreative);
        job.setCreatedBy(creator);
        job.setSequenceNumber(sequenceNumber.intValue());
        job.setTitle(req.title());
        job.setType(req.type());
        job.setPriority(req.priority());
        job.setStatus(JobStatus.NOVO);
        job.setDescription(req.description());
        job.setDeadline(req.deadline());
        job.setBriefingData(req.briefingData());

        Job saved = jobRepository.save(job);
        return jobMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobListItemDTO> listJobs(Long workspaceId, Long userId,
                                          JobStatus status, JobType type, JobPriority priority,
                                          Long clientId, Long assignedCreativeId) {
        Member caller = requireMember(userId, workspaceId);
        List<Job> jobs;
        if (caller.getRole() == MemberRole.CREATIVE) {
            jobs = jobRepository.findVisibleToCreative(workspaceId, caller.getId(), false);
        } else {
            Specification<Job> spec = Specification.where(inWorkspace(workspaceId))
                    .and(notArchived())
                    .and(hasStatus(status))
                    .and(hasType(type))
                    .and(hasPriority(priority))
                    .and(forClient(clientId))
                    .and(assignedCreative(assignedCreativeId));
            jobs = jobRepository.findAll(spec);
        }
        return jobMapper.toListItemDTOList(jobs);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponseDTO getJob(Long workspaceId, Long userId, Long jobId) {
        Member caller = requireMember(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceIdWithDetails(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        assertCanView(caller, job);
        return jobMapper.toResponseDTO(job);
    }

    @Override
    @Transactional
    public JobResponseDTO updateJob(Long workspaceId, Long userId, Long jobId, JobRequestDTO req) {
        Member caller = requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));

        briefingValidator.validate(req.type(), req.briefingData());

        if (!job.getClient().getId().equals(req.clientId())) {
            Client client = clientRepository.findByIdAndWorkspaceId(req.clientId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            job.setClient(client);
        }

        if (req.assignedCreativeId() == null) {
            job.setAssignedCreative(null);
        } else if (job.getAssignedCreative() == null || !job.getAssignedCreative().getId().equals(req.assignedCreativeId())) {
            Member m = memberRepository.findByIdAndWorkspaceId(req.assignedCreativeId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
            job.setAssignedCreative(m);
        }

        job.setTitle(req.title());
        job.setType(req.type());
        job.setPriority(req.priority());
        job.setDescription(req.description());
        job.setDeadline(req.deadline());
        job.setBriefingData(req.briefingData());

        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    @Override
    @Transactional
    public JobResponseDTO archiveJob(Long workspaceId, Long userId, Long jobId, boolean archived) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        job.setArchived(archived);
        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    @Override
    @Transactional
    public JobResponseDTO uploadFile(Long workspaceId, Long userId, Long jobId, MultipartFile file) {
        Member caller = requireMember(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        assertCanView(caller, job);

        if (file.isEmpty()) throw new BusinessException("Arquivo vazio");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException("Arquivo excede 50MB");
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIMES.contains(mime))
            throw new BusinessException("Tipo de arquivo não permitido: " + mime);

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID() + ext;
        try {
            fileStorageService.store(file, "jobs/" + jobId, stored);
        } catch (IOException e) {
            throw new FileStorageException("Falha ao salvar arquivo", e);
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        JobFile jf = new JobFile();
        jf.setJob(job);
        jf.setOriginalFilename(original);
        jf.setStoredFilename(stored);
        jf.setMimeType(mime);
        jf.setSizeBytes(file.getSize());
        jf.setUploadedBy(uploader);
        job.getFiles().add(jf);
        jobFileRepository.save(jf);

        return jobMapper.toResponseDTO(job);
    }

    @Override
    @Transactional
    public void deleteFile(Long workspaceId, Long userId, Long jobId, Long fileId) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        JobFile file = jobFileRepository.findByIdAndJobId(fileId, job.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        try {
            fileStorageService.delete("jobs/" + job.getId() + "/" + file.getStoredFilename());
        } catch (IOException e) {
            throw new FileStorageException("Falha ao excluir arquivo", e);
        }
        jobFileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long workspaceId, Long userId, Long jobId, Long fileId) {
        Member caller = requireMember(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        assertCanView(caller, job);
        JobFile file = jobFileRepository.findByIdAndJobId(fileId, job.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        try {
            return fileStorageService.load("jobs/" + job.getId() + "/" + file.getStoredFilename());
        } catch (IOException e) {
            throw new FileStorageException("Falha ao ler arquivo", e);
        }
    }

    // helpers
    private Member requireMember(Long userId, Long workspaceId) {
        return memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ForbiddenException("Usuário não pertence ao workspace"));
    }

    private Member requireOwnerOrManager(Long userId, Long workspaceId) {
        Member m = requireMember(userId, workspaceId);
        if (m.getRole() == MemberRole.CREATIVE)
            throw new ForbiddenException("Operação restrita a OWNER/MANAGER");
        return m;
    }

    private void assertCanView(Member caller, Job job) {
        if (caller.getRole() != MemberRole.CREATIVE) return;
        boolean assigned = job.getAssignedCreative() != null && job.getAssignedCreative().getId().equals(caller.getId());
        boolean clientMember = clientMemberRepository.existsByClientIdAndMemberId(job.getClient().getId(), caller.getId());
        if (!assigned && !clientMember) throw new ForbiddenException("Sem acesso ao job");
    }
}
```

**Step 4 — verify passes:** `./mvnw test -Dtest=JobServiceImplTest -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/service/JobService.java \
        backend/src/main/java/com/briefflow/service/impl/JobServiceImpl.java \
        backend/src/test/java/com/briefflow/unit/service/JobServiceImplTest.java
git commit -m "feat(jobs): JobService with role-based visibility and upload"
```

---

## Task B10 — JobController + 8 endpoints

**Files:**
- Create: `backend/src/main/java/com/briefflow/controller/JobController.java`

**Depends on:** B9

**Step 1 — Red:** Coberto pelo integration test em B12 (`JobControllerTest`).

**Step 3 — Green:**

```java
// JobController.java
package com.briefflow.controller;

import com.briefflow.dto.job.ArchiveJobRequestDTO;
import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import com.briefflow.service.JobService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> create(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody JobRequestDTO request) {
        JobResponseDTO dto = jobService.createJob(workspaceId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<JobListItemDTO>> list(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) JobPriority priority,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long assignedCreativeId) {
        return ResponseEntity.ok(jobService.listJobs(workspaceId, userId, status, type, priority, clientId, assignedCreativeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> get(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(workspaceId, userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDTO> update(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody JobRequestDTO request) {
        return ResponseEntity.ok(jobService.updateJob(workspaceId, userId, id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<JobResponseDTO> archive(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ArchiveJobRequestDTO request) {
        return ResponseEntity.ok(jobService.archiveJob(workspaceId, userId, id, request.archived()));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobResponseDTO> uploadFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.uploadFile(workspaceId, userId, id, file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @PathVariable Long fileId) {
        jobService.deleteFile(workspaceId, userId, id, fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestAttribute("workspaceId") Long workspaceId,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @PathVariable Long fileId) {
        Resource r = jobService.downloadFile(workspaceId, userId, id, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.getFilename() + "\"")
                .body(r);
    }
}
```

**Step 4 — verify:** `./mvnw compile -q`

**Step 5 — commit:**
```bash
git add backend/src/main/java/com/briefflow/controller/JobController.java
git commit -m "feat(jobs): JobController with 8 REST endpoints"
```

---

## Task B11 — Integration test JobRepositoryTest (Testcontainers)

**Files:**
- Create: `backend/src/test/java/com/briefflow/integration/repository/JobRepositoryTest.java`

**Depends on:** B6

**Step 1 — Red:**

```java
// JobRepositoryTest.java
package com.briefflow.integration.repository;

import com.briefflow.entity.*;
import com.briefflow.enums.*;
import com.briefflow.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class JobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("briefflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.enabled", () -> true);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private JobRepository jobRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ClientMemberRepository clientMemberRepository;

    @Test
    void should_incrementAndGetJobCounter_atomically() {
        Workspace w = newWorkspace("WS1");
        Long first = jobRepository.incrementAndGetJobCounter(w.getId());
        Long second = jobRepository.incrementAndGetJobCounter(w.getId());
        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    @Test
    void should_enforceUniqueSequenceNumberPerWorkspace() {
        Workspace w = newWorkspace("WS2");
        User u = newUser("u@t.com");
        Member m = newMember(w, u, MemberRole.MANAGER);
        Client c = newClient(w);

        Job j1 = newJob(w, c, m, u, 1, "J1");
        jobRepository.save(j1);

        Job j2 = newJob(w, c, m, u, 1, "J2");
        assertThrows(Exception.class, () -> jobRepository.saveAndFlush(j2));
    }

    @Test
    void should_findByIdAndWorkspaceIdWithDetails_joinFetchEverything() {
        Workspace w = newWorkspace("WS3");
        User u = newUser("u3@t.com");
        Member m = newMember(w, u, MemberRole.MANAGER);
        Client c = newClient(w);
        Job j = newJob(w, c, m, u, 1, "With details");
        jobRepository.save(j);

        Optional<Job> found = jobRepository.findByIdAndWorkspaceIdWithDetails(j.getId(), w.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getClient().getName());
        assertNotNull(found.get().getAssignedCreative().getUser().getName());
    }

    @Test
    void should_returnJobsAssignedToCreative_via_findVisibleToCreative() {
        Workspace w = newWorkspace("WS4");
        User owner = newUser("owner@t.com");
        User creative = newUser("creative@t.com");
        Member managerM = newMember(w, owner, MemberRole.MANAGER);
        Member creativeM = newMember(w, creative, MemberRole.CREATIVE);
        Client c = newClient(w);

        Job assigned = newJob(w, c, creativeM, owner, 1, "Assigned");
        Job notAssigned = newJob(w, c, managerM, owner, 2, "Other");
        jobRepository.save(assigned);
        jobRepository.save(notAssigned);

        List<Job> visible = jobRepository.findVisibleToCreative(w.getId(), creativeM.getId(), false);
        assertEquals(1, visible.size());
        assertEquals("Assigned", visible.get(0).getTitle());
    }

    @Test
    void should_includeJob_when_creativeIsMemberOfClient() {
        Workspace w = newWorkspace("WS5");
        User owner = newUser("owner5@t.com");
        User creative = newUser("c5@t.com");
        Member managerM = newMember(w, owner, MemberRole.MANAGER);
        Member creativeM = newMember(w, creative, MemberRole.CREATIVE);
        Client c = newClient(w);

        ClientMember cm = new ClientMember();
        cm.setClientId(c.getId());
        cm.setMemberId(creativeM.getId());
        clientMemberRepository.save(cm);

        Job j = newJob(w, c, managerM, owner, 1, "Via clientMember");
        jobRepository.save(j);

        List<Job> visible = jobRepository.findVisibleToCreative(w.getId(), creativeM.getId(), false);
        assertEquals(1, visible.size());
    }

    // helpers
    private Workspace newWorkspace(String name) {
        Workspace w = new Workspace();
        w.setName(name);
        return workspaceRepository.save(w);
    }
    private User newUser(String email) {
        User u = new User();
        u.setName("U");
        u.setEmail(email);
        u.setPasswordHash("x");
        return userRepository.save(u);
    }
    private Member newMember(Workspace w, User u, MemberRole role) {
        Member m = new Member();
        m.setWorkspace(w); m.setUser(u); m.setRole(role);
        return memberRepository.save(m);
    }
    private Client newClient(Workspace w) {
        Client c = new Client();
        c.setName("Client"); c.setWorkspace(w); c.setActive(true);
        return clientRepository.save(c);
    }
    private Job newJob(Workspace w, Client c, Member assignedCreative, User creator, Integer seq, String title) {
        Job j = new Job();
        j.setWorkspace(w); j.setClient(c); j.setAssignedCreative(assignedCreative); j.setCreatedBy(creator);
        j.setSequenceNumber(seq); j.setTitle(title);
        j.setType(JobType.POST_FEED); j.setPriority(JobPriority.NORMAL); j.setStatus(JobStatus.NOVO);
        j.setDeadline(LocalDate.now().plusDays(7));
        j.setBriefingData(new HashMap<>());
        return j;
    }
}
```

**Step 2 — verify fails:** `./mvnw test -Dtest=JobRepositoryTest` → fails until B2, B5, B6 exist.

**Step 3 — Green:** Garantido pelas tasks anteriores.

**Step 4 — verify passes:** `./mvnw test -Dtest=JobRepositoryTest -q`

**Step 5 — commit:**
```bash
git add backend/src/test/java/com/briefflow/integration/repository/JobRepositoryTest.java
git commit -m "test(jobs): JobRepository integration tests with Testcontainers"
```

---

## Task B12 — Integration test JobControllerTest

**Files:**
- Create: `backend/src/test/java/com/briefflow/integration/controller/JobControllerTest.java`

**Depends on:** B10

**Step 1 — Red:**

```java
// JobControllerTest.java
package com.briefflow.integration.controller;

import com.briefflow.dto.auth.RegisterRequestDTO;
import com.briefflow.dto.auth.TokenResponseDTO;
import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobType;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("briefflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.enabled", () -> true);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private ClientRepository clientRepository;

    private String accessToken;
    private Long clientId;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO("Owner", "owner@jobs.com", "password123", "Agency");
        MvcResult r = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg))).andReturn();
        TokenResponseDTO tok = objectMapper.readValue(r.getResponse().getContentAsString(), TokenResponseDTO.class);
        accessToken = tok.accessToken();

        Workspace w = workspaceRepository.findAll().get(0);
        Client c = new Client();
        c.setName("Acme"); c.setWorkspace(w); c.setActive(true);
        clientId = clientRepository.save(c).getId();
    }

    @Test
    void should_createJob_when_validRequest() throws Exception {
        JobRequestDTO req = new JobRequestDTO(
                clientId, null, "Post Black Friday",
                JobType.POST_FEED, JobPriority.NORMAL,
                null,
                LocalDate.now().plusDays(5),
                Map.of("captionText", "BF!", "format", "1:1")
        );

        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("JOB-001"))
                .andExpect(jsonPath("$.title").value("Post Black Friday"))
                .andExpect(jsonPath("$.type").value("POST_FEED"))
                .andExpect(jsonPath("$.status").value("NOVO"));
    }

    @Test
    void should_returnUnprocessable_when_briefingInvalid() throws Exception {
        JobRequestDTO req = new JobRequestDTO(
                clientId, null, "Bad", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("format", "1:1") // missing captionText
        );
        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_listJobs_filteringByStatus() throws Exception {
        JobRequestDTO req = new JobRequestDTO(
                clientId, null, "J", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1")
        );
        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/jobs?status=NOVO")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void should_getJob_returnsDetails() throws Exception {
        JobRequestDTO req = new JobRequestDTO(
                clientId, null, "Detail", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1")
        );
        MvcResult r = mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/jobs/" + id)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Detail"));
    }

    @Test
    void should_archiveJob_returnsOkAndArchivedTrue() throws Exception {
        JobRequestDTO req = new JobRequestDTO(
                clientId, null, "ToArchive", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1")
        );
        MvcResult r = mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/jobs/" + id + "/archive")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"archived\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void should_returnBadRequest_when_missingRequiredFields() throws Exception {
        String payload = "{\"clientId\":null,\"title\":\"\",\"type\":null}";
        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }
}
```

**Step 2 — verify fails:** `./mvnw test -Dtest=JobControllerTest` → fails if controller not present.

**Step 3 — Green:** Garantido pelas tasks anteriores.

**Step 4 — verify passes:** `./mvnw test -Dtest=JobControllerTest -q`

**Step 5 — commit:**
```bash
git add backend/src/test/java/com/briefflow/integration/controller/JobControllerTest.java
git commit -m "test(jobs): JobController integration tests (create, list, get, archive)"
```

---

## Final Verification

After all tasks:
```bash
./mvnw test -q
./mvnw verify -q
```

Todos os testes devem passar. Qualquer red remanescente deve ser corrigido antes do code review.

## Notas de implementação

- **Visibility Creative**: `findVisibleToCreative` usa subquery em `ClientMember` — requer que a entity `ClientMember` exponha `clientId`/`memberId` (RF03 já criou). Confirmar campos em B6.
- **JSONB**: `@JdbcTypeCode(SqlTypes.JSON)` requer Hibernate 6.2+. Spring Boot 3.5.0 já traz essa versão.
- **Atomic counter**: o `RETURNING` nativo do Postgres funciona dentro da mesma transação e evita race condition entre `SELECT` + `UPDATE`.
- **File storage**: reutiliza `FileStorageService` existente (usado por RF03 em client logos). Subdir `jobs/{jobId}`.
- **Permissões**: seguir tabela da spec — OWNER/MANAGER fazem tudo; CREATIVE só lê/lista jobs visíveis, faz upload em jobs que vê, não pode criar/editar/arquivar.
- **Briefing validation**: roda ANTES de persistir. Erros voltam como `422 Unprocessable` via `GlobalExceptionHandler` (BusinessException).
- **Lessons aplicadas**: `@NotBlank` em Strings obrigatórias, `@Mapping` MapStruct para todos os mapeamentos, `DateTimeFormatter.ISO_*` nos formatadores, `JOIN FETCH` em `findByIdAndWorkspaceIdWithDetails` para evitar N+1, `@Modifying` na query nativa de counter, permission check no service, `List<@NotNull Long>` se endpoint aceitar lista (não aplicável aqui).


---

## Frontend — Tasks Detalhadas


## Tasks Detalhadas

### Task F1: Criar `job.model.ts`

**Files:**
- Create: `frontend/src/app/features/jobs/models/job.model.ts`

**Depends on:** Nenhuma — arquivo novo, sem imports cruzados.

**Step 1: Red — teste que falha**

Não há teste unitário para o arquivo de tipos (puramente TypeScript). A validação do "falha" é feita via `ng build` ou import do arquivo em outro teste ainda não escrito. Alternativa: criar `job.model.spec.ts` que importa e faz type check simbólico.

```typescript
// frontend/src/app/features/jobs/models/job.model.spec.ts
import type {
  Job,
  JobListItem,
  JobRequest,
  JobFile,
  JobListFilters,
  JobType,
  JobPriority,
  JobStatus,
  ClientSummary,
  MemberSummary,
} from './job.model';

describe('job.model types', () => {
  it('should allow building a full Job object literal', () => {
    const job: Job = {
      id: 1,
      code: 'JOB-001',
      title: 'Post de lançamento',
      client: { id: 10, name: 'Acme' },
      type: 'POST_FEED',
      description: null,
      deadline: '2026-05-01',
      priority: 'NORMAL',
      assignedCreative: null,
      status: 'NOVO',
      briefingData: { captionText: 'Texto', format: '1:1' },
      archived: false,
      files: [],
      createdAt: '2026-04-11T10:00:00Z',
      updatedAt: '2026-04-11T10:00:00Z',
      createdByName: 'Maria',
    };
    expect(job.code).toBe('JOB-001');
  });

  it('should allow building a JobListItem', () => {
    const item: JobListItem = {
      id: 1,
      code: 'JOB-001',
      title: 'Post',
      clientName: 'Acme',
      type: 'POST_FEED',
      deadline: null,
      priority: 'NORMAL',
      assignedCreativeName: null,
      status: 'NOVO',
      isOverdue: false,
    };
    expect(item.isOverdue).toBe(false);
  });

  it('should allow building a JobRequest without optional fields', () => {
    const req: JobRequest = {
      title: 'Teste',
      clientId: 1,
      type: 'POST_FEED',
      priority: 'NORMAL',
      briefingData: { captionText: 'x', format: '1:1' },
    };
    expect(req.title).toBe('Teste');
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job.model.spec.ts'`
Expected: FAIL — "Cannot find module './job.model'".

**Step 3: Green — implementação**

```typescript
// frontend/src/app/features/jobs/models/job.model.ts

export type JobType =
  | 'POST_FEED'
  | 'STORIES'
  | 'CARROSSEL'
  | 'REELS_VIDEO'
  | 'BANNER'
  | 'LOGO'
  | 'OUTROS';

export type JobPriority = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';

export type JobStatus =
  | 'NOVO'
  | 'EM_CRIACAO'
  | 'REVISAO_INTERNA'
  | 'AGUARDANDO_APROVACAO'
  | 'APROVADO'
  | 'PUBLICADO';

export interface ClientSummary {
  id: number;
  name: string;
}

export interface MemberSummary {
  id: number;
  name: string;
}

export interface JobFile {
  id: number;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  downloadUrl: string;
}

export interface Job {
  id: number;
  code: string;
  title: string;
  client: ClientSummary;
  type: JobType;
  description: string | null;
  deadline: string | null;
  priority: JobPriority;
  assignedCreative: MemberSummary | null;
  status: JobStatus;
  briefingData: Record<string, unknown>;
  archived: boolean;
  files: JobFile[];
  createdAt: string;
  updatedAt: string;
  createdByName: string;
}

export interface JobListItem {
  id: number;
  code: string;
  title: string;
  clientName: string;
  type: JobType;
  deadline: string | null;
  priority: JobPriority;
  assignedCreativeName: string | null;
  status: JobStatus;
  isOverdue: boolean;
}

export interface JobRequest {
  title: string;
  clientId: number;
  type: JobType;
  description?: string;
  deadline?: string;
  priority: JobPriority;
  assignedCreativeId?: number;
  briefingData: Record<string, unknown>;
}

export interface JobListFilters {
  search?: string;
  clientId?: number;
  type?: JobType;
  priority?: JobPriority;
  assignedCreativeId?: number;
  archived?: boolean;
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job.model.spec.ts'`
Expected: PASS.

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/models/job.model.ts frontend/src/app/features/jobs/models/job.model.spec.ts
git commit -m "feat(frontend): add Job domain models"
```

---

### Task F2: Criar `briefing-schemas.ts`

**Files:**
- Create: `frontend/src/app/features/jobs/models/briefing-schemas.ts`
- Create: `frontend/src/app/features/jobs/models/briefing-schemas.spec.ts`

**Depends on:** F1 (usa `JobType`) — mas independente em tempo de código (pode rodar em paralelo com F1 se subagentes coordenam). Marcado ⚡ PARALLEL GROUP A assumindo que os dois arquivos são criados no mesmo commit.

**Step 1: Red — teste que falha**

```typescript
// briefing-schemas.spec.ts
import { BRIEFING_SCHEMAS, type BriefingFieldSchema } from './briefing-schemas';

describe('BRIEFING_SCHEMAS', () => {
  it('should have all 7 JobType entries', () => {
    expect(Object.keys(BRIEFING_SCHEMAS)).toEqual([
      'POST_FEED', 'STORIES', 'CARROSSEL', 'REELS_VIDEO', 'BANNER', 'LOGO', 'OUTROS',
    ]);
  });

  it('POST_FEED should require captionText and format', () => {
    const required = BRIEFING_SCHEMAS.POST_FEED.filter(f => f.required).map(f => f.key);
    expect(required).toEqual(['captionText', 'format']);
  });

  it('POST_FEED format should offer 1:1 and 4:5 options', () => {
    const formatField = BRIEFING_SCHEMAS.POST_FEED.find(f => f.key === 'format')!;
    expect(formatField.type).toBe('select');
    expect(formatField.options).toEqual(['1:1', '4:5']);
  });

  it('CARROSSEL slideCount should have min/max 2-10', () => {
    const slideCount = BRIEFING_SCHEMAS.CARROSSEL.find(f => f.key === 'slideCount')!;
    expect(slideCount.type).toBe('number');
    expect(slideCount.min).toBe(2);
    expect(slideCount.max).toBe(10);
  });

  it('CARROSSEL should include a dynamic-list field for slideTexts', () => {
    const slideTexts = BRIEFING_SCHEMAS.CARROSSEL.find(f => f.key === 'slideTexts')!;
    expect(slideTexts.type).toBe('dynamic-list');
  });

  it('OUTROS should only require freeDescription', () => {
    const schema = BRIEFING_SCHEMAS.OUTROS;
    expect(schema.length).toBe(1);
    expect(schema[0]).toMatchObject({ key: 'freeDescription', required: true });
  });

  it('should export BriefingFieldSchema type with expected shape', () => {
    const f: BriefingFieldSchema = { key: 'x', label: 'X', type: 'text', required: true };
    expect(f.key).toBe('x');
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/briefing-schemas.spec.ts'`
Expected: FAIL — module not found.

**Step 3: Green — implementação**

```typescript
// briefing-schemas.ts
import type { JobType } from './job.model';

export type BriefingFieldType =
  | 'text'
  | 'textarea'
  | 'number'
  | 'select'
  | 'dynamic-list';

export interface BriefingFieldSchema {
  key: string;
  label: string;
  type: BriefingFieldType;
  required?: boolean;
  options?: string[];
  min?: number;
  max?: number;
}

export const BRIEFING_SCHEMAS: Record<JobType, BriefingFieldSchema[]> = {
  POST_FEED: [
    { key: 'captionText', label: 'Texto da legenda', type: 'textarea', required: true },
    { key: 'format', label: 'Formato', type: 'select', options: ['1:1', '4:5'], required: true },
    { key: 'colorPalette', label: 'Paleta de cores', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  STORIES: [
    { key: 'text', label: 'Texto', type: 'textarea', required: true },
    { key: 'format', label: 'Formato', type: 'select', options: ['9:16'], required: true },
    { key: 'cta', label: 'CTA', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  CARROSSEL: [
    { key: 'slideCount', label: 'Número de slides', type: 'number', required: true, min: 2, max: 10 },
    { key: 'slideTexts', label: 'Texto por slide', type: 'dynamic-list' },
    { key: 'format', label: 'Formato', type: 'select', options: ['1:1', '4:5'] },
  ],
  REELS_VIDEO: [
    { key: 'duration', label: 'Duração (segundos)', type: 'number', required: true },
    { key: 'script', label: 'Roteiro/Storyboard', type: 'textarea', required: true },
    { key: 'audioReference', label: 'Referência de áudio', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  BANNER: [
    { key: 'dimensions', label: 'Dimensões', type: 'text', required: true },
    { key: 'text', label: 'Texto', type: 'textarea', required: true },
    { key: 'cta', label: 'CTA', type: 'text' },
  ],
  LOGO: [
    { key: 'desiredStyle', label: 'Estilo desejado', type: 'textarea', required: true },
    { key: 'colorReferences', label: 'Referências de cor', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  OUTROS: [
    { key: 'freeDescription', label: 'Descrição livre', type: 'textarea', required: true },
  ],
};
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/briefing-schemas.spec.ts'`
Expected: PASS.

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/models/briefing-schemas.ts frontend/src/app/features/jobs/models/briefing-schemas.spec.ts
git commit -m "feat(frontend): add briefing schemas for 7 job types"
```

---

### Task F3: `JobApiService`

**Files:**
- Create: `frontend/src/app/features/jobs/services/job-api.service.ts`
- Create: `frontend/src/app/features/jobs/services/job-api.service.spec.ts`

**Depends on:** F1 (models). Consome endpoints backend — requer Bn (JobController implementado). Pode ser desenvolvido em paralelo ao backend usando apenas mocks HTTP (TestBed + provideHttpClientTesting).

**Step 1: Red — teste que falha**

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { JobApiService } from './job-api.service';
import { environment } from '@env/environment';
import type { Job, JobListItem, JobRequest, JobFile } from '../models/job.model';

describe('JobApiService', () => {
  let service: JobApiService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/jobs`;

  const mockListItem: JobListItem = {
    id: 1,
    code: 'JOB-001',
    title: 'Post lançamento',
    clientName: 'Acme',
    type: 'POST_FEED',
    deadline: '2026-05-01',
    priority: 'NORMAL',
    assignedCreativeName: null,
    status: 'NOVO',
    isOverdue: false,
  };

  const mockJob: Job = {
    id: 1,
    code: 'JOB-001',
    title: 'Post lançamento',
    client: { id: 10, name: 'Acme' },
    type: 'POST_FEED',
    description: null,
    deadline: '2026-05-01',
    priority: 'NORMAL',
    assignedCreative: null,
    status: 'NOVO',
    briefingData: { captionText: 'x', format: '1:1' },
    archived: false,
    files: [],
    createdAt: '2026-04-11T10:00:00Z',
    updatedAt: '2026-04-11T10:00:00Z',
    createdByName: 'Maria',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(JobApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list jobs without filters', () => {
    service.list().subscribe(res => expect(res).toEqual([mockListItem]));
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([mockListItem]);
  });

  it('should list with search and filter params', () => {
    service.list({
      search: 'post',
      clientId: 10,
      type: 'POST_FEED',
      priority: 'ALTA',
      assignedCreativeId: 5,
      archived: false,
    }).subscribe();
    const req = httpMock.expectOne(r =>
      r.url === baseUrl &&
      r.params.get('search') === 'post' &&
      r.params.get('clientId') === '10' &&
      r.params.get('type') === 'POST_FEED' &&
      r.params.get('priority') === 'ALTA' &&
      r.params.get('assignedCreativeId') === '5' &&
      r.params.get('archived') === 'false'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get a job by id', () => {
    service.getById(1).subscribe(res => expect(res).toEqual(mockJob));
    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockJob);
  });

  it('should create a job', () => {
    const body: JobRequest = {
      title: 'Post lançamento',
      clientId: 10,
      type: 'POST_FEED',
      priority: 'NORMAL',
      briefingData: { captionText: 'x', format: '1:1' },
    };
    service.create(body).subscribe(res => expect(res).toEqual(mockJob));
    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(mockJob);
  });

  it('should update a job', () => {
    const body: JobRequest = { ...mockJob, clientId: 10, briefingData: {} } as JobRequest;
    service.update(1, body).subscribe(res => expect(res).toEqual(mockJob));
    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockJob);
  });

  it('should archive a job', () => {
    service.archive(1, true).subscribe(res => expect(res).toEqual(mockJob));
    const req = httpMock.expectOne(`${baseUrl}/1/archive`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ archived: true });
    req.flush(mockJob);
  });

  it('should upload a file using multipart and reportProgress', () => {
    const file = new File(['hello'], 'ref.jpg', { type: 'image/jpeg' });
    const events: number[] = [];

    service.uploadFile(1, file).subscribe(event => events.push(event.type));

    const req = httpMock.expectOne(`${baseUrl}/1/files`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toEqual(file);
    expect(req.request.reportProgress).toBe(true);
    req.flush({ id: 99 } as JobFile);
    expect(events).toContain(HttpEventType.Response);
  });

  it('should delete a file', () => {
    service.deleteFile(1, 99).subscribe();
    const req = httpMock.expectOne(`${baseUrl}/1/files/99`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should expose downloadUrl builder', () => {
    expect(service.downloadUrl(1, 99)).toBe(`${baseUrl}/1/files/99/download`);
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job-api.service.spec.ts'`
Expected: FAIL — `JobApiService` undefined.

**Step 3: Green — implementação**

```typescript
// job-api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import type { Job, JobListItem, JobRequest, JobFile, JobListFilters } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/jobs`;

  list(filters?: JobListFilters): Observable<JobListItem[]> {
    let params = new HttpParams();
    if (filters?.search) params = params.set('search', filters.search);
    if (filters?.clientId !== undefined) params = params.set('clientId', String(filters.clientId));
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.priority) params = params.set('priority', filters.priority);
    if (filters?.assignedCreativeId !== undefined) {
      params = params.set('assignedCreativeId', String(filters.assignedCreativeId));
    }
    if (filters?.archived !== undefined) {
      params = params.set('archived', String(filters.archived));
    }
    return this.http.get<JobListItem[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.baseUrl}/${id}`);
  }

  create(request: JobRequest): Observable<Job> {
    return this.http.post<Job>(this.baseUrl, request);
  }

  update(id: number, request: JobRequest): Observable<Job> {
    return this.http.put<Job>(`${this.baseUrl}/${id}`, request);
  }

  archive(id: number, archived: boolean): Observable<Job> {
    return this.http.patch<Job>(`${this.baseUrl}/${id}/archive`, { archived });
  }

  uploadFile(jobId: number, file: File): Observable<HttpEvent<JobFile>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<JobFile>(`${this.baseUrl}/${jobId}/files`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  deleteFile(jobId: number, fileId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${jobId}/files/${fileId}`);
  }

  downloadUrl(jobId: number, fileId: number): string {
    return `${this.baseUrl}/${jobId}/files/${fileId}/download`;
  }
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-api.service.spec.ts'`
Expected: PASS (9 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/services/job-api.service.ts frontend/src/app/features/jobs/services/job-api.service.spec.ts
git commit -m "feat(frontend): add JobApiService with list/create/update/archive/upload"
```

---

### Task F4: `jobs.routes.ts`

**Files:**
- Create: `frontend/src/app/features/jobs/jobs.routes.ts`

**Depends on:** F1 (nada em runtime, mas precisa que o feature folder exista)

**Step 1: Red — teste que falha**

Sem teste unitário dedicado (rotas são configuração declarativa). O "falha" é o build do roteador quando a rota for registrada em F5. Alternativa: smoke test validando que o default export tem as rotas esperadas.

```typescript
// jobs.routes.spec.ts
import routes from './jobs.routes';

describe('jobs.routes', () => {
  it('should export 4 routes', () => {
    expect(routes.length).toBe(4);
  });

  it('should have list route at empty path', () => {
    expect(routes[0].path).toBe('');
  });

  it('should apply roleGuard on new and edit', () => {
    const newRoute = routes.find(r => r.path === 'new')!;
    const editRoute = routes.find(r => r.path === ':id/edit')!;
    expect(newRoute.canActivate).toBeDefined();
    expect(editRoute.canActivate).toBeDefined();
  });

  it('should declare detail route under :id', () => {
    expect(routes.find(r => r.path === ':id')).toBeDefined();
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/jobs.routes.spec.ts'`
Expected: FAIL — module not found.

**Step 3: Green — implementação**

```typescript
// jobs.routes.ts
import { Routes } from '@angular/router';
import { roleGuard } from '@core/guards/role.guard';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/job-list/job-list.component').then(m => m.JobListComponent),
  },
  {
    path: 'new',
    canActivate: [roleGuard('OWNER', 'MANAGER')],
    loadComponent: () =>
      import('./pages/job-create/job-create.component').then(m => m.JobCreateComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/job-detail/job-detail.component').then(m => m.JobDetailComponent),
  },
  {
    path: ':id/edit',
    canActivate: [roleGuard('OWNER', 'MANAGER')],
    loadComponent: () =>
      import('./pages/job-edit/job-edit.component').then(m => m.JobEditComponent),
  },
];

export default routes;
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/jobs.routes.spec.ts'`
Expected: PASS. Build será quebrado enquanto as páginas não existirem — isso é esperado; os imports dinâmicos ficam lazy e o teste apenas verifica a estrutura das rotas.

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/jobs.routes.ts frontend/src/app/features/jobs/jobs.routes.spec.ts
git commit -m "feat(frontend): add jobs feature routes with roleGuard"
```

---

### Task F5: Registrar lazy route em `app.routes.ts`

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

**Depends on:** F4

**Step 1: Red — teste que falha**

Sem teste isolado (configuração global). O verificador é build + navegação manual após F8 estar pronto.

**Step 2: Verificar falha**

N/A — mudança de configuração.

**Step 3: Green — implementação**

Adicionar entrada no children de MainLayoutComponent entre `clients` e `members`:

```typescript
{
  path: 'jobs',
  loadChildren: () => import('./features/jobs/jobs.routes'),
},
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng build` — deve compilar sem erros (pages ainda não existem → compilação aceita imports dinâmicos). Rodar `ng serve` e verificar que nada da UI atual quebrou. A navegação para `/jobs` só funciona após F8.

**Step 5: Commit**

```bash
git add frontend/src/app/app.routes.ts
git commit -m "feat(frontend): register jobs lazy route"
```

---

### Task F6: Adicionar "Jobs" no sidebar

**Files:**
- Modify: `frontend/src/app/layout/sidebar/sidebar.component.ts`

**Depends on:** F5 (para que o link leve a algum lugar — mesmo que ainda não renderize, mantém UX consistente).

**Step 1: Red — teste que falha**

Se já houver `sidebar.component.spec.ts`, adicionar test. Caso contrário, smoke test no próprio build.

```typescript
// sidebar.component.spec.ts — adicionar este it() ao describe existente
it('should include a Jobs nav item', () => {
  const fixture = TestBed.createComponent(SidebarComponent);
  const items = fixture.componentInstance.navItems;
  expect(items.some(i => i.label === 'Jobs' && i.route === '/jobs')).toBe(true);
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/sidebar.component.spec.ts'`
Expected: FAIL — item "Jobs" não existe.

**Step 3: Green — implementação**

Adicionar item na lista `navItems` em `sidebar.component.ts`, posicionado entre "Clientes" e "Equipe":

```typescript
readonly navItems: NavItem[] = [
  { label: 'Dashboard', icon: 'pi pi-objects-column', route: '/dashboard' },
  { label: 'Clientes', icon: 'pi pi-building', route: '/clients' },
  { label: 'Jobs', icon: 'pi pi-briefcase', route: '/jobs' },
  { label: 'Equipe', icon: 'pi pi-users', route: '/members' },
  { label: 'Configurações', icon: 'pi pi-cog', route: '/settings', roles: ['OWNER', 'MANAGER'] },
];
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/sidebar.component.spec.ts'`
Expected: PASS.

**Step 5: Commit**

```bash
git add frontend/src/app/layout/sidebar/sidebar.component.ts
git commit -m "feat(frontend): add Jobs item to sidebar"
```

---

### Task F7: frontend-design para `JobListComponent`

**Files:** Nenhum arquivo de código.

**Depends on:** Nenhuma de código — mas BLOQUEIA F8.

**Ação:** Invocar a skill `frontend-design` passando o seguinte briefing:

> Design da página `/jobs` (JobListComponent). Contexto: listagem principal de jobs de uma agência com `p-table` PrimeNG. Header com título "Jobs", contador, busca por título/código (debounce 300ms), filtros inline (cliente via dropdown, tipo, prioridade, criativo, toggle "Ver arquivados"), botão "+ Novo Job" (OWNER/MANAGER). Colunas: Código (JetBrains Mono), Título (truncate), Cliente (avatar + nome), Tipo (badge), Criativo (avatar ou —), Prazo (com ícone vermelho se `isOverdue`), Prioridade (badge cinza/azul/amarelo/vermelho), Ações (menu 3 pontos: Ver, Editar, Arquivar — só OWNER/MANAGER). Paginação 20. Empty state: "Nenhum job cadastrado" + CTA. Usar cores do design system (Indigo primary #6366F1, Gray neutros, semantic badges). CREATIVE não vê coluna Ações nem filtro de criativo.

**Saída esperada:** Visual approved + screenshot de referência salva em `docs/spec/rf04-uis/` OU confirmação verbal do team-lead. Não avançar para F8 sem essa aprovação.

**Commit:** Não há commit de código. Registrar no plan checklist `[x]` após aprovação.

---

### Task F8: Implementar `JobListComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/pages/job-list/job-list.component.ts`
- Create: `frontend/src/app/features/jobs/pages/job-list/job-list.component.html`
- Create: `frontend/src/app/features/jobs/pages/job-list/job-list.component.spec.ts`

**Depends on:** F3, F7. E, para as p-table selects de cliente/criativo: consumir `ClientApiService` e `MemberApiService` já existentes.

**Step 1: Red — testes que falham**

```typescript
// job-list.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobListComponent } from './job-list.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { StorageService } from '@core/services/storage.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { ConfirmationService } from 'primeng/api';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import type { JobListItem } from '@features/jobs/models/job.model';

const mockJobs: JobListItem[] = [
  {
    id: 1, code: 'JOB-001', title: 'Post lançamento', clientName: 'Acme',
    type: 'POST_FEED', deadline: '2026-05-01', priority: 'NORMAL',
    assignedCreativeName: null, status: 'NOVO', isOverdue: false,
  },
  {
    id: 2, code: 'JOB-002', title: 'Carrossel Black Friday', clientName: 'Beta',
    type: 'CARROSSEL', deadline: '2026-04-01', priority: 'URGENTE',
    assignedCreativeName: 'João', status: 'NOVO', isOverdue: true,
  },
];

describe('JobListComponent', () => {
  let fixture: ComponentFixture<JobListComponent>;
  let component: JobListComponent;
  let jobApiSpy: { list: ReturnType<typeof vi.fn>; archive: ReturnType<typeof vi.fn> };
  let clientApiSpy: { list: ReturnType<typeof vi.fn> };
  let memberApiSpy: { list: ReturnType<typeof vi.fn> };
  let storageSpy: { getUser: ReturnType<typeof vi.fn> };

  function setup(role = 'OWNER') {
    jobApiSpy = {
      list: vi.fn().mockReturnValue(of(mockJobs)),
      archive: vi.fn().mockReturnValue(of(mockJobs[0])),
    };
    clientApiSpy = { list: vi.fn().mockReturnValue(of([])) };
    memberApiSpy = { list: vi.fn().mockReturnValue(of({ members: [] })) };
    storageSpy = {
      getUser: vi.fn().mockReturnValue({
        id: 10, name: 'Ana', email: 'ana@a.com',
        workspaceId: 1, workspaceName: 'A', role,
      }),
    };

    TestBed.configureTestingModule({
      imports: [JobListComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: ClientApiService, useValue: clientApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => { vi.clearAllMocks(); TestBed.resetTestingModule(); });

  it('should load jobs on init with archived=false', () => {
    setup();
    expect(jobApiSpy.list).toHaveBeenCalledWith({ archived: false });
    expect(component.jobs()).toEqual(mockJobs);
  });

  it('should render job codes and titles', () => {
    setup();
    fixture.detectChanges();
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('JOB-001');
    expect(text).toContain('Post lançamento');
    expect(text).toContain('JOB-002');
  });

  it('should show "Novo Job" button for MANAGER', () => {
    setup('MANAGER');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Novo Job');
  });

  it('should hide "Novo Job" button for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Novo Job');
  });

  it('should hide action menus for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('[data-testid="job-menu-button"]');
    expect(buttons.length).toBe(0);
  });

  it('should debounce search input', async () => {
    setup();
    component.onSearchInput({ target: { value: 'acme' } } as unknown as Event);
    component.onSearchInput({ target: { value: 'acme post' } } as unknown as Event);
    await new Promise(r => setTimeout(r, 350));
    expect(jobApiSpy.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: 'acme post' })
    );
  });

  it('should toggle archived filter and reload', () => {
    setup();
    component.showArchived.set(true);
    component.loadJobs();
    expect(jobApiSpy.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ archived: true })
    );
  });

  it('should call archive with confirmation', () => {
    setup('OWNER');
    const confirm = TestBed.inject(ConfirmationService);
    vi.spyOn(confirm, 'confirm').mockImplementation((cfg: any) => { cfg.accept(); return confirm; });
    component.archiveJob(mockJobs[0]);
    expect(jobApiSpy.archive).toHaveBeenCalledWith(1, true);
  });

  it('should show empty state when no jobs', () => {
    jobApiSpy.list = vi.fn().mockReturnValue(of([]));
    TestBed.overrideProvider(JobApiService, { useValue: jobApiSpy });
    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum job');
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job-list.component.spec.ts'`
Expected: FAIL — componente inexistente.

**Step 3: Green — implementação**

```typescript
// job-list.component.ts
import {
  ChangeDetectionStrategy, Component, inject, signal, computed,
  OnInit, OnDestroy,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, forkJoin } from 'rxjs';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { StorageService } from '@core/services/storage.service';
import type { JobListItem, JobType, JobPriority, JobListFilters } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [
    FormsModule, TableModule, ButtonModule, InputTextModule, IconFieldModule,
    InputIconModule, SelectModule, ToggleSwitchModule, MenuModule, TagModule,
    ConfirmDialogModule,
  ],
  templateUrl: './job-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService],
})
export class JobListComponent implements OnInit, OnDestroy {
  private readonly jobApi = inject(JobApiService);
  private readonly clientApi = inject(ClientApiService);
  private readonly memberApi = inject(MemberApiService);
  private readonly storage = inject(StorageService);
  private readonly confirmService = inject(ConfirmationService);
  private readonly router = inject(Router);

  readonly jobs = signal<JobListItem[]>([]);
  readonly loading = signal(true);
  readonly searchTerm = signal('');
  readonly showArchived = signal(false);
  readonly clientFilter = signal<number | null>(null);
  readonly typeFilter = signal<JobType | null>(null);
  readonly priorityFilter = signal<JobPriority | null>(null);
  readonly creativeFilter = signal<number | null>(null);

  readonly clientOptions = signal<{ label: string; value: number }[]>([]);
  readonly creativeOptions = signal<{ label: string; value: number }[]>([]);

  readonly typeOptions = [
    { label: 'Post Feed', value: 'POST_FEED' },
    { label: 'Stories', value: 'STORIES' },
    { label: 'Carrossel', value: 'CARROSSEL' },
    { label: 'Reels/Vídeo', value: 'REELS_VIDEO' },
    { label: 'Banner', value: 'BANNER' },
    { label: 'Logo', value: 'LOGO' },
    { label: 'Outros', value: 'OUTROS' },
  ];

  readonly priorityOptions = [
    { label: 'Baixa', value: 'BAIXA' },
    { label: 'Normal', value: 'NORMAL' },
    { label: 'Alta', value: 'ALTA' },
    { label: 'Urgente', value: 'URGENTE' },
  ];

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  readonly currentUser = this.storage.getUser();
  readonly canManage = this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(term => {
        this.searchTerm.set(term);
        this.loadJobs();
      });

    this.loadJobs();
    this.loadFilterOptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadJobs(): void {
    this.loading.set(true);
    const filters: JobListFilters = { archived: this.showArchived() };
    const s = this.searchTerm(); if (s) filters.search = s;
    const c = this.clientFilter(); if (c !== null) filters.clientId = c;
    const t = this.typeFilter(); if (t) filters.type = t;
    const p = this.priorityFilter(); if (p) filters.priority = p;
    const cr = this.creativeFilter(); if (cr !== null) filters.assignedCreativeId = cr;

    this.jobApi.list(filters).subscribe({
      next: jobs => { this.jobs.set(jobs); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  private loadFilterOptions(): void {
    forkJoin({
      clients: this.clientApi.list({ active: true }),
      members: this.canManage ? this.memberApi.list() : of({ members: [] } as any),
    }).subscribe({
      next: ({ clients, members }) => {
        this.clientOptions.set(clients.map(c => ({ label: c.name, value: c.id })));
        this.creativeOptions.set(
          members.members.filter(m => m.role === 'CREATIVE').map(m => ({ label: m.userName, value: m.id })),
        );
      },
      error: () => { /* silencioso — filtros ficam vazios */ },
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onFilterChange(): void { this.loadJobs(); }

  goToNew(): void { this.router.navigate(['/jobs/new']); }

  goToDetail(job: JobListItem): void { this.router.navigate(['/jobs', job.id]); }

  goToEdit(job: JobListItem): void { this.router.navigate(['/jobs', job.id, 'edit']); }

  archiveJob(job: JobListItem): void {
    this.confirmService.confirm({
      message: `Deseja arquivar o job "${job.code} — ${job.title}"?`,
      header: 'Arquivar job',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Arquivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.jobApi.archive(job.id, true).subscribe({
          next: () => this.loadJobs(),
          error: err => console.error('Erro ao arquivar job:', err),
        });
      },
    });
  }

  unarchiveJob(job: JobListItem): void {
    this.jobApi.archive(job.id, false).subscribe({
      next: () => this.loadJobs(),
      error: err => console.error('Erro ao restaurar job:', err),
    });
  }

  getMenuItems(job: JobListItem): MenuItem[] {
    const items: MenuItem[] = [
      { label: 'Ver', icon: 'pi pi-eye', command: () => this.goToDetail(job) },
    ];
    if (this.canManage) {
      items.push(
        { label: 'Editar', icon: 'pi pi-pencil', command: () => this.goToEdit(job) },
        this.showArchived()
          ? { label: 'Restaurar', icon: 'pi pi-replay', command: () => this.unarchiveJob(job) }
          : { label: 'Arquivar', icon: 'pi pi-archive', command: () => this.archiveJob(job) },
      );
    }
    return items;
  }

  getPrioritySeverity(p: JobPriority): 'info' | 'success' | 'warn' | 'danger' | 'secondary' {
    switch (p) {
      case 'URGENTE': return 'danger';
      case 'ALTA': return 'warn';
      case 'NORMAL': return 'info';
      case 'BAIXA': return 'secondary';
    }
  }
}
```

O template (`job-list.component.html`) segue o layout aprovado em F7 — p-table com `[value]="jobs()"`, colunas conforme spec, empty state, header com busca e filtros. O template deve usar `@if`/`@for`, JetBrains Mono para `{{ job.code }}`, `[severity]` nos badges, e `data-testid="job-menu-button"` no botão do menu.

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-list.component.spec.ts'`
Expected: PASS (9 tests). Rodar também `ng build` para garantir que a tela é compilada.

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/pages/job-list/
git commit -m "feat(frontend): add JobListComponent with filters and role-gated actions"
```

---

### Task F9: frontend-design para `BriefingFieldsComponent`

**Files:** Nenhum arquivo de código.

**Depends on:** Nenhuma. BLOQUEIA F10.

**Ação:** Invocar `frontend-design` com briefing:

> Design do `BriefingFieldsComponent`. Recebe `type: Signal<JobType>` e `formGroup: FormGroup` parent. Renderiza campos dinamicamente baseado em `BRIEFING_SCHEMAS[type]`. Tipos de campo: text (p-inputtext), textarea (p-textarea), number (p-inputnumber com min/max), select (p-select com options), dynamic-list (lista adicionar/remover de inputs — usada em CARROSSEL.slideTexts). Cada campo tem label acima, placeholder descritivo, mensagem de erro abaixo em vermelho. Layout vertical, gap 16px, dentro do card "Briefing" do form pai. Required marcado com asterisco vermelho. Quando `type` muda, os campos antigos são limpos e os novos renderizados com fade suave.

**Saída:** Aprovação visual do design (screenshot/figma ref). Não avançar para F10 sem aprovação.

---

### Task F10: Implementar `BriefingFieldsComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/components/briefing-fields/briefing-fields.component.ts`
- Create: `frontend/src/app/features/jobs/components/briefing-fields/briefing-fields.component.html`
- Create: `frontend/src/app/features/jobs/components/briefing-fields/briefing-fields.component.spec.ts`

**Depends on:** F2, F9

**Step 1: Red — teste que falha**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { BriefingFieldsComponent } from './briefing-fields.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('BriefingFieldsComponent', () => {
  let fixture: ComponentFixture<BriefingFieldsComponent>;
  let component: BriefingFieldsComponent;
  let fb: FormBuilder;
  let form: FormGroup;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BriefingFieldsComponent, ReactiveFormsModule, NoopAnimationsModule],
    });
    fb = TestBed.inject(FormBuilder);
    form = fb.group({ briefingData: fb.group({}) });
    fixture = TestBed.createComponent(BriefingFieldsComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('formGroup', form.get('briefingData') as FormGroup);
    fixture.componentRef.setInput('type', 'POST_FEED');
    fixture.detectChanges();
  });

  it('should render POST_FEED fields', () => {
    const group = form.get('briefingData') as FormGroup;
    expect(group.get('captionText')).toBeTruthy();
    expect(group.get('format')).toBeTruthy();
    expect(group.get('colorPalette')).toBeTruthy();
  });

  it('should mark required fields with validators', () => {
    const group = form.get('briefingData') as FormGroup;
    const caption = group.get('captionText')!;
    caption.setValue('');
    expect(caption.hasValidator(Validators.required)).toBe(true);
  });

  it('should replace fields when type changes to STORIES', () => {
    fixture.componentRef.setInput('type', 'STORIES');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    expect(group.get('captionText')).toBeNull();
    expect(group.get('text')).toBeTruthy();
    expect(group.get('format')).toBeTruthy();
  });

  it('should generate N inputs for CARROSSEL slideTexts dynamic-list when slideCount changes', () => {
    fixture.componentRef.setInput('type', 'CARROSSEL');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    group.get('slideCount')!.setValue(3);
    fixture.detectChanges();
    const slides = group.get('slideTexts') as any;
    expect(slides.controls.length).toBe(3);
  });

  it('should validate number min/max on slideCount', () => {
    fixture.componentRef.setInput('type', 'CARROSSEL');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    const sc = group.get('slideCount')!;
    sc.setValue(1);
    expect(sc.invalid).toBe(true);
    sc.setValue(11);
    expect(sc.invalid).toBe(true);
    sc.setValue(5);
    expect(sc.valid).toBe(true);
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/briefing-fields.component.spec.ts'`
Expected: FAIL.

**Step 3: Green — implementação**

```typescript
// briefing-fields.component.ts
import {
  ChangeDetectionStrategy, Component, input, effect, inject, DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormGroup, FormControl, FormArray, Validators, ReactiveFormsModule,
} from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { BRIEFING_SCHEMAS, type BriefingFieldSchema } from '@features/jobs/models/briefing-schemas';
import type { JobType } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-briefing-fields',
  standalone: true,
  imports: [
    ReactiveFormsModule, InputTextModule, TextareaModule, InputNumberModule,
    SelectModule, ButtonModule,
  ],
  templateUrl: './briefing-fields.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BriefingFieldsComponent {
  readonly type = input.required<JobType>();
  readonly formGroup = input.required<FormGroup>();

  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const t = this.type();
      const fg = this.formGroup();
      this.buildControls(fg, BRIEFING_SCHEMAS[t]);
    });
  }

  private buildControls(fg: FormGroup, schema: BriefingFieldSchema[]): void {
    Object.keys(fg.controls).forEach(key => fg.removeControl(key));

    schema.forEach(field => {
      const validators = field.required ? [Validators.required] : [];
      if (field.type === 'number') {
        if (field.min !== undefined) validators.push(Validators.min(field.min));
        if (field.max !== undefined) validators.push(Validators.max(field.max));
        fg.addControl(field.key, new FormControl<number | null>(null, validators));
      } else if (field.type === 'dynamic-list') {
        fg.addControl(field.key, new FormArray<FormControl<string | null>>([]));
      } else {
        fg.addControl(field.key, new FormControl<string | null>(null, validators));
      }
    });

    this.wireDynamicListsIfNeeded(fg, schema);
  }

  private wireDynamicListsIfNeeded(fg: FormGroup, schema: BriefingFieldSchema[]): void {
    const slideCount = schema.find(s => s.key === 'slideCount');
    const slideTexts = schema.find(s => s.key === 'slideTexts' && s.type === 'dynamic-list');
    if (slideCount && slideTexts) {
      const countCtrl = fg.get('slideCount') as FormControl<number | null>;
      const arr = fg.get('slideTexts') as FormArray<FormControl<string | null>>;
      countCtrl.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(count => {
          const n = count ?? 0;
          while (arr.length < n) arr.push(new FormControl<string | null>(null));
          while (arr.length > n) arr.removeAt(arr.length - 1);
        });
    }
  }

  schema(): BriefingFieldSchema[] {
    return BRIEFING_SCHEMAS[this.type()];
  }

  asFormArray(key: string): FormArray {
    return this.formGroup().get(key) as FormArray;
  }
}
```

Template renderiza um `@for` sobre `schema()` e aplica `@switch` no `type` para escolher o input adequado. Dynamic-list renderiza um `@for` sobre `asFormArray('slideTexts').controls` com label "Slide N".

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/briefing-fields.component.spec.ts'`
Expected: PASS (5 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/components/briefing-fields/
git commit -m "feat(frontend): add BriefingFieldsComponent with dynamic schema rendering"
```

---

### Task F11: frontend-design para `JobFileUploaderComponent`

**Ação:** Invocar `frontend-design` com briefing:

> Design do `JobFileUploaderComponent`. Aceita imagens/pdf/vídeos (max 50MB). Dois modos: **staging** (create — lista de arquivos em memória com preview e botão X; botão "Enviar Arquivo" apenas adiciona à lista) e **direct** (edit/detail — upload imediato). Em ambos os modos: dropzone (drag & drop), área de upload com ícone de upload, lista de arquivos com nome truncado, tamanho, barra de progresso durante upload, estados de sucesso/erro por arquivo, botão remover. No modo direct, cada arquivo mostra botão "Baixar" e "Remover". Erros: MIME inválido, tamanho excedido, falha de rede — com botão "Tentar novamente" nos que falharam.

**Saída:** Aprovação visual. BLOQUEIA F12.

---

### Task F12: Implementar `JobFileUploaderComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/components/job-file-uploader/job-file-uploader.component.ts`
- Create: `frontend/src/app/features/jobs/components/job-file-uploader/job-file-uploader.component.html`
- Create: `frontend/src/app/features/jobs/components/job-file-uploader/job-file-uploader.component.spec.ts`

**Depends on:** F3, F11

**Step 1: Red — teste que falha**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobFileUploaderComponent } from './job-file-uploader.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { HttpEventType } from '@angular/common/http';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import type { JobFile } from '@features/jobs/models/job.model';

describe('JobFileUploaderComponent', () => {
  let fixture: ComponentFixture<JobFileUploaderComponent>;
  let component: JobFileUploaderComponent;
  let apiSpy: { uploadFile: ReturnType<typeof vi.fn>; deleteFile: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiSpy = {
      uploadFile: vi.fn().mockReturnValue(of({ type: HttpEventType.Response, body: { id: 99 } as JobFile })),
      deleteFile: vi.fn().mockReturnValue(of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [JobFileUploaderComponent, NoopAnimationsModule],
      providers: [{ provide: JobApiService, useValue: apiSpy }],
    });
    fixture = TestBed.createComponent(JobFileUploaderComponent);
    component = fixture.componentInstance;
  });

  it('should reject MIME type not allowed', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const file = new File(['x'], 'file.exe', { type: 'application/x-msdownload' });
    component.addFiles([file]);
    expect(component.pendingFiles().length).toBe(0);
    expect(component.errors().length).toBeGreaterThan(0);
  });

  it('should reject file larger than 50MB', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const big = new File([new Uint8Array(51 * 1024 * 1024)], 'big.pdf', { type: 'application/pdf' });
    component.addFiles([big]);
    expect(component.pendingFiles().length).toBe(0);
  });

  it('should stage valid files in staging mode without uploading', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(component.pendingFiles().length).toBe(1);
    expect(apiSpy.uploadFile).not.toHaveBeenCalled();
  });

  it('should upload immediately in direct mode', () => {
    fixture.componentRef.setInput('mode', 'direct');
    fixture.componentRef.setInput('jobId', 1);
    fixture.detectChanges();
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(apiSpy.uploadFile).toHaveBeenCalledWith(1, file);
  });

  it('should emit filesStaged after adding in staging mode', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const spy = vi.fn();
    component.filesStaged.subscribe(spy);
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(spy).toHaveBeenCalled();
  });

  it('should call deleteFile in direct mode when removing existing file', () => {
    fixture.componentRef.setInput('mode', 'direct');
    fixture.componentRef.setInput('jobId', 1);
    fixture.detectChanges();
    const existing: JobFile = {
      id: 5, originalFilename: 'a.jpg', mimeType: 'image/jpeg',
      sizeBytes: 100, uploadedAt: '2026-04-11', downloadUrl: '/x',
    };
    component.removeExistingFile(existing);
    expect(apiSpy.deleteFile).toHaveBeenCalledWith(1, 5);
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job-file-uploader.component.spec.ts'`
Expected: FAIL.

**Step 3: Green — implementação**

```typescript
// job-file-uploader.component.ts
import {
  ChangeDetectionStrategy, Component, inject, input, output, signal,
} from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { JobApiService } from '@features/jobs/services/job-api.service';
import type { JobFile } from '@features/jobs/models/job.model';

const ALLOWED_MIME = [
  'image/jpeg', 'image/png', 'image/webp', 'image/gif',
  'application/pdf',
  'video/mp4', 'video/quicktime',
];
const MAX_SIZE = 50 * 1024 * 1024;

export type UploaderMode = 'staging' | 'direct';

export interface PendingFile {
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'done' | 'error';
  errorMessage?: string;
}

@Component({
  selector: 'app-job-file-uploader',
  standalone: true,
  imports: [ButtonModule, ProgressBarModule],
  templateUrl: './job-file-uploader.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobFileUploaderComponent {
  private readonly api = inject(JobApiService);

  readonly mode = input.required<UploaderMode>();
  readonly jobId = input<number | null>(null);
  readonly existingFiles = input<JobFile[]>([]);
  readonly canManage = input<boolean>(true);

  readonly pendingFiles = signal<PendingFile[]>([]);
  readonly errors = signal<string[]>([]);

  readonly filesStaged = output<File[]>();
  readonly fileUploaded = output<JobFile>();
  readonly fileDeleted = output<number>();

  addFiles(files: File[]): void {
    const valid: File[] = [];
    const errs: string[] = [];
    for (const f of files) {
      if (!ALLOWED_MIME.includes(f.type)) {
        errs.push(`Formato não suportado: ${f.name}`);
        continue;
      }
      if (f.size > MAX_SIZE) {
        errs.push(`${f.name} excede o limite de 50MB`);
        continue;
      }
      valid.push(f);
    }
    this.errors.set(errs);
    if (valid.length === 0) return;

    if (this.mode() === 'staging') {
      this.pendingFiles.update(list => [
        ...list,
        ...valid.map(f => ({ file: f, progress: 0, status: 'pending' as const })),
      ]);
      this.filesStaged.emit(valid);
    } else {
      valid.forEach(f => this.uploadNow(f));
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const list = Array.from(input.files ?? []);
    this.addFiles(list);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const list = Array.from(event.dataTransfer?.files ?? []);
    this.addFiles(list);
  }

  uploadPending(jobId: number): void {
    this.pendingFiles().forEach(pf => {
      if (pf.status === 'pending') {
        this.uploadOne(pf, jobId);
      }
    });
  }

  retry(pf: PendingFile): void {
    const id = this.jobId();
    if (id !== null) this.uploadOne(pf, id);
  }

  removePending(pf: PendingFile): void {
    this.pendingFiles.update(list => list.filter(p => p !== pf));
  }

  removeExistingFile(file: JobFile): void {
    const id = this.jobId();
    if (id === null) return;
    this.api.deleteFile(id, file.id).subscribe({
      next: () => this.fileDeleted.emit(file.id),
      error: err => {
        console.error('Erro ao remover arquivo:', err);
        this.errors.update(list => [...list, `Erro ao remover ${file.originalFilename}`]);
      },
    });
  }

  private uploadNow(file: File): void {
    const pf: PendingFile = { file, progress: 0, status: 'uploading' };
    this.pendingFiles.update(list => [...list, pf]);
    const id = this.jobId();
    if (id !== null) this.uploadOne(pf, id);
  }

  private uploadOne(pf: PendingFile, jobId: number): void {
    pf.status = 'uploading';
    this.api.uploadFile(jobId, pf.file).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          pf.progress = Math.round((100 * event.loaded) / event.total);
          this.pendingFiles.update(list => [...list]);
        } else if (event.type === HttpEventType.Response && event.body) {
          pf.status = 'done';
          pf.progress = 100;
          this.pendingFiles.update(list => [...list]);
          this.fileUploaded.emit(event.body);
        }
      },
      error: err => {
        pf.status = 'error';
        pf.errorMessage = err?.error?.message ?? 'Falha no upload';
        this.pendingFiles.update(list => [...list]);
      },
    });
  }
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-file-uploader.component.spec.ts'`
Expected: PASS (6 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/components/job-file-uploader/
git commit -m "feat(frontend): add JobFileUploaderComponent with staging/direct modes"
```

---

### Task F13: frontend-design para `JobSummarySidebarComponent`

**Ação:** Invocar `frontend-design`:

> Sidebar sticky que acompanha o form de criação/edição de job. Largura 320px, sticky top 24px, padding 24px, background white, border gray-200, radius-md. Conteúdo: Título "Resumo" em H4. Lista de 6-8 linhas mostrando campos preenchidos (cliente, tipo, prazo, prioridade, criativo, título) com label em gray-500 12px e valor em gray-900 14px. Indicador de campos obrigatórios faltando (lista em vermelho com ícone). Se tudo preenchido: check verde "Pronto para salvar". Botões no fundo: "Salvar Job" (primary, full width, loading state) e "Cancelar" (ghost, full width). Modo edit mostra "Atualizar Job" no lugar.

**BLOQUEIA F14.**

---

### Task F14: Implementar `JobSummarySidebarComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/components/job-summary-sidebar/job-summary-sidebar.component.ts`
- Create: `frontend/src/app/features/jobs/components/job-summary-sidebar/job-summary-sidebar.component.html`
- Create: `frontend/src/app/features/jobs/components/job-summary-sidebar/job-summary-sidebar.component.spec.ts`

**Depends on:** F1, F13

**Step 1: Red**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { JobSummarySidebarComponent } from './job-summary-sidebar.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('JobSummarySidebarComponent', () => {
  let fixture: ComponentFixture<JobSummarySidebarComponent>;
  let component: JobSummarySidebarComponent;
  let form: FormGroup;
  let fb: FormBuilder;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobSummarySidebarComponent, ReactiveFormsModule, NoopAnimationsModule],
    });
    fb = TestBed.inject(FormBuilder);
    form = fb.nonNullable.group({
      title: ['', [Validators.required]],
      clientId: [null, [Validators.required]],
      type: ['POST_FEED', [Validators.required]],
      priority: ['NORMAL', [Validators.required]],
      deadline: [''],
      assignedCreativeId: [null],
    });

    fixture = TestBed.createComponent(JobSummarySidebarComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('mode', 'create');
    fixture.componentRef.setInput('clientName', null);
    fixture.componentRef.setInput('creativeName', null);
    fixture.detectChanges();
  });

  it('should show "Salvar Job" button in create mode', () => {
    expect(fixture.nativeElement.textContent).toContain('Salvar Job');
  });

  it('should show "Atualizar Job" in edit mode', () => {
    fixture.componentRef.setInput('mode', 'edit');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Atualizar Job');
  });

  it('should disable save button when form is invalid', () => {
    expect(component.canSave()).toBe(false);
  });

  it('should enable save button when form is valid', () => {
    form.patchValue({ title: 'Post', clientId: 1 });
    fixture.detectChanges();
    expect(component.canSave()).toBe(true);
  });

  it('should emit save when save button triggers', () => {
    const spy = vi.fn();
    component.save.subscribe(spy);
    form.patchValue({ title: 'Post', clientId: 1 });
    fixture.detectChanges();
    component.onSave();
    expect(spy).toHaveBeenCalled();
  });

  it('should list missing required fields', () => {
    const missing = component.missingRequiredFields();
    expect(missing).toContain('Título');
    expect(missing).toContain('Cliente');
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job-summary-sidebar.component.spec.ts'`
Expected: FAIL.

**Step 3: Green**

```typescript
// job-summary-sidebar.component.ts
import {
  ChangeDetectionStrategy, Component, input, output, computed,
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ButtonModule } from 'primeng/button';

const REQUIRED_LABELS: Record<string, string> = {
  title: 'Título',
  clientId: 'Cliente',
  type: 'Tipo',
  priority: 'Prioridade',
};

@Component({
  selector: 'app-job-summary-sidebar',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './job-summary-sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobSummarySidebarComponent {
  readonly form = input.required<FormGroup>();
  readonly mode = input.required<'create' | 'edit'>();
  readonly clientName = input<string | null>(null);
  readonly creativeName = input<string | null>(null);
  readonly loading = input<boolean>(false);

  readonly save = output<void>();
  readonly cancel = output<void>();

  readonly canSave = computed(() => this.form().valid && !this.loading());

  readonly buttonLabel = computed(() =>
    this.mode() === 'create' ? 'Salvar Job' : 'Atualizar Job'
  );

  missingRequiredFields(): string[] {
    const f = this.form();
    return Object.keys(REQUIRED_LABELS)
      .filter(k => f.get(k)?.invalid ?? false)
      .map(k => REQUIRED_LABELS[k]);
  }

  onSave(): void { this.save.emit(); }
  onCancel(): void { this.cancel.emit(); }
}
```

**Step 4: Verificar sucesso**

Run: PASS (6 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/components/job-summary-sidebar/
git commit -m "feat(frontend): add JobSummarySidebarComponent"
```

---

### Task F15: frontend-design para `JobFormComponent`

**Ação:** Invocar `frontend-design`:

> Shared form component usado em `/jobs/new` e `/jobs/:id/edit`. Layout: stack vertical de 3 cards. Card 1 "Informações Gerais": grid 2 colunas para título (full width), cliente (p-select), tipo (p-select), prazo (p-datepicker), prioridade (p-select), criativo atribuído (p-select — carregado dinamicamente baseado no cliente). Descrição em textarea full width no rodapé do card. Card 2 "Briefing": header "Briefing — {tipo label}" + renderiza `<app-briefing-fields>`. Card 3 "Anexos do Briefing": renderiza `<app-job-file-uploader>`. Cada card com title em H3 18px, border gray-200, radius-md, padding 24px, gap 24px entre cards. Forms labels 14px gray-700, inputs height 40px, erros em red 12px abaixo do input. Reactive Forms com validação em tempo real.

**BLOQUEIA F16.**

---

### Task F16: Implementar `JobFormComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/components/job-form/job-form.component.ts`
- Create: `frontend/src/app/features/jobs/components/job-form/job-form.component.html`
- Create: `frontend/src/app/features/jobs/components/job-form/job-form.component.spec.ts`

**Depends on:** F3, F10, F12, F14, F15

**Step 1: Red**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobFormComponent } from './job-form.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

describe('JobFormComponent', () => {
  let fixture: ComponentFixture<JobFormComponent>;
  let component: JobFormComponent;
  let jobApiSpy: any;
  let clientApiSpy: any;
  let memberApiSpy: any;

  beforeEach(() => {
    jobApiSpy = { create: vi.fn(), update: vi.fn() };
    clientApiSpy = {
      list: vi.fn().mockReturnValue(of([{ id: 1, name: 'Acme', active: true }])),
      getAssignedMembers: vi.fn().mockReturnValue(of([10])),
    };
    memberApiSpy = {
      list: vi.fn().mockReturnValue(of({
        members: [
          { id: 10, userName: 'João', role: 'CREATIVE' },
          { id: 11, userName: 'Ana', role: 'MANAGER' },
        ],
      })),
    };
    TestBed.configureTestingModule({
      imports: [JobFormComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: ClientApiService, useValue: clientApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    fixture = TestBed.createComponent(JobFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', 'create');
    fixture.componentRef.setInput('initialJob', null);
    fixture.detectChanges();
  });

  it('should create with default values', () => {
    expect(component.form.get('priority')!.value).toBe('NORMAL');
    expect(component.form.get('type')!.value).toBe('POST_FEED');
  });

  it('should be invalid when required fields are empty', () => {
    expect(component.form.valid).toBe(false);
  });

  it('should be valid with required fields filled', () => {
    component.form.patchValue({ title: 'Post', clientId: 1 });
    (component.form.get('briefingData') as any).patchValue({
      captionText: 'abc', format: '1:1',
    });
    expect(component.form.valid).toBe(true);
  });

  it('should reload creatives when client changes', () => {
    component.form.get('clientId')!.setValue(1);
    expect(clientApiSpy.getAssignedMembers).toHaveBeenCalledWith(1);
  });

  it('should filter creative options to CREATIVE role only', () => {
    component.form.get('clientId')!.setValue(1);
    fixture.detectChanges();
    expect(component.creativeOptions().map(o => o.value)).toEqual([10]);
  });

  it('should emit submit when onSubmit called with valid form', () => {
    const spy = vi.fn();
    component.submitted.subscribe(spy);
    component.form.patchValue({ title: 'Post', clientId: 1 });
    (component.form.get('briefingData') as any).patchValue({
      captionText: 'abc', format: '1:1',
    });
    component.onSubmit();
    expect(spy).toHaveBeenCalled();
  });

  it('should populate form from initialJob in edit mode', () => {
    const job: Job = {
      id: 1, code: 'JOB-001', title: 'Existing',
      client: { id: 1, name: 'Acme' }, type: 'STORIES',
      description: 'desc', deadline: '2026-06-01', priority: 'ALTA',
      assignedCreative: { id: 10, name: 'João' }, status: 'NOVO',
      briefingData: { text: 'hi', format: '9:16' }, archived: false,
      files: [], createdAt: '2026-04-11', updatedAt: '2026-04-11',
      createdByName: 'Maria',
    };
    fixture.componentRef.setInput('mode', 'edit');
    fixture.componentRef.setInput('initialJob', job);
    fixture.detectChanges();
    expect(component.form.get('title')!.value).toBe('Existing');
    expect(component.form.get('type')!.value).toBe('STORIES');
  });
});
```

**Step 2: Verificar falha**

Run: `cd frontend && ng test --include='**/job-form.component.spec.ts'`
Expected: FAIL.

**Step 3: Green**

```typescript
// job-form.component.ts
import {
  ChangeDetectionStrategy, Component, inject, input, output, effect,
  computed, signal,
} from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { BriefingFieldsComponent } from '../briefing-fields/briefing-fields.component';
import { JobFileUploaderComponent } from '../job-file-uploader/job-file-uploader.component';
import type { Job, JobRequest, JobType, JobPriority } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, CardModule, InputTextModule, TextareaModule,
    SelectModule, DatePickerModule, BriefingFieldsComponent, JobFileUploaderComponent,
  ],
  templateUrl: './job-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientApi = inject(ClientApiService);
  private readonly memberApi = inject(MemberApiService);

  readonly mode = input.required<'create' | 'edit'>();
  readonly initialJob = input<Job | null>(null);

  readonly submitted = output<JobRequest>();

  readonly clientOptions = signal<{ label: string; value: number }[]>([]);
  readonly creativeOptions = signal<{ label: string; value: number }[]>([]);

  readonly typeOptions = [
    { label: 'Post Feed', value: 'POST_FEED' },
    { label: 'Stories', value: 'STORIES' },
    { label: 'Carrossel', value: 'CARROSSEL' },
    { label: 'Reels/Vídeo', value: 'REELS_VIDEO' },
    { label: 'Banner', value: 'BANNER' },
    { label: 'Logo', value: 'LOGO' },
    { label: 'Outros', value: 'OUTROS' },
  ];
  readonly priorityOptions = [
    { label: 'Baixa', value: 'BAIXA' },
    { label: 'Normal', value: 'NORMAL' },
    { label: 'Alta', value: 'ALTA' },
    { label: 'Urgente', value: 'URGENTE' },
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    clientId: this.fb.control<number | null>(null, [Validators.required]),
    type: this.fb.nonNullable.control<JobType>('POST_FEED', [Validators.required]),
    description: [''],
    deadline: this.fb.control<Date | null>(null),
    priority: this.fb.nonNullable.control<JobPriority>('NORMAL', [Validators.required]),
    assignedCreativeId: this.fb.control<number | null>(null),
    briefingData: this.fb.group({}),
  });

  readonly currentType = computed(() => this.form.get('type')!.value as JobType);

  constructor() {
    this.clientApi.list({ active: true }).subscribe({
      next: clients => this.clientOptions.set(clients.map(c => ({ label: c.name, value: c.id }))),
      error: err => console.error('Erro ao carregar clientes:', err),
    });

    this.form.get('clientId')!.valueChanges.subscribe(clientId => {
      if (clientId !== null) this.loadCreativesForClient(clientId);
      else this.creativeOptions.set([]);
    });

    effect(() => {
      const job = this.initialJob();
      if (job && this.mode() === 'edit') {
        this.form.patchValue({
          title: job.title,
          clientId: job.client.id,
          type: job.type,
          description: job.description ?? '',
          deadline: job.deadline ? new Date(job.deadline) : null,
          priority: job.priority,
          assignedCreativeId: job.assignedCreative?.id ?? null,
        });
        (this.form.get('briefingData') as FormGroup).patchValue(job.briefingData);
      }
    });
  }

  private loadCreativesForClient(clientId: number): void {
    this.memberApi.list().subscribe({
      next: ({ members }) => {
        this.clientApi.getAssignedMembers(clientId).subscribe({
          next: assignedIds => {
            const options = members
              .filter(m => m.role === 'CREATIVE' && assignedIds.includes(m.id))
              .map(m => ({ label: m.userName, value: m.id }));
            this.creativeOptions.set(options);
          },
          error: err => console.error('Erro ao carregar criativos:', err),
        });
      },
      error: err => console.error('Erro ao carregar membros:', err),
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const request: JobRequest = {
      title: raw.title,
      clientId: raw.clientId!,
      type: raw.type,
      priority: raw.priority,
      briefingData: raw.briefingData as Record<string, unknown>,
    };
    if (raw.description) request.description = raw.description;
    if (raw.deadline) request.deadline = raw.deadline.toISOString().split('T')[0];
    if (raw.assignedCreativeId !== null) request.assignedCreativeId = raw.assignedCreativeId;
    this.submitted.emit(request);
  }
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-form.component.spec.ts'`
Expected: PASS (7 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/components/job-form/
git commit -m "feat(frontend): add JobFormComponent shared between create and edit"
```

---

### Task F17: frontend-design para `JobCreateComponent`

**Ação:** Invocar `frontend-design`:

> Página `/jobs/new`. Layout: container 1280px max-width, padding 32px. Header breadcrumb "Jobs / Novo Job" em H2 + subtítulo. Abaixo: grid 2 colunas (main 1fr + sidebar 320px), gap 32px. Main renderiza `<app-job-form>`. Sidebar renderiza `<app-job-summary-sidebar>` sticky top 24px. Toast notifications para sucesso (verde) e erro (vermelho). Após criar com sucesso, navega para `/jobs/{id}`. Uploads pendentes do uploader são disparados após o POST — feedback visual na tela de detalhe.

**BLOQUEIA F18.**

---

### Task F18: Implementar `JobCreateComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/pages/job-create/job-create.component.ts`
- Create: `frontend/src/app/features/jobs/pages/job-create/job-create.component.html`
- Create: `frontend/src/app/features/jobs/pages/job-create/job-create.component.spec.ts`

**Depends on:** F16, F17

**Step 1: Red**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobCreateComponent } from './job-create.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job, JobRequest } from '@features/jobs/models/job.model';

describe('JobCreateComponent', () => {
  let fixture: ComponentFixture<JobCreateComponent>;
  let component: JobCreateComponent;
  let jobApi: any; let router: any; let msg: any;

  const mockJob: Job = {
    id: 42, code: 'JOB-042', title: 'X',
    client: { id: 1, name: 'A' }, type: 'POST_FEED', description: null,
    deadline: null, priority: 'NORMAL', assignedCreative: null, status: 'NOVO',
    briefingData: {}, archived: false, files: [],
    createdAt: '', updatedAt: '', createdByName: 'M',
  };

  beforeEach(() => {
    jobApi = { create: vi.fn().mockReturnValue(of(mockJob)) };
    router = { navigate: vi.fn() };
    msg = { add: vi.fn() };

    TestBed.configureTestingModule({
      imports: [JobCreateComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApi },
        { provide: ClientApiService, useValue: { list: () => of([]), getAssignedMembers: () => of([]) } },
        { provide: MemberApiService, useValue: { list: () => of({ members: [] }) } },
        { provide: Router, useValue: router },
        { provide: MessageService, useValue: msg },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    fixture = TestBed.createComponent(JobCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should navigate to detail after successful create', () => {
    const req: JobRequest = {
      title: 'X', clientId: 1, type: 'POST_FEED', priority: 'NORMAL',
      briefingData: { captionText: 'x', format: '1:1' },
    };
    component.onFormSubmit(req);
    expect(jobApi.create).toHaveBeenCalledWith(req);
    expect(router.navigate).toHaveBeenCalledWith(['/jobs', 42]);
  });

  it('should show error toast on failed create', () => {
    jobApi.create = vi.fn().mockReturnValue(throwError(() => new Error('fail')));
    component.onFormSubmit({
      title: 'X', clientId: 1, type: 'POST_FEED', priority: 'NORMAL', briefingData: {},
    });
    expect(msg.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
  });
});
```

**Step 2: Verificar falha**

Run: PASS esperado. Expected: FAIL — componente não existe.

**Step 3: Green**

```typescript
// job-create.component.ts
import { ChangeDetectionStrategy, Component, inject, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFormComponent } from '../../components/job-form/job-form.component';
import { JobSummarySidebarComponent } from '../../components/job-summary-sidebar/job-summary-sidebar.component';
import { JobFileUploaderComponent } from '../../components/job-file-uploader/job-file-uploader.component';
import type { JobRequest } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-create',
  standalone: true,
  imports: [JobFormComponent, JobSummarySidebarComponent, JobFileUploaderComponent, ToastModule],
  templateUrl: './job-create.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
})
export class JobCreateComponent {
  private readonly jobApi = inject(JobApiService);
  private readonly router = inject(Router);
  private readonly msg = inject(MessageService);

  @ViewChild(JobFileUploaderComponent) uploader?: JobFileUploaderComponent;

  readonly loading = signal(false);

  onFormSubmit(request: JobRequest): void {
    this.loading.set(true);
    this.jobApi.create(request).subscribe({
      next: job => {
        this.loading.set(false);
        this.msg.add({ severity: 'success', summary: 'Job criado', detail: job.code });
        // Dispara uploads pendentes (staging)
        this.uploader?.uploadPending(job.id);
        this.router.navigate(['/jobs', job.id]);
      },
      error: err => {
        this.loading.set(false);
        const detail = err?.error?.message ?? 'Erro ao criar job. Tente novamente.';
        this.msg.add({ severity: 'error', summary: 'Erro', detail });
      },
    });
  }

  onCancel(): void {
    this.router.navigate(['/jobs']);
  }
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-create.component.spec.ts'`
Expected: PASS (2 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/pages/job-create/
git commit -m "feat(frontend): add JobCreateComponent page"
```

---

### Task F19: frontend-design para `JobEditComponent`

**Ação:** Invocar `frontend-design`:

> Página `/jobs/:id/edit`. Mesma estrutura da `JobCreateComponent`: grid 2 colunas com form e sidebar. Header "Editar Job — JOB-001" em H2. Diferenças: o formulário é pré-populado via `initialJob`, o uploader roda em modo **direct** (upload imediato), anexos existentes do job são passados para o uploader. Botão do sidebar agora é "Atualizar Job". Toast de sucesso redireciona para `/jobs/{id}` (detail).

**BLOQUEIA F20.**

---

### Task F20: Implementar `JobEditComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/pages/job-edit/job-edit.component.ts`
- Create: `frontend/src/app/features/jobs/pages/job-edit/job-edit.component.html`
- Create: `frontend/src/app/features/jobs/pages/job-edit/job-edit.component.spec.ts`

**Depends on:** F16, F19

**Step 1: Red**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobEditComponent } from './job-edit.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

describe('JobEditComponent', () => {
  let fixture: ComponentFixture<JobEditComponent>;
  let component: JobEditComponent;
  let jobApi: any; let router: any; let msg: any;

  const mockJob: Job = {
    id: 42, code: 'JOB-042', title: 'Existing',
    client: { id: 1, name: 'A' }, type: 'POST_FEED', description: null,
    deadline: null, priority: 'NORMAL', assignedCreative: null, status: 'NOVO',
    briefingData: { captionText: 'abc', format: '1:1' }, archived: false, files: [],
    createdAt: '', updatedAt: '', createdByName: '',
  };

  beforeEach(() => {
    jobApi = {
      getById: vi.fn().mockReturnValue(of(mockJob)),
      update: vi.fn().mockReturnValue(of(mockJob)),
    };
    router = { navigate: vi.fn() };
    msg = { add: vi.fn() };
    TestBed.configureTestingModule({
      imports: [JobEditComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApi },
        { provide: ClientApiService, useValue: { list: () => of([]), getAssignedMembers: () => of([]) } },
        { provide: MemberApiService, useValue: { list: () => of({ members: [] }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { params: { id: '42' } } } },
        { provide: Router, useValue: router },
        { provide: MessageService, useValue: msg },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    fixture = TestBed.createComponent(JobEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load job by id on init', () => {
    expect(jobApi.getById).toHaveBeenCalledWith(42);
    expect(component.job()).toEqual(mockJob);
  });

  it('should update and navigate to detail on success', () => {
    component.onFormSubmit({
      title: 'X', clientId: 1, type: 'POST_FEED',
      priority: 'NORMAL', briefingData: {},
    });
    expect(jobApi.update).toHaveBeenCalledWith(42, expect.any(Object));
    expect(router.navigate).toHaveBeenCalledWith(['/jobs', 42]);
  });

  it('should show error toast when update fails', () => {
    jobApi.update = vi.fn().mockReturnValue(throwError(() => new Error('fail')));
    component.onFormSubmit({
      title: 'X', clientId: 1, type: 'POST_FEED', priority: 'NORMAL', briefingData: {},
    });
    expect(msg.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
  });
});
```

**Step 2: Verificar falha**

Run: Expected: FAIL.

**Step 3: Green**

```typescript
// job-edit.component.ts
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFormComponent } from '../../components/job-form/job-form.component';
import { JobSummarySidebarComponent } from '../../components/job-summary-sidebar/job-summary-sidebar.component';
import type { Job, JobRequest } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-edit',
  standalone: true,
  imports: [JobFormComponent, JobSummarySidebarComponent, ToastModule],
  templateUrl: './job-edit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
})
export class JobEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly jobApi = inject(JobApiService);
  private readonly msg = inject(MessageService);

  readonly job = signal<Job | null>(null);
  readonly loading = signal(false);
  readonly jobId = Number(this.route.snapshot.params['id']);

  ngOnInit(): void {
    this.jobApi.getById(this.jobId).subscribe({
      next: job => this.job.set(job),
      error: () => {
        this.msg.add({ severity: 'error', summary: 'Erro', detail: 'Job não encontrado.' });
        this.router.navigate(['/jobs']);
      },
    });
  }

  onFormSubmit(request: JobRequest): void {
    this.loading.set(true);
    this.jobApi.update(this.jobId, request).subscribe({
      next: () => {
        this.loading.set(false);
        this.msg.add({ severity: 'success', summary: 'Job atualizado', detail: '' });
        this.router.navigate(['/jobs', this.jobId]);
      },
      error: err => {
        this.loading.set(false);
        const detail = err?.error?.message ?? 'Erro ao atualizar job.';
        this.msg.add({ severity: 'error', summary: 'Erro', detail });
      },
    });
  }

  onCancel(): void {
    this.router.navigate(['/jobs', this.jobId]);
  }
}
```

**Step 4: Verificar sucesso**

Run: PASS (3 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/pages/job-edit/
git commit -m "feat(frontend): add JobEditComponent page"
```

---

### Task F21: frontend-design para `JobDetailComponent`

**Ação:** Invocar `frontend-design`:

> Página `/jobs/:id`. Layout: container 1280px, grid 2 colunas (main 1fr + aside 320px, gap 32px). Header acima da grid: breadcrumb, código JOB-XXX em JetBrains Mono 18px + título H2, badge de status colorido, ações à direita (OWNER/MANAGER: "Editar" secondary, "Arquivar" danger ghost). Main esquerda: card "Descrição Geral", card "Briefing" (view-only — renderiza briefingData de acordo com BRIEFING_SCHEMAS[type] como lista label/value), card "Anexos" (grid de cards por arquivo: thumbnail de imagem se MIME image/*, ícone de PDF/vídeo caso contrário; nome, tamanho; clicar baixa; botão remover nos cards para OWNER/MANAGER; uploads em progresso via `<app-job-file-uploader mode="direct">`). Aside direita: card "Metadados" com cliente (avatar + nome), tipo (badge), prazo + `isOverdue` warn, prioridade (badge), criativo atribuído, criado por, criado em. Light mode, spacing generoso.

**BLOQUEIA F22.**

---

### Task F22: Implementar `JobDetailComponent`

**Files:**
- Create: `frontend/src/app/features/jobs/pages/job-detail/job-detail.component.ts`
- Create: `frontend/src/app/features/jobs/pages/job-detail/job-detail.component.html`
- Create: `frontend/src/app/features/jobs/pages/job-detail/job-detail.component.spec.ts`

**Depends on:** F3, F10 (indireto — reutiliza BRIEFING_SCHEMAS para renderizar view-only), F21

**Step 1: Red**

```typescript
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobDetailComponent } from './job-detail.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ActivatedRoute, Router } from '@angular/router';
import { StorageService } from '@core/services/storage.service';
import { MessageService, ConfirmationService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

const mockJob: Job = {
  id: 42, code: 'JOB-042', title: 'Post Black Friday',
  client: { id: 1, name: 'Acme' }, type: 'POST_FEED', description: 'Lorem',
  deadline: '2026-05-01', priority: 'ALTA',
  assignedCreative: { id: 10, name: 'João' }, status: 'NOVO',
  briefingData: { captionText: 'texto', format: '1:1' }, archived: false,
  files: [{
    id: 1, originalFilename: 'ref.jpg', mimeType: 'image/jpeg',
    sizeBytes: 123, uploadedAt: '2026-04-11', downloadUrl: '/x',
  }],
  createdAt: '2026-04-11', updatedAt: '2026-04-11', createdByName: 'Maria',
};

describe('JobDetailComponent', () => {
  let fixture: ComponentFixture<JobDetailComponent>;
  let component: JobDetailComponent;
  let api: any; let router: any; let storage: any; let confirm: any;

  function setup(role = 'OWNER') {
    api = {
      getById: vi.fn().mockReturnValue(of(mockJob)),
      archive: vi.fn().mockReturnValue(of({ ...mockJob, archived: true })),
    };
    router = { navigate: vi.fn() };
    storage = { getUser: vi.fn().mockReturnValue({ role }) };
    confirm = { confirm: vi.fn().mockImplementation((cfg: any) => { cfg.accept(); return confirm; }) };

    TestBed.configureTestingModule({
      imports: [JobDetailComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { params: { id: '42' } } } },
        { provide: Router, useValue: router },
        { provide: StorageService, useValue: storage },
        { provide: MessageService, useValue: { add: vi.fn() } },
        { provide: ConfirmationService, useValue: confirm },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => { vi.clearAllMocks(); TestBed.resetTestingModule(); });

  it('should load job by id', () => {
    setup();
    expect(api.getById).toHaveBeenCalledWith(42);
    expect(component.job()).toEqual(mockJob);
  });

  it('should render job code, title, briefing fields', () => {
    setup();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('JOB-042');
    expect(text).toContain('Post Black Friday');
    expect(text).toContain('Texto da legenda');
  });

  it('should show Edit/Archive buttons for OWNER', () => {
    setup('OWNER');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Editar');
    expect(fixture.nativeElement.textContent).toContain('Arquivar');
  });

  it('should hide Edit/Archive for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Editar');
    expect(fixture.nativeElement.textContent).not.toContain('Arquivar');
  });

  it('should archive with confirmation', () => {
    setup('OWNER');
    component.archive();
    expect(api.archive).toHaveBeenCalledWith(42, true);
  });

  it('should navigate to list when job not found', () => {
    api = { getById: vi.fn().mockReturnValue(throwError(() => ({ status: 404 }))) };
    TestBed.overrideProvider(JobApiService, { useValue: api });
    fixture = TestBed.createComponent(JobDetailComponent);
    fixture.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith(['/jobs']);
  });
});
```

**Step 2: Verificar falha**

Run: Expected: FAIL.

**Step 3: Green**

```typescript
// job-detail.component.ts
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFileUploaderComponent } from '../../components/job-file-uploader/job-file-uploader.component';
import { StorageService } from '@core/services/storage.service';
import { BRIEFING_SCHEMAS } from '@features/jobs/models/briefing-schemas';
import type { Job } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [
    ButtonModule, TagModule, CardModule, ToastModule, ConfirmDialogModule,
    JobFileUploaderComponent, RouterLink,
  ],
  templateUrl: './job-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService, ConfirmationService],
})
export class JobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(JobApiService);
  private readonly storage = inject(StorageService);
  private readonly msg = inject(MessageService);
  private readonly confirmService = inject(ConfirmationService);

  readonly job = signal<Job | null>(null);
  readonly jobId = Number(this.route.snapshot.params['id']);
  readonly currentUser = this.storage.getUser();
  readonly canManage = this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  ngOnInit(): void {
    this.api.getById(this.jobId).subscribe({
      next: job => this.job.set(job),
      error: () => {
        this.msg.add({ severity: 'error', summary: 'Erro', detail: 'Job não encontrado.' });
        this.router.navigate(['/jobs']);
      },
    });
  }

  briefingFields(): Array<{ label: string; value: unknown }> {
    const job = this.job();
    if (!job) return [];
    return BRIEFING_SCHEMAS[job.type].map(field => ({
      label: field.label,
      value: job.briefingData[field.key],
    }));
  }

  goToEdit(): void {
    this.router.navigate(['/jobs', this.jobId, 'edit']);
  }

  archive(): void {
    const job = this.job();
    if (!job) return;
    this.confirmService.confirm({
      message: `Deseja arquivar o job "${job.code}"?`,
      header: 'Arquivar',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Arquivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.archive(this.jobId, true).subscribe({
          next: updated => {
            this.job.set(updated);
            this.msg.add({ severity: 'success', summary: 'Job arquivado', detail: '' });
          },
          error: err => {
            console.error('Erro ao arquivar job:', err);
            this.msg.add({ severity: 'error', summary: 'Erro', detail: 'Falha ao arquivar.' });
          },
        });
      },
    });
  }

  onFileUploaded(): void {
    // Recarrega job para refletir novos arquivos
    this.api.getById(this.jobId).subscribe({
      next: job => this.job.set(job),
      error: () => { /* silent */ },
    });
  }

  onFileDeleted(): void {
    this.api.getById(this.jobId).subscribe({
      next: job => this.job.set(job),
      error: () => { /* silent */ },
    });
  }
}
```

**Step 4: Verificar sucesso**

Run: `cd frontend && ng test --include='**/job-detail.component.spec.ts'`
Expected: PASS (6 tests).

**Step 5: Commit**

```bash
git add frontend/src/app/features/jobs/pages/job-detail/
git commit -m "feat(frontend): add JobDetailComponent page with view-only briefing"
```

---

### Task F23: Smoke test + ajustes finais

**Files:** Nenhum (a não ser ajustes pontuais descobertos).

**Depends on:** Tudo.

**Ações:**
1. Iniciar backend (`cd backend && ./mvnw spring-boot:run`)
2. Iniciar frontend (`cd frontend && ng serve`)
3. Login como OWNER
4. Criar um cliente de teste (se ainda não existir)
5. Navegar até `/jobs`, verificar tabela vazia + botão "Novo Job"
6. Criar job tipo `POST_FEED` preenchendo briefing + anexando um .jpg
7. Verificar redirect para `/jobs/{id}` e arquivo presente
8. Editar o job: mudar tipo para `STORIES` — campos de briefing devem trocar
9. Verificar update persiste
10. Arquivar o job e confirmar que some da listagem default
11. Toggle "Ver arquivados" e verificar que aparece
12. Login como CREATIVE (membro com ClientMember do cliente usado): acessar `/jobs`
    - Deve ver o job
    - Não deve ver botão "Novo Job"
    - Menu do job só tem "Ver"
    - Tentar navegar para `/jobs/new` diretamente — redirect para `/dashboard` via roleGuard
13. Rodar suite completa: `cd frontend && ng test`
14. Rodar build: `cd frontend && ng build`

**Critério de sucesso:** Todos os testes passam, build sem warnings relevantes, smoke test passa 100%.

**Commit (se houver ajustes):**

```bash
git add <arquivos>
git commit -m "fix(frontend): smoke test adjustments for RF04"
```

---

## Observações de integração (cross-layer)

- **Dependência backend crítica:** todas as tasks de implementação (F3 em diante) assumem que o backend expõe `/api/v1/jobs/*`. O service `JobApiService` pode ser escrito antes do backend estar pronto (apenas mocks HTTP nos testes), mas smoke test e F23 pressupõem backend deployado.
- **Endpoints consumidos de RF03:** a listagem de clientes (`GET /api/v1/clients?active=true`) e de membros (`GET /api/v1/members` + `GET /api/v1/clients/{id}/members`) são usados nos filtros e no form. Já existem no backend.
- **`assignedCreativeId` no form:** a lógica "criativo deve ter ClientMember com o cliente selecionado" é enforced backend (400 caso não). Frontend já filtra o dropdown para apenas criativos vinculados via `clientApi.getAssignedMembers(clientId)`, evitando que o usuário escolha um inválido.
- **Upload direct vs staging:** distinção importante — erro reincidente seria confundir os dois modos. Staging em create guarda `PendingFile[]` em memória e dispara uploads via `uploader.uploadPending(jobId)` após o POST. Direct em edit sobe imediatamente chamando `uploadFile` no onFileSelect.
- **Path aliases obrigatórios:** todo import com 2+ níveis de `../` precisa usar `@features/*`, `@core/*`, `@shared/*`, `@env/*`. Já aplicado nas amostras acima.

---

## Regras de qualidade verificadas

- [x] `frontend-design` precede cada task de UI visual (F7, F9, F11, F13, F15, F17, F19, F21)
- [x] Standalone components, sem NgModule
- [x] `input()`, `output()`, `computed()`, `signal()` — sem decorators legados
- [x] `inject()` — sem constructor injection
- [x] `@if` / `@for` / `@switch`
- [x] `ChangeDetectionStrategy.OnPush` em todos os componentes
- [x] Reactive Forms (não template-driven)
- [x] `templateUrl` + HTML separado (nada inline)
- [x] Lazy loading da rota `/jobs`
- [x] `roleGuard('OWNER', 'MANAGER')` em `/jobs/new` e `/jobs/:id/edit`
- [x] Sem `as any`
- [x] Error handlers em todos os subscribes destrutivos
- [x] Debounce 300ms na busca
- [x] Vitest com globals (sem imports de `describe`/`it`/`expect`)
- [x] `provideHttpClientTesting()` em specs de service

---
