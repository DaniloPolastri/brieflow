# RF05 — Kanban de Produção: Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Criar branch `feature/rf05-kanban` a partir de `main` (DEPOIS de mergear RF04 PR #7). Marcar `[x]` no checklist abaixo **imediatamente** ao completar cada task e fazer commit.

**Goal:** Transformar a listagem de jobs (p-table do RF04) em um kanban board visual com drag-drop entre 6 colunas fixas de status, atualização em tempo real via SSE, toggle lista/kanban, e filtro "meus jobs" pra criativos.

**Architecture:** Backend adiciona 2 endpoints (PATCH status + GET SSE stream) ao JobController/JobSseController existente, com `JobSseService` gerenciando SseEmitters por clientId e heartbeat via @Scheduled. Frontend usa Angular CDK DragDrop (nova dependência) pra drag-drop entre 6 colunas hardcoded do JobStatus enum, com optimistic update + rollback + SSE integration via EventSource nativo.

**Tech Stack:** Java 21 + Spring Boot 3 + SseEmitter + @Scheduled | Angular 20 + @angular/cdk (DragDrop) + PrimeNG 19 + Tailwind v4 + Vitest + EventSource API

**Spec base:** `docs/spec/2026-04-12-rf05-kanban-producao-design.md`

**Pre-requisito:** RF04 (PR #7) deve estar mergeado em main antes de criar o feature branch.

---

## Task Summary

### Backend (B1–B8)

- [x] Task B1: DTOs (UpdateJobStatusDTO, JobStatusResponseDTO, JobStatusEvent)
- [x] Task B2: JobSseService (SSE emitter manager + @Scheduled heartbeat) ⚡ PARALLEL GROUP A
- [x] Task B3: @EnableScheduling + SecurityConfig changes ⚡ PARALLEL GROUP A
- [x] Task B4: updateJobStatus in JobService/JobServiceImpl (depends on B1)
- [x] Task B5: PATCH endpoint in JobController (depends on B4)
- [x] Task B6: JobSseController — SSE stream endpoint (depends on B2, B3)
- [x] Task B7: Unit tests — JobServiceImplTest + JobSseServiceTest (depends on B4, B2)
- [x] Task B8: Integration tests — JobControllerTest + JobSseControllerTest (depends on B5, B6)

### Frontend (F1–F12)

- [x] Task F1: Install @angular/cdk + add RF05 types to job.model.ts
- [x] Task F2: JobSseService — EventSource wrapper with Observable ⚡ PARALLEL GROUP B
- [x] Task F3: JobApiService.updateStatus() method ⚡ PARALLEL GROUP B
- [ ] Task F4: frontend-design for KanbanCardComponent
- [ ] Task F5: KanbanCardComponent implementation (TDD) — depends on F1
- [ ] Task F6: frontend-design for KanbanColumnComponent
- [ ] Task F7: KanbanColumnComponent implementation (TDD) — depends on F5
- [ ] Task F8: frontend-design for KanbanBoardComponent
- [ ] Task F9: KanbanBoardComponent implementation — drag-drop + SSE (TDD) — depends on F2, F3, F7, **B5 backend**
- [ ] Task F10: frontend-design for JobListComponent toggle (lista/kanban + meus jobs)
- [ ] Task F11: JobListComponent modification — view toggle + kanban integration (TDD) — depends on F9
- [ ] Task F12: Smoke test — build + all tests green — depends on B8, F11

---

## Grupos Paralelos

| Grupo | Tasks | Condição |
|---|---|---|
| `⚡ PARALLEL GROUP A` | B2, B3 | Backend infra — SSE service e config são independentes |
| `⚡ PARALLEL GROUP B` | F2, F3 | Frontend services — SSE wrapper e API method são independentes |

**Paralelismo cross-layer:** B1 (DTOs) e F1 (models + CDK install) podem rodar simultaneamente. F2+F3 podem rodar em paralelo com B2+B3. F4-F7 (card + column components) podem adiantar enquanto B4-B6 ainda estão em andamento. F9 (KanbanBoard) é o primeiro ponto de bloqueio cross-layer (precisa de B5 pra PATCH funcionar de verdade).

---

## Ordem de Execução (Fases)

### Fase 1 — Foundation (paralela cross-layer)
- Backend: B1 (DTOs) + B2 (SseService) + B3 (@EnableScheduling) ⚡
- Frontend: F1 (CDK + types) + F2 (SseService) + F3 (ApiService.updateStatus) ⚡

### Fase 2 — Backend core
- B4 (updateJobStatus service) → B5 (PATCH controller) → B6 (SSE controller)

### Fase 3 — Backend tests
- B7 (unit) + B8 (integration) ⚡

### Fase 4 — Frontend components (com frontend-design)
- F4 → F5 (KanbanCard)
- F6 → F7 (KanbanColumn)
- F8 → F9 (KanbanBoard — precisa de B5 pra PATCH)

### Fase 5 — Frontend integration
- F10 → F11 (JobList toggle + kanban wiring)

### Fase 6 — Smoke test
- F12 (build + all tests)

---

## Dependências Cross-Layer

| Task frontend | Depende do backend |
|---|---|
| F9 (KanbanBoard SSE + drag-drop) | B5 (PATCH /status endpoint), B6 (SSE stream endpoint) |
| F12 (smoke test) | B8 (integration tests passando) |

---

## Regras Não-Negociáveis

### Backend
- [ ] DTO names/field names copiados da spec BYTE-FOR-BYTE (lesson #1 do RF04 — REINCIDENTE 2x)
- [ ] @Transactional em service methods que modificam dados
- [ ] Permission check no service layer (CREATIVE só move seus jobs)
- [ ] Custom exceptions (ForbiddenException, ResourceNotFoundException)
- [ ] @NotNull em campos obrigatórios de DTOs
- [ ] Sem migration (usa Job.status existente do RF04)
- [ ] @Scheduled heartbeat em JobSseService

### Frontend
- [ ] **Invocar `frontend-design` ANTES de cada component visual** (REINCIDENTE 3x)
- [ ] Standalone components + OnPush
- [ ] input()/output()/signal()/computed() — sem decorators
- [ ] inject() — sem constructor injection
- [ ] @if/@for/@switch — sem *ngIf/*ngFor
- [ ] templateUrl separado
- [ ] Path aliases (@core, @features, @shared)
- [ ] Nunca passar arrays recém-criados para [options] de PrimeNG (usar computed)
- [ ] OnPush + reactive state: markForCheck quando necessário
- [ ] Angular CDK imports standalone (CdkDrag, CdkDropList, etc.)
- [ ] appendTo="body" em todos os dropdowns/menus

---

## Branch & Workflow

1. **Mergear RF04 PR #7 primeiro** (se ainda não mergeado)
2. A partir de `main` (pós-merge): `git checkout -b feature/rf05-kanban`
3. Executar tasks na ordem das fases
4. **Marcar `[x]` em cada task imediatamente ao completar + commit**
5. Ao terminar: invocar `requesting-code-review`
6. Processar feedback via `receiving-code-review`
7. Finalizar via `finishing-a-development-branch`

---


## Backend — Tasks Detalhadas

---

## Task B1: DTOs (UpdateJobStatusDTO, JobStatusResponseDTO, JobStatusEvent)

**File paths:**
- `backend/src/main/java/com/briefflow/dto/job/UpdateJobStatusDTO.java`
- `backend/src/main/java/com/briefflow/dto/job/JobStatusResponseDTO.java`
- `backend/src/main/java/com/briefflow/dto/job/JobStatusEvent.java`

### Red — no test for DTOs (records are declarative)

DTOs are simple Java records with no logic. No dedicated tests needed.

### Green — create the 3 DTOs

**`UpdateJobStatusDTO.java`:**
```java
package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateJobStatusDTO(
    @NotNull JobStatus status,
    boolean confirm
) {}
```

**`JobStatusResponseDTO.java`:**
```java
package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;

public record JobStatusResponseDTO(
    Long id,
    String code,
    JobStatus previousStatus,
    JobStatus newStatus,
    boolean skippedSteps,
    boolean applied
) {}
```

**`JobStatusEvent.java`:**
```java
package com.briefflow.dto.job;

import com.briefflow.enums.JobStatus;

public record JobStatusEvent(
    Long jobId,
    JobStatus previousStatus,
    JobStatus newStatus
) {}
```

### Verify
```bash
./mvnw compile -pl backend -q
```

### Commit
```
feat: add UpdateJobStatusDTO, JobStatusResponseDTO, JobStatusEvent for RF05
```

---

## Task B2: JobSseService (SSE emitter manager + @Scheduled heartbeat)

**File path:** `backend/src/main/java/com/briefflow/service/JobSseService.java`

> NOTE: JobSseService is a concrete `@Service` — not interface+impl. It's infrastructure/plumbing, not domain logic, so no interface indirection needed.

### Red — write test first

**File path:** `backend/src/test/java/com/briefflow/unit/service/JobSseServiceTest.java`

```java
package com.briefflow.unit.service;

import com.briefflow.dto.job.JobStatusEvent;
import com.briefflow.enums.JobStatus;
import com.briefflow.service.JobSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class JobSseServiceTest {

    private JobSseService jobSseService;

    @BeforeEach
    void setUp() {
        jobSseService = new JobSseService();
    }

    @Test
    void should_returnNewSseEmitter_when_subscribe() {
        SseEmitter emitter = jobSseService.subscribe(1L);
        assertNotNull(emitter);
    }

    @Test
    void should_publishToSubscribedEmitters_when_eventSent() {
        // Subscribe creates an emitter — no exception on publish
        jobSseService.subscribe(1L);
        JobStatusEvent event = new JobStatusEvent(10L, JobStatus.NOVO, JobStatus.EM_CRIACAO);
        assertDoesNotThrow(() -> jobSseService.publish(1L, event));
    }

    @Test
    void should_notThrow_when_publishToClientWithNoSubscribers() {
        JobStatusEvent event = new JobStatusEvent(10L, JobStatus.NOVO, JobStatus.EM_CRIACAO);
        assertDoesNotThrow(() -> jobSseService.publish(999L, event));
    }

    @Test
    void should_heartbeatAllEmitters_withoutError() {
        jobSseService.subscribe(1L);
        jobSseService.subscribe(2L);
        assertDoesNotThrow(() -> jobSseService.heartbeat());
    }
}
```

Run test (expect compilation failure — `JobSseService` doesn't exist yet):
```bash
./mvnw test -Dtest=JobSseServiceTest -pl backend
```

### Green — implement JobSseService

```java
package com.briefflow.service;

import com.briefflow.dto.job.JobStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class JobSseService {

    private static final Logger log = LoggerFactory.getLogger(JobSseService.class);
    private static final long SSE_TIMEOUT = 60_000L;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long clientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable removeCallback = () -> removeEmitter(clientId, emitter);
        emitter.onCompletion(removeCallback);
        emitter.onTimeout(removeCallback);
        emitter.onError(e -> removeCallback.run());

        return emitter;
    }

    public void publish(Long clientId, JobStatusEvent event) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters == null) {
            return;
        }

        for (SseEmitter emitter : clientEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("job-status-changed")
                        .data(event));
            } catch (IOException e) {
                log.debug("Removing failed SSE emitter for client {}", clientId);
                removeEmitter(clientId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        emitters.forEach((clientId, clientEmitters) -> {
            for (SseEmitter emitter : clientEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    log.debug("Removing dead SSE emitter for client {} during heartbeat", clientId);
                    removeEmitter(clientId, emitter);
                }
            }
        });
    }

    private void removeEmitter(Long clientId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.remove(emitter);
            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        }
    }
}
```

### Verify
```bash
./mvnw test -Dtest=JobSseServiceTest -pl backend
```

### Commit
```
feat: add JobSseService with SSE emitter management and heartbeat
```

---

## Task B3: @EnableScheduling + SecurityConfig changes

**File paths:**
- `backend/src/main/java/com/briefflow/BriefflowApplication.java`
- `backend/src/main/java/com/briefflow/config/SecurityConfig.java`

### Red — no dedicated test (config changes verified via integration tests in B8)

### Green — implement changes

**`BriefflowApplication.java` — add `@EnableScheduling`:**
```java
package com.briefflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BriefflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefflowApplication.class, args);
    }
}
```

**`SecurityConfig.java` — add SSE stream endpoint to permitted paths:**

Add this line inside `authorizeHttpRequests`:
```java
.requestMatchers("/api/v1/clients/*/jobs/stream").permitAll()
```

This goes after the existing `.requestMatchers("/api/v1/invite/**").permitAll()` line. The SSE controller validates the JWT from the `?token=` query param itself (not via the filter chain), so the endpoint is permitted in Spring Security but authenticated in the controller.

### Verify
```bash
./mvnw compile -pl backend -q
```

### Commit
```
feat: add @EnableScheduling and permit SSE stream endpoint in SecurityConfig
```

---

## Task B4: updateJobStatus in JobService/JobServiceImpl

**File paths:**
- `backend/src/main/java/com/briefflow/service/JobService.java` (add method signature)
- `backend/src/main/java/com/briefflow/service/impl/JobServiceImpl.java` (add implementation)

**Dependencies:** B1 (DTOs must exist)

### Red — write test first

Add the following tests to `backend/src/test/java/com/briefflow/unit/service/JobServiceImplTest.java`:

> NOTE: If `JobServiceImplTest.java` doesn't exist yet (RF04 not merged), create it. Tests follow the existing pattern from `ClientServiceImplTest`.

```java
// Tests to add to JobServiceImplTest:

@Test
void should_updateStatus_when_managerMoves() {
    // Setup: job is NOVO, manager moves to EM_CRIACAO
    Member manager = buildMember(1L, MemberRole.MANAGER);
    Job job = buildJob(10L, "JOB-001", JobStatus.NOVO, null);

    when(memberRepository.findByUserIdAndWorkspaceId(1L, 100L)).thenReturn(Optional.of(manager));
    when(jobRepository.findByIdAndWorkspaceId(10L, 100L)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenReturn(job);

    UpdateJobStatusDTO dto = new UpdateJobStatusDTO(JobStatus.EM_CRIACAO, false);
    JobStatusResponseDTO result = jobService.updateJobStatus(100L, 1L, 10L, dto);

    assertTrue(result.applied());
    assertFalse(result.skippedSteps());
    assertEquals(JobStatus.EM_CRIACAO, result.newStatus());
    verify(jobRepository).save(job);
}

@Test
void should_throwForbidden_when_creativeMovesOtherJob() {
    // Setup: creative tries to move job assigned to another creative
    Member creative = buildMember(2L, MemberRole.CREATIVE);
    Member otherCreative = buildMember(3L, MemberRole.CREATIVE);
    Job job = buildJob(10L, "JOB-001", JobStatus.NOVO, otherCreative);

    when(memberRepository.findByUserIdAndWorkspaceId(2L, 100L)).thenReturn(Optional.of(creative));
    when(jobRepository.findByIdAndWorkspaceId(10L, 100L)).thenReturn(Optional.of(job));

    UpdateJobStatusDTO dto = new UpdateJobStatusDTO(JobStatus.EM_CRIACAO, false);
    assertThrows(ForbiddenException.class,
            () -> jobService.updateJobStatus(100L, 2L, 10L, dto));

    verify(jobRepository, never()).save(any());
}

@Test
void should_returnSkippedSteps_when_forwardSkipWithoutConfirm() {
    // Setup: job is NOVO, user moves to REVISAO_INTERNA (skip EM_CRIACAO), confirm=false
    Member manager = buildMember(1L, MemberRole.MANAGER);
    Job job = buildJob(10L, "JOB-001", JobStatus.NOVO, null);

    when(memberRepository.findByUserIdAndWorkspaceId(1L, 100L)).thenReturn(Optional.of(manager));
    when(jobRepository.findByIdAndWorkspaceId(10L, 100L)).thenReturn(Optional.of(job));

    UpdateJobStatusDTO dto = new UpdateJobStatusDTO(JobStatus.REVISAO_INTERNA, false);
    JobStatusResponseDTO result = jobService.updateJobStatus(100L, 1L, 10L, dto);

    assertTrue(result.skippedSteps());
    assertFalse(result.applied());
    assertEquals(JobStatus.NOVO, job.getStatus()); // not changed
    verify(jobRepository, never()).save(any());
}

@Test
void should_applyStatus_when_confirmedSkip() {
    // Setup: job is NOVO, user moves to REVISAO_INTERNA with confirm=true
    Member manager = buildMember(1L, MemberRole.MANAGER);
    Job job = buildJob(10L, "JOB-001", JobStatus.NOVO, null);

    when(memberRepository.findByUserIdAndWorkspaceId(1L, 100L)).thenReturn(Optional.of(manager));
    when(jobRepository.findByIdAndWorkspaceId(10L, 100L)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenReturn(job);

    UpdateJobStatusDTO dto = new UpdateJobStatusDTO(JobStatus.REVISAO_INTERNA, true);
    JobStatusResponseDTO result = jobService.updateJobStatus(100L, 1L, 10L, dto);

    assertTrue(result.applied());
    assertTrue(result.skippedSteps());
    assertEquals(JobStatus.REVISAO_INTERNA, result.newStatus());
    verify(jobRepository).save(job);
}

@Test
void should_allowBackwardMove_withoutConfirm() {
    // Setup: job is REVISAO_INTERNA, user moves back to EM_CRIACAO
    Member manager = buildMember(1L, MemberRole.MANAGER);
    Job job = buildJob(10L, "JOB-001", JobStatus.REVISAO_INTERNA, null);

    when(memberRepository.findByUserIdAndWorkspaceId(1L, 100L)).thenReturn(Optional.of(manager));
    when(jobRepository.findByIdAndWorkspaceId(10L, 100L)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenReturn(job);

    UpdateJobStatusDTO dto = new UpdateJobStatusDTO(JobStatus.EM_CRIACAO, false);
    JobStatusResponseDTO result = jobService.updateJobStatus(100L, 1L, 10L, dto);

    assertTrue(result.applied());
    assertFalse(result.skippedSteps());
    verify(jobRepository).save(job);
}
```

**Helper methods needed in the test class:**
```java
private Member buildMember(Long id, MemberRole role) {
    Member member = new Member();
    member.setId(id);
    member.setRole(role);
    return member;
}

private Job buildJob(Long id, String code, JobStatus status, Member assignee) {
    Job job = new Job();
    job.setId(id);
    job.setCode(code);
    job.setStatus(status);
    job.setAssignee(assignee);
    return job;
}
```

Run tests (expect failure — `updateJobStatus` not implemented):
```bash
./mvnw test -Dtest=JobServiceImplTest -pl backend
```

### Green — implement updateJobStatus

**Add to `JobService.java` interface:**
```java
JobStatusResponseDTO updateJobStatus(Long workspaceId, Long userId, Long jobId, UpdateJobStatusDTO dto);
```

**Add to `JobServiceImpl.java`:**

First, inject `JobSseService` alongside existing dependencies:
```java
private final JobSseService jobSseService;
```

Add to constructor parameter list and assignment.

Then implement the method:
```java
@Override
@Transactional
public JobStatusResponseDTO updateJobStatus(Long workspaceId, Long userId, Long jobId, UpdateJobStatusDTO dto) {
    Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

    Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Job nao encontrado"));

    // Permission: CREATIVE can only move their own assigned jobs
    if (caller.getRole() == MemberRole.CREATIVE) {
        if (job.getAssignee() == null || !job.getAssignee().getId().equals(caller.getId())) {
            throw new ForbiddenException("Criativos so podem mover jobs atribuidos a eles");
        }
    }

    JobStatus previousStatus = job.getStatus();
    JobStatus newStatus = dto.status();

    // Forward skip detection
    boolean skippedSteps = newStatus.ordinal() > previousStatus.ordinal() + 1;

    if (skippedSteps && !dto.confirm()) {
        return new JobStatusResponseDTO(
                job.getId(),
                job.getCode(),
                previousStatus,
                newStatus,
                true,
                false
        );
    }

    // Apply the status change
    job.setStatus(newStatus);
    jobRepository.save(job);

    // Emit SSE event
    JobStatusEvent event = new JobStatusEvent(job.getId(), previousStatus, newStatus);
    jobSseService.publish(job.getClient().getId(), event);

    return new JobStatusResponseDTO(
            job.getId(),
            job.getCode(),
            previousStatus,
            newStatus,
            skippedSteps,
            true
    );
}
```

**Key design decisions:**
- `job.getClient().getId()` — the SSE publish uses the client ID as the channel key (matches the SSE subscribe endpoint which is scoped by clientId)
- The `JobSseService` is injected into `JobServiceImpl` — since the SSE publish is a side-effect of status change, it lives in the service layer
- No MapStruct mapper needed — `JobStatusResponseDTO` is manually constructed with business logic (skippedSteps, applied flags)

### Verify
```bash
./mvnw test -Dtest=JobServiceImplTest -pl backend
```

### Commit
```
feat: implement updateJobStatus with permission check and skip-step detection
```

---

## Task B5: PATCH endpoint in JobController

**File path:** `backend/src/main/java/com/briefflow/controller/JobController.java`

**Dependencies:** B4

### Red — no separate test (covered by integration test in B8)

### Green — add endpoint

Add to `JobController.java`:
```java
@PatchMapping("/{id}/status")
public ResponseEntity<JobStatusResponseDTO> updateStatus(
        @RequestAttribute("workspaceId") Long workspaceId,
        @RequestAttribute("userId") Long userId,
        @PathVariable Long id,
        @Valid @RequestBody UpdateJobStatusDTO request) {
    return ResponseEntity.ok(jobService.updateJobStatus(workspaceId, userId, id, request));
}
```

Required imports:
```java
import com.briefflow.dto.job.UpdateJobStatusDTO;
import com.briefflow.dto.job.JobStatusResponseDTO;
```

### Verify
```bash
./mvnw compile -pl backend -q
```

### Commit
```
feat: add PATCH /api/v1/jobs/{id}/status endpoint
```

---

## Task B6: JobSseController — SSE stream endpoint

**File path:** `backend/src/main/java/com/briefflow/controller/JobSseController.java`

**Dependencies:** B2 (JobSseService), B3 (SecurityConfig permits endpoint)

### Red — no separate unit test (integration test in B8; SSE controller is thin)

### Green — implement controller

```java
package com.briefflow.controller;

import com.briefflow.exception.UnauthorizedException;
import com.briefflow.security.JwtService;
import com.briefflow.service.JobSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/jobs")
public class JobSseController {

    private final JobSseService jobSseService;
    private final JwtService jwtService;

    public JobSseController(JobSseService jobSseService, JwtService jwtService) {
        this.jobSseService = jobSseService;
        this.jwtService = jwtService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long clientId, @RequestParam String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Token invalido");
        }
        return jobSseService.subscribe(clientId);
    }
}
```

**Design notes:**
- Auth is via `?token=` query param because `EventSource` doesn't support custom headers
- The endpoint is permitted in `SecurityConfig` (B3), so the JwtFilter doesn't run — auth is done manually via `jwtService.isTokenValid(token)`
- If invalid token, throws `UnauthorizedException` which is handled by `GlobalExceptionHandler`

### Verify
```bash
./mvnw compile -pl backend -q
```

### Commit
```
feat: add JobSseController with SSE stream endpoint for real-time updates
```

---

## Task B7: Unit tests — JobServiceImplTest + JobSseServiceTest

**File paths:**
- `backend/src/test/java/com/briefflow/unit/service/JobServiceImplTest.java` (already written in B4 Red phase)
- `backend/src/test/java/com/briefflow/unit/service/JobSseServiceTest.java` (already written in B2 Red phase)

**Dependencies:** B4, B2

This task verifies that all unit tests written during B2 and B4 pass. If RF04 left `JobServiceImplTest` with existing tests, ensure the new tests integrate cleanly alongside them.

### Verify — run all unit tests together
```bash
./mvnw test -Dtest="JobServiceImplTest,JobSseServiceTest" -pl backend
```

All 5 `JobServiceImplTest` tests + 4 `JobSseServiceTest` tests should pass.

### Commit
No separate commit — tests were committed with their respective tasks (B2, B4).

---

## Task B8: Integration tests — JobControllerTest + JobSseControllerTest

**File paths:**
- `backend/src/test/java/com/briefflow/integration/controller/JobControllerTest.java` (add tests or create)
- `backend/src/test/java/com/briefflow/integration/controller/JobSseControllerTest.java` (new file)

**Dependencies:** B5, B6

### Red — write integration tests

**Add to `JobControllerTest.java`** (or create if it doesn't exist from RF04):

```java
@Test
void should_patchStatus_200() throws Exception {
    // Setup: create a job first, then PATCH its status
    String body = """
        {"status": "EM_CRIACAO", "confirm": false}
        """;

    mockMvc.perform(patch("/api/v1/jobs/{id}/status", existingJobId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .requestAttr("workspaceId", testWorkspaceId)
            .requestAttr("userId", testManagerUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applied").value(true))
        .andExpect(jsonPath("$.newStatus").value("EM_CRIACAO"));
}

@Test
void should_patchStatus_return_skippedSteps() throws Exception {
    // Setup: job is NOVO, move to REVISAO_INTERNA (skip EM_CRIACAO)
    String body = """
        {"status": "REVISAO_INTERNA", "confirm": false}
        """;

    mockMvc.perform(patch("/api/v1/jobs/{id}/status", existingJobId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .requestAttr("workspaceId", testWorkspaceId)
            .requestAttr("userId", testManagerUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skippedSteps").value(true))
        .andExpect(jsonPath("$.applied").value(false));
}
```

**`JobSseControllerTest.java`** (smoke test):

```java
package com.briefflow.integration.controller;

import com.briefflow.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobSseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @Test
    void should_openSseStream_200() throws Exception {
        String token = jwtService.generateAccessToken(1L, "test@test.com", 1L);

        mockMvc.perform(get("/api/v1/clients/{clientId}/jobs/stream", 1L)
                .param("token", token))
            .andExpect(status().isOk());
    }

    @Test
    void should_reject_invalidToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/jobs/stream", 1L)
                .param("token", "invalid-token"))
            .andExpect(status().isUnauthorized());
    }
}
```

### Green — tests should pass with all prior tasks implemented

### Verify
```bash
./mvnw test -Dtest="JobControllerTest,JobSseControllerTest" -pl backend
```

### Commit
```
test: add integration tests for PATCH /status and SSE stream endpoints
```

---

## Summary of files created/modified

### New files (6):
- `backend/src/main/java/com/briefflow/dto/job/UpdateJobStatusDTO.java`
- `backend/src/main/java/com/briefflow/dto/job/JobStatusResponseDTO.java`
- `backend/src/main/java/com/briefflow/dto/job/JobStatusEvent.java`
- `backend/src/main/java/com/briefflow/service/JobSseService.java`
- `backend/src/main/java/com/briefflow/controller/JobSseController.java`
- `backend/src/test/java/com/briefflow/unit/service/JobSseServiceTest.java`
- `backend/src/test/java/com/briefflow/integration/controller/JobSseControllerTest.java`

### Modified files (4):
- `backend/src/main/java/com/briefflow/BriefflowApplication.java` — add `@EnableScheduling`
- `backend/src/main/java/com/briefflow/config/SecurityConfig.java` — permit SSE endpoint
- `backend/src/main/java/com/briefflow/service/JobService.java` — add `updateJobStatus` signature
- `backend/src/main/java/com/briefflow/service/impl/JobServiceImpl.java` — implement `updateJobStatus`, inject `JobSseService`
- `backend/src/main/java/com/briefflow/controller/JobController.java` — add `PATCH /{id}/status` endpoint
- `backend/src/test/java/com/briefflow/unit/service/JobServiceImplTest.java` — add 5 new tests

### No new migration needed


---

## Frontend — Tasks Detalhadas

---

### Task F1: Install @angular/cdk + add RF05 types to job.model.ts

**Goal:** Install the Angular CDK dependency and add all new types needed for RF05.

**Steps:**

1. Install Angular CDK:
```bash
cd frontend && npm install @angular/cdk
```

2. Add RF05 types to `frontend/src/app/features/jobs/models/job.model.ts`:

```typescript
// --- RF05: Kanban types ---

export type JobStatus = 'NOVO' | 'EM_CRIACAO' | 'REVISAO_INTERNA' | 'AGUARDANDO_APROVACAO' | 'APROVADO' | 'PUBLICADO';

export interface UpdateJobStatusRequest {
  status: JobStatus;
  confirm: boolean;
}

export interface JobStatusResponse {
  id: number;
  code: string;
  previousStatus: JobStatus;
  newStatus: JobStatus;
  skippedSteps: boolean;
  applied: boolean;
}

export interface JobStatusEvent {
  jobId: number;
  previousStatus: JobStatus;
  newStatus: JobStatus;
}

export interface KanbanColumn {
  status: JobStatus;
  label: string;
  bgColor: string;
  textColor: string;
  dotColor: string;
}

export const KANBAN_COLUMNS: KanbanColumn[] = [
  { status: 'NOVO', label: 'Novo', bgColor: 'bg-blue-50', textColor: 'text-blue-800', dotColor: 'bg-blue-500' },
  { status: 'EM_CRIACAO', label: 'Em Criação', bgColor: 'bg-amber-50', textColor: 'text-amber-800', dotColor: 'bg-amber-500' },
  { status: 'REVISAO_INTERNA', label: 'Revisão Interna', bgColor: 'bg-purple-50', textColor: 'text-purple-800', dotColor: 'bg-purple-500' },
  { status: 'AGUARDANDO_APROVACAO', label: 'Aguardando Aprovação', bgColor: 'bg-orange-50', textColor: 'text-orange-800', dotColor: 'bg-orange-500' },
  { status: 'APROVADO', label: 'Aprovado', bgColor: 'bg-emerald-50', textColor: 'text-emerald-800', dotColor: 'bg-emerald-500' },
  { status: 'PUBLICADO', label: 'Publicado', bgColor: 'bg-green-50', textColor: 'text-green-800', dotColor: 'bg-green-500' },
];
```

> **Note:** The `JobListItem` interface from RF04 must already have a `status: JobStatus` field (or `statusName` that maps). Verify and add `status: JobStatus` if missing. Also ensure `assignedCreativeId: number | null` exists on `JobListItem` for permission checks.

**Test:** No dedicated test — verified by F12 smoke build.

---

### Task F2: JobSseService — EventSource wrapper (⚡ PARALLEL GROUP A)

**Goal:** Create a service that wraps native `EventSource` and exposes SSE events as an RxJS Observable.

**File:** `frontend/src/app/features/jobs/services/job-sse.service.ts`

**TDD Steps:**

1. **RED — Write test first** (`frontend/src/app/features/jobs/services/job-sse.service.spec.ts`):

```typescript
import { TestBed } from '@angular/core/testing';
import { JobSseService } from './job-sse.service';
import { StorageService } from '@core/services/storage.service';

// Mock EventSource globally
class MockEventSource {
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  addEventListener = vi.fn();
  close = vi.fn();
  constructor(public url: string) {
    MockEventSource.lastInstance = this;
  }
  static lastInstance: MockEventSource;
}

describe('JobSseService', () => {
  let service: JobSseService;
  let originalEventSource: typeof EventSource;

  beforeEach(() => {
    originalEventSource = globalThis.EventSource;
    (globalThis as any).EventSource = MockEventSource;

    TestBed.configureTestingModule({
      providers: [
        JobSseService,
        {
          provide: StorageService,
          useValue: { getAccessToken: () => 'test-jwt-token' },
        },
      ],
    });
    service = TestBed.inject(JobSseService);
  });

  afterEach(() => {
    service.disconnect();
    globalThis.EventSource = originalEventSource;
  });

  it('should create EventSource with correct URL and token', () => {
    service.connect(42);
    expect(MockEventSource.lastInstance.url).toContain('/api/v1/clients/42/jobs/stream');
    expect(MockEventSource.lastInstance.url).toContain('token=test-jwt-token');
  });

  it('should emit parsed JobStatusEvent on named event', () => {
    const events: any[] = [];
    service.connect(42).subscribe(e => events.push(e));

    const handler = MockEventSource.lastInstance.addEventListener.mock.calls
      .find((c: any[]) => c[0] === 'job-status-changed');
    expect(handler).toBeTruthy();

    handler![1]({ data: JSON.stringify({ jobId: 1, previousStatus: 'NOVO', newStatus: 'EM_CRIACAO' }) });
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ jobId: 1, previousStatus: 'NOVO', newStatus: 'EM_CRIACAO' });
  });

  it('should close EventSource on disconnect', () => {
    service.connect(42);
    const instance = MockEventSource.lastInstance;
    service.disconnect();
    expect(instance.close).toHaveBeenCalled();
  });

  it('should close previous connection when connecting again', () => {
    service.connect(1);
    const first = MockEventSource.lastInstance;
    service.connect(2);
    expect(first.close).toHaveBeenCalled();
  });
});
```

2. **GREEN — Implement service:**

```typescript
import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '@env/environment';
import { StorageService } from '@core/services/storage.service';
import { JobStatusEvent } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobSseService {
  private readonly storage = inject(StorageService);
  private eventSource: EventSource | null = null;
  private readonly events$ = new Subject<JobStatusEvent>();

  connect(clientId: number): Observable<JobStatusEvent> {
    this.disconnect();

    const token = this.storage.getAccessToken();
    const url = `${environment.apiUrl}/api/v1/clients/${clientId}/jobs/stream?token=${token}`;

    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('job-status-changed', (event: MessageEvent) => {
      const data: JobStatusEvent = JSON.parse(event.data);
      this.events$.next(data);
    });

    this.eventSource.onerror = () => {
      // EventSource reconnects automatically; no manual handling needed
    };

    return this.events$.asObservable();
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
```

3. **REFACTOR:** Ensure clean teardown.

---

### Task F3: JobApiService.updateStatus() method (⚡ PARALLEL GROUP A)

**Goal:** Add the `updateStatus` method to the existing `JobApiService`.

**File:** `frontend/src/app/features/jobs/services/job-api.service.ts` (modify existing)

**TDD Steps:**

1. **RED — Add test** to `frontend/src/app/features/jobs/services/job-api.service.spec.ts`:

```typescript
// Add to existing describe block:

it('should call PATCH /jobs/{id}/status with body', () => {
  const mockResponse: JobStatusResponse = {
    id: 1, code: 'JOB-001', previousStatus: 'NOVO', newStatus: 'EM_CRIACAO',
    skippedSteps: false, applied: true,
  };

  service.updateStatus(1, 'EM_CRIACAO', false).subscribe(res => {
    expect(res).toEqual(mockResponse);
  });

  const req = httpMock.expectOne(r =>
    r.method === 'PATCH' && r.url.endsWith('/jobs/1/status')
  );
  expect(req.request.body).toEqual({ status: 'EM_CRIACAO', confirm: false });
  req.flush(mockResponse);
});

it('should send confirm=true when skipping steps', () => {
  service.updateStatus(1, 'APROVADO', true).subscribe();

  const req = httpMock.expectOne(r => r.method === 'PATCH');
  expect(req.request.body).toEqual({ status: 'APROVADO', confirm: true });
  req.flush({});
});
```

2. **GREEN — Add method** to `JobApiService`:

```typescript
import { JobStatus, JobStatusResponse, UpdateJobStatusRequest } from '../models/job.model';

// Inside the class:
updateStatus(jobId: number, status: JobStatus, confirm = false): Observable<JobStatusResponse> {
  const body: UpdateJobStatusRequest = { status, confirm };
  return this.http.patch<JobStatusResponse>(`${this.baseUrl}/${jobId}/status`, body);
}
```

---

### Task F4: frontend-design for KanbanCardComponent

**Goal:** Invoke `frontend-design` skill to define the visual design for the kanban card before implementing.

**Invoke:** `frontend-design` skill with the following brief:
- Card dimensions: ~250px width (determined by column), auto height
- Layout: compact card with 4 zones stacked vertically
  - **Top row:** Job code (JetBrains Mono, 12px, gray-400) + JobType badge (right-aligned)
  - **Middle:** Title (14px, semibold, gray-900, line-clamp-2)
  - **Bottom row:** Priority badge (left) + deadline text (right, red if overdue) + creative avatar (24px circle, right-most)
- Border-left: 3px solid status color (from KANBAN_COLUMNS dotColor)
- Background: white, border gray-200, rounded-lg (8px)
- Hover: shadow-sm transition
- Disabled state (creative can't drag): opacity-50, cursor-default
- Design system refs: Design Guidelines kanban card section, status colors from spec

**Output:** Validated HTML/CSS template to use in F5.

---

### Task F5: KanbanCardComponent implementation (TDD)

**Goal:** Implement the kanban card component following the design from F4.

**Files:**
- `frontend/src/app/features/jobs/components/kanban-card/kanban-card.component.ts`
- `frontend/src/app/features/jobs/components/kanban-card/kanban-card.component.html`
- `frontend/src/app/features/jobs/components/kanban-card/kanban-card.component.spec.ts`

**TDD Steps:**

1. **RED — Write tests first:**

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanCardComponent } from './kanban-card.component';

describe('KanbanCardComponent', () => {
  let fixture: ComponentFixture<KanbanCardComponent>;

  const mockJob = {
    id: 1, code: 'JOB-001', title: 'Banner para campanha de verão',
    type: 'BANNER', priority: 'ALTA', status: 'NOVO',
    clientName: 'Acme Corp', assignedCreativeName: 'João Silva',
    assignedCreativeId: 5, dueDate: '2026-04-15', isOverdue: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KanbanCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanCardComponent);
    fixture.componentRef.setInput('job', mockJob);
    fixture.componentRef.setInput('disabled', false);
    fixture.detectChanges();
  });

  it('should render job code in monospace', () => {
    const code = fixture.nativeElement.querySelector('[data-testid="job-code"]');
    expect(code.textContent.trim()).toBe('JOB-001');
  });

  it('should render job title', () => {
    const title = fixture.nativeElement.querySelector('[data-testid="job-title"]');
    expect(title.textContent.trim()).toBe('Banner para campanha de verão');
  });

  it('should render type badge', () => {
    const badge = fixture.nativeElement.querySelector('[data-testid="type-badge"]');
    expect(badge.textContent.trim()).toContain('Banner');
  });

  it('should render priority badge', () => {
    const badge = fixture.nativeElement.querySelector('[data-testid="priority-badge"]');
    expect(badge).toBeTruthy();
  });

  it('should show deadline text', () => {
    const deadline = fixture.nativeElement.querySelector('[data-testid="deadline"]');
    expect(deadline).toBeTruthy();
  });

  it('should apply overdue styling when job is overdue', () => {
    fixture.componentRef.setInput('job', { ...mockJob, isOverdue: true });
    fixture.detectChanges();
    const deadline = fixture.nativeElement.querySelector('[data-testid="deadline"]');
    expect(deadline.classList.contains('text-red-500')).toBe(true);
  });

  it('should show creative avatar when assigned', () => {
    const avatar = fixture.nativeElement.querySelector('[data-testid="creative-avatar"]');
    expect(avatar).toBeTruthy();
  });

  it('should apply opacity-50 when disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('[data-testid="kanban-card"]');
    expect(card.classList.contains('opacity-50')).toBe(true);
  });

  it('should emit cardClicked on click', () => {
    const spy = vi.fn();
    fixture.componentInstance.cardClicked.subscribe(spy);
    const card = fixture.nativeElement.querySelector('[data-testid="kanban-card"]');
    card.click();
    expect(spy).toHaveBeenCalledWith(mockJob);
  });
});
```

2. **GREEN — Implement component:**

Component class (`kanban-card.component.ts`):
```typescript
import { Component, ChangeDetectionStrategy, input, output, computed } from '@angular/core';
import { KANBAN_COLUMNS, KanbanColumn } from '../../models/job.model';

@Component({
  selector: 'app-kanban-card',
  standalone: true,
  imports: [],
  templateUrl: './kanban-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KanbanCardComponent {
  readonly job = input.required<any>(); // JobListItem — exact type from RF04
  readonly disabled = input<boolean>(false);
  readonly cardClicked = output<any>();

  readonly statusColumn = computed<KanbanColumn | undefined>(() =>
    KANBAN_COLUMNS.find(c => c.status === this.job().status)
  );

  readonly typeLabel = computed(() => {
    const map: Record<string, string> = {
      POST_FEED: 'Post Feed', STORIES: 'Stories', CARROSSEL: 'Carrossel',
      REELS_VIDEO: 'Reels', BANNER: 'Banner', LOGO: 'Logo', OUTROS: 'Outros',
    };
    return map[this.job().type] ?? this.job().type;
  });

  readonly priorityConfig = computed(() => {
    const map: Record<string, { label: string; class: string }> = {
      BAIXA: { label: 'Baixa', class: 'bg-gray-100 text-gray-600' },
      NORMAL: { label: 'Normal', class: 'bg-blue-50 text-blue-700' },
      ALTA: { label: 'Alta', class: 'bg-amber-50 text-amber-700' },
      URGENTE: { label: 'Urgente', class: 'bg-red-50 text-red-700' },
    };
    return map[this.job().priority] ?? { label: this.job().priority, class: 'bg-gray-100 text-gray-600' };
  });

  onClick(): void {
    this.cardClicked.emit(this.job());
  }
}
```

Template (`kanban-card.component.html`): Uses Tailwind classes per F4 design. Includes `data-testid` attributes for testing. Uses `@if` control flow.

3. **REFACTOR:** Extract shared badge/avatar helpers if needed.

---

### Task F6: frontend-design for KanbanColumnComponent

**Goal:** Invoke `frontend-design` skill to define the column layout.

**Invoke:** `frontend-design` with brief:
- Full height column with header + scrollable card area + empty state
- **Header:** Status dot (8px circle) + label (14px semibold) + count badge (pill, gray-100)
- **Card area:** Vertical scroll, `max-height: calc(100vh - 280px)`, 8px gap between cards, padding 8px
- **Background:** Column-specific bgColor from `KANBAN_COLUMNS`
- **Drop zone:** When dragging, show subtle blue border highlight (2px dashed indigo-300)
- **Empty state:** Centered, dashed border, "Arraste jobs aqui" text in gray-400, min-height 120px
- Column width: equal flex (1/6 of container minus gaps)
- Design system refs: spacing 4px base, border-radius 8px for column container

**Output:** Validated HTML/CSS template to use in F7.

---

### Task F7: KanbanColumnComponent implementation (TDD)

**Goal:** Implement a single kanban column with drop zone.

**Files:**
- `frontend/src/app/features/jobs/components/kanban-column/kanban-column.component.ts`
- `frontend/src/app/features/jobs/components/kanban-column/kanban-column.component.html`
- `frontend/src/app/features/jobs/components/kanban-column/kanban-column.component.spec.ts`

**TDD Steps:**

1. **RED — Write tests first:**

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanColumnComponent } from './kanban-column.component';
import { KanbanCardComponent } from '../kanban-card/kanban-card.component';

describe('KanbanColumnComponent', () => {
  let fixture: ComponentFixture<KanbanColumnComponent>;

  const mockJobs = [
    { id: 1, code: 'JOB-001', title: 'Job 1', type: 'BANNER', priority: 'NORMAL', status: 'NOVO', assignedCreativeId: null, assignedCreativeName: null, dueDate: null, isOverdue: false, clientName: 'C' },
    { id: 2, code: 'JOB-002', title: 'Job 2', type: 'LOGO', priority: 'ALTA', status: 'NOVO', assignedCreativeId: 1, assignedCreativeName: 'Ana', dueDate: '2026-05-01', isOverdue: false, clientName: 'D' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KanbanColumnComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanColumnComponent);
    fixture.componentRef.setInput('status', 'NOVO');
    fixture.componentRef.setInput('label', 'Novo');
    fixture.componentRef.setInput('bgColor', 'bg-blue-50');
    fixture.componentRef.setInput('textColor', 'text-blue-800');
    fixture.componentRef.setInput('dotColor', 'bg-blue-500');
    fixture.componentRef.setInput('jobs', mockJobs);
    fixture.componentRef.setInput('canDragFn', () => true);
    fixture.detectChanges();
  });

  it('should render column label', () => {
    const label = fixture.nativeElement.querySelector('[data-testid="column-label"]');
    expect(label.textContent.trim()).toBe('Novo');
  });

  it('should render job count badge', () => {
    const count = fixture.nativeElement.querySelector('[data-testid="column-count"]');
    expect(count.textContent.trim()).toBe('2');
  });

  it('should render a card for each job', () => {
    const cards = fixture.nativeElement.querySelectorAll('app-kanban-card');
    expect(cards.length).toBe(2);
  });

  it('should show empty state when no jobs', () => {
    fixture.componentRef.setInput('jobs', []);
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('[data-testid="empty-state"]');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('Arraste jobs aqui');
  });

  it('should emit jobDropped when drop event fires', () => {
    // CDK drop event test — will use CdkDropList test utilities
    const spy = vi.fn();
    fixture.componentInstance.jobDropped.subscribe(spy);
    // Simulated via component method or CDK test harness
  });

  it('should emit jobClicked when card is clicked', () => {
    const spy = vi.fn();
    fixture.componentInstance.jobClicked.subscribe(spy);
    const card = fixture.nativeElement.querySelector('app-kanban-card');
    card.dispatchEvent(new Event('cardClicked'));
    // Verify spy called (exact mechanism depends on event bubbling setup)
  });
});
```

2. **GREEN — Implement:**

Component class (`kanban-column.component.ts`):
```typescript
import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CdkDropList, CdkDrag, CdkDragDrop } from '@angular/cdk/drag-drop';
import { KanbanCardComponent } from '../kanban-card/kanban-card.component';
import { JobStatus } from '../../models/job.model';

@Component({
  selector: 'app-kanban-column',
  standalone: true,
  imports: [CdkDropList, CdkDrag, KanbanCardComponent],
  templateUrl: './kanban-column.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KanbanColumnComponent {
  readonly status = input.required<JobStatus>();
  readonly label = input.required<string>();
  readonly bgColor = input.required<string>();
  readonly textColor = input.required<string>();
  readonly dotColor = input.required<string>();
  readonly jobs = input.required<any[]>();
  readonly canDragFn = input.required<(job: any) => boolean>();

  readonly jobDropped = output<CdkDragDrop<any[]>>();
  readonly jobClicked = output<any>();

  onDrop(event: CdkDragDrop<any[]>): void {
    this.jobDropped.emit(event);
  }

  onCardClick(job: any): void {
    this.jobClicked.emit(job);
  }
}
```

Template uses `cdkDropList`, `cdkDrag` with `[cdkDragDisabled]="!canDragFn()(job)"`, `@for` loop, `@if` for empty state.

---

### Task F8: frontend-design for KanbanBoardComponent

**Goal:** Invoke `frontend-design` skill to define the full board layout.

**Invoke:** `frontend-design` with brief:
- Horizontal layout: 6 columns in a flex row with 12px gap, full width, horizontal scroll if needed
- `CdkDropListGroup` wraps all columns for cross-column drag
- Confirm dialog overlay for skip-step confirmation
- Toast notifications for error rollback
- Visual: clean, minimal chrome, columns fill available height
- Drag preview: slightly rotated card (+2deg), shadow-md, 90% opacity
- Loading state: skeleton shimmer for columns while jobs load
- References: Linear kanban, Notion board

**Output:** Validated layout template for F9.

---

### Task F9: KanbanBoardComponent implementation — drag-drop + SSE (TDD)

**Goal:** Orchestrate the 6 columns, manage drag-drop with optimistic updates, handle SSE events.

**Files:**
- `frontend/src/app/features/jobs/components/kanban-board/kanban-board.component.ts`
- `frontend/src/app/features/jobs/components/kanban-board/kanban-board.component.html`
- `frontend/src/app/features/jobs/components/kanban-board/kanban-board.component.spec.ts`

**TDD Steps:**

1. **RED — Write tests first:**

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanBoardComponent } from './kanban-board.component';
import { JobApiService } from '../../services/job-api.service';
import { JobSseService } from '../../services/job-sse.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of, Subject } from 'rxjs';

describe('KanbanBoardComponent', () => {
  let fixture: ComponentFixture<KanbanBoardComponent>;
  let jobApi: jasmine.SpyObj<JobApiService>;
  let sseEvents$: Subject<any>;

  beforeEach(async () => {
    sseEvents$ = new Subject();
    jobApi = { updateStatus: vi.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [KanbanBoardComponent],
      providers: [
        { provide: JobApiService, useValue: jobApi },
        { provide: JobSseService, useValue: { connect: () => sseEvents$.asObservable(), disconnect: vi.fn() } },
        { provide: ConfirmationService, useValue: { confirm: vi.fn() } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanBoardComponent);
    fixture.componentRef.setInput('clientId', 1);
    fixture.componentRef.setInput('jobs', [
      { id: 1, status: 'NOVO', code: 'JOB-001', title: 'A', type: 'BANNER', priority: 'NORMAL', assignedCreativeId: null, assignedCreativeName: null, dueDate: null, isOverdue: false, clientName: 'C' },
      { id: 2, status: 'EM_CRIACAO', code: 'JOB-002', title: 'B', type: 'LOGO', priority: 'ALTA', assignedCreativeId: 1, assignedCreativeName: 'Ana', dueDate: null, isOverdue: false, clientName: 'D' },
    ]);
    fixture.componentRef.setInput('currentUserId', 1);
    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
  });

  it('should render 6 kanban columns', () => {
    const columns = fixture.nativeElement.querySelectorAll('app-kanban-column');
    expect(columns.length).toBe(6);
  });

  it('should group jobs by status into correct columns', () => {
    // NOVO column should have 1 job, EM_CRIACAO should have 1
    const component = fixture.componentInstance;
    const grouped = component.columnJobs();
    expect(grouped['NOVO'].length).toBe(1);
    expect(grouped['EM_CRIACAO'].length).toBe(1);
    expect(grouped['REVISAO_INTERNA'].length).toBe(0);
  });

  it('should call updateStatus on drop and apply optimistically', () => {
    jobApi.updateStatus.mockReturnValue(of({ applied: true, skippedSteps: false } as any));
    // Simulate drop via component method
    component.onJobDrop(1, 'EM_CRIACAO', false);
    expect(jobApi.updateStatus).toHaveBeenCalledWith(1, 'EM_CRIACAO', false);
  });

  it('should move job when SSE event arrives from another user', () => {
    sseEvents$.next({ jobId: 1, previousStatus: 'NOVO', newStatus: 'REVISAO_INTERNA' });
    fixture.detectChanges();
    const grouped = component.columnJobs();
    expect(grouped['NOVO'].length).toBe(0);
    expect(grouped['REVISAO_INTERNA'].length).toBe(1);
  });

  it('should revert optimistic update on API error', () => {
    jobApi.updateStatus.mockReturnValue(throwError(() => ({ status: 403 })));
    component.onJobDrop(1, 'EM_CRIACAO', false);
    fixture.detectChanges();
    // Job should still be in NOVO
    expect(component.columnJobs()['NOVO'].length).toBe(1);
  });
});
```

2. **GREEN — Implement:**

Key logic:
- `jobs` input signal → internal `WritableSignal<JobListItem[]>` to allow optimistic mutations
- `columnJobs = computed(() => { ... })` — groups internal jobs by status into a `Record<JobStatus, JobListItem[]>`
- `canDragFn(job)` — returns true if `canManage` or (`job.assignedCreativeId === currentUserId`)
- `onJobDrop(jobId, targetStatus, confirm)`:
  1. Find job, save `previousStatus`
  2. Optimistically move job in internal signal
  3. Detect skip: if `targetOrdinal > sourceOrdinal + 1` and `confirm === false`, call API with `confirm=false`
  4. If response has `skippedSteps: true, applied: false` → show ConfirmDialog, on accept → call again with `confirm=true`, on reject → revert
  5. If response has `applied: true` → done
  6. On error → revert + show error toast
- SSE: subscribe in `ngOnInit`, update internal jobs signal when event arrives (move the job to new status)
- `ngOnDestroy`: disconnect SSE

**Imports:** `CdkDropListGroup`, `ConfirmDialogModule`, `ToastModule`, `KanbanColumnComponent`.

---

### Task F10: frontend-design for JobListComponent toggle

**Goal:** Invoke `frontend-design` skill to design the view toggle and "Meus Jobs" filter added to the job-list header.

**Invoke:** `frontend-design` with brief:
- **View toggle:** Two icon buttons (`pi-th-large` for kanban, `pi-list` for list) in a button group, right side of page header
- Active button: indigo-500 background, white icon. Inactive: white background, gray-500 icon, border
- **"Meus Jobs" toggle:** `p-toggleswitch` with label "Meus Jobs" to the left of view toggle
- Default state: Kanban view, "Meus Jobs" ON for CREATIVE, OFF for OWNER/MANAGER
- When in kanban view: table is hidden, `<app-kanban-board>` is shown
- When in list view: kanban is hidden, existing p-table is shown
- Responsive: on smaller screens, toggle still accessible
- Design references: Linear view switcher

**Output:** Validated header layout for F11.

---

### Task F11: JobListComponent modification — view toggle + kanban integration (TDD)

**Goal:** Add the view toggle and "Meus Jobs" filter to the existing JobListComponent, integrating the KanbanBoardComponent.

**File:** `frontend/src/app/features/jobs/pages/job-list/job-list.component.ts` (modify) + `.html` (modify)

**TDD Steps:**

1. **RED — Add new tests** to `job-list.component.spec.ts`:

```typescript
it('should default to kanban view', () => {
  expect(component.viewMode()).toBe('kanban');
});

it('should persist view mode in localStorage', () => {
  component.setViewMode('list');
  expect(localStorage.getItem('jobViewMode')).toBe('list');
});

it('should show kanban board when viewMode is kanban', () => {
  component.setViewMode('kanban');
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('app-kanban-board')).toBeTruthy();
  expect(fixture.nativeElement.querySelector('p-table')).toBeFalsy();
});

it('should show table when viewMode is list', () => {
  component.setViewMode('list');
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('app-kanban-board')).toBeFalsy();
  expect(fixture.nativeElement.querySelector('p-table')).toBeTruthy();
});

it('should default myJobsOnly to true for CREATIVE', () => {
  // Setup: mock user with role CREATIVE
  expect(component.myJobsOnly()).toBe(true);
});

it('should default myJobsOnly to false for MANAGER', () => {
  // Setup: mock user with role MANAGER
  expect(component.myJobsOnly()).toBe(false);
});

it('should filter jobs when myJobsOnly is true', () => {
  component.myJobsOnly.set(true);
  fixture.detectChanges();
  // Verify only current user's jobs are passed to kanban board
});
```

2. **GREEN — Modify component:**

Add to component class:
```typescript
readonly viewMode = signal<'list' | 'kanban'>(
  (localStorage.getItem('jobViewMode') as 'list' | 'kanban') ?? 'kanban'
);

readonly myJobsOnly = signal<boolean>(this.currentUser?.role === 'CREATIVE');

readonly filteredJobs = computed(() => {
  const jobs = this.jobs();
  if (!this.myJobsOnly()) return jobs;
  return jobs.filter(j => j.assignedCreativeId === this.currentUser?.id);
});

setViewMode(mode: 'list' | 'kanban'): void {
  this.viewMode.set(mode);
  localStorage.setItem('jobViewMode', mode);
}

toggleMyJobs(): void {
  this.myJobsOnly.update(v => !v);
}
```

Update template:
- Header: add view toggle buttons + "Meus Jobs" toggle switch
- Body: `@if (viewMode() === 'kanban')` → `<app-kanban-board>`, `@else` → existing p-table
- Pass `[jobs]="filteredJobs()"` to kanban board + `[clientId]`, `[currentUserId]`, `[canManage]`

Add `KanbanBoardComponent` to component imports.

---

### Task F12: Smoke test — build + all tests green

**Goal:** Verify the full frontend builds without errors and all tests pass.

**Steps:**

1. Run build:
```bash
cd frontend && ng build
```

2. Run all tests:
```bash
cd frontend && ng test --watch=false
```

3. Fix any compilation errors, broken imports, or failing tests.

4. Verify no lint warnings for unused imports or missing types.

**Acceptance criteria:**
- `ng build` succeeds with zero errors
- All existing tests still pass (no regressions)
- All new tests pass
- No TypeScript strict mode violations
