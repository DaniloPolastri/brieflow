# RF05 — Kanban de Produção: Design Spec

**Data:** 2026-04-12
**Status:** Aprovado
**Depende de:** RF04 (Jobs e Briefing) — deve estar mergeado antes da execução

---

## Contexto

RF05 transforma a listagem de jobs (p-table do RF04) em um **kanban board com drag-drop** e atualização em tempo real via SSE. O kanban é a view default dentro do workspace do cliente (`/clients/:clientId/jobs`), com toggle pra voltar à lista. É a visualização central do fluxo de produção da agência.

Sem status customizáveis (P1 pós-MVP). Sem token de aprovação automático (RF07). Sem filtros avançados (P1). Foco: board visual funcional com drag-drop + SSE.

---

## Decisões de Design

| Decisão | Escolha | Justificativa |
|---|---|---|
| Colunas do kanban | 6 colunas fixas (hardcoded do JobStatus enum) | Status padrão cobrem 90% dos casos (MVP-SCOPE P1 pra custom) |
| Coexistência com lista | Toggle "Kanban \| Lista" na mesma rota, default Kanban, persiste em localStorage | Pattern consagrado (Linear, Notion, Jira). Lista mantém busca/ordenação pra power users |
| Drag-drop — quem move | OWNER/MANAGER move qualquer job; CREATIVE move só jobs atribuídos a ele | Criativo precisa sinalizar progresso; não faz sentido mexer em jobs de outros |
| Drag-drop — pular etapas | Confirmação soft (ConfirmDialog) ao pular etapa pra frente; mover pra trás é livre | Flexibilidade pra agência pequena, com safety net pra erros |
| Card visual | Código + título + tipo badge + prioridade + prazo (overdue) + avatar criativo | Máximo de contexto em ~250px de largura |
| Real-time | SSE (Server-Sent Events) — server→client, EventSource nativo | Zero dependência npm extra; unidirecional é suficiente pro kanban; MVP-SCOPE vetou WebSocket mas SSE é HTTP streaming padrão |
| Auth no SSE | JWT via query param `?token=xxx` | EventSource não suporta custom headers; token curto (15min) + HTTPS; migra pra httpOnly cookie pós-MVP |
| Reordenação intra-coluna | Não suportada (MVP) | Requer campo `position INT` + persistência; deferir pra P1 |
| Polling fallback | Não — SSE é o mecanismo primário; reconexão automática nativa do EventSource | Simplifica; polling seria redundante com SSE |

---

## Backend

### Novos DTOs

```java
public record UpdateJobStatusDTO(
    @NotNull JobStatus status,
    boolean confirm
) {}

public record JobStatusResponseDTO(
    Long id,
    String code,
    JobStatus previousStatus,
    JobStatus newStatus,
    boolean skippedSteps,
    boolean applied
) {}

public record JobStatusEvent(
    Long jobId,
    JobStatus previousStatus,
    JobStatus newStatus
) {}
```

### Novo endpoint: `PATCH /api/v1/jobs/{id}/status`

Atualiza apenas o status de um job (mais leve que PUT que atualiza tudo).

**Fluxo no service:**

1. Valida permissão: OWNER/MANAGER move qualquer job; CREATIVE move só se `assignedCreative.id == callerId`
2. Detecta salto de etapa: se `toStatus.ordinal() > fromStatus.ordinal() + 1` e `confirm == false`, retorna `{ skippedSteps: true, applied: false }` sem alterar o job
3. Se `confirm == true` ou não pulou etapa: atualiza `job.status`, salva, emite evento SSE
4. Retorna `{ applied: true }`

Mover pra trás (ordinal menor) é sempre permitido sem confirmação.

**Controller:**

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

### Novo endpoint SSE: `GET /api/v1/clients/{clientId}/jobs/stream`

Abre conexão SSE persistente. Publica eventos quando jobs do client mudam de status.

- `Content-Type: text/event-stream`
- Auth via query param `?token=xxx` (backend valida JWT no handshake)
- Timeout: 60s (EventSource reconecta automaticamente)
- Heartbeat: `comment` event a cada 30s pra manter conexão viva contra proxies/firewalls

### `JobSseService`

```java
@Service
public class JobSseService {
    // ConcurrentHashMap<clientId, CopyOnWriteArrayList<SseEmitter>>
    // subscribe(clientId) → cria SseEmitter, registra, retorna
    // publish(clientId, event) → itera emitters do client, envia evento, remove os mortos
    // heartbeat() @Scheduled(fixedRate=30000) → envia comment pra todos
    // removeEmitter() → cleanup em onCompletion/onTimeout/onError
}
```

### `JobSseController`

```java
@RestController
@RequestMapping("/api/v1/clients/{clientId}/jobs")
public class JobSseController {
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long clientId, @RequestParam String token) {
        // Valida JWT do token param
        // Retorna jobSseService.subscribe(clientId)
    }
}
```

### Habilitar `@EnableScheduling`

Adicionar na class `BriefflowApplication.java` se não existir.

### Migration

**Nenhuma.** O campo `status VARCHAR(32)` já existe na tabela `jobs`. O enum `JobStatus` já tem os 6 valores.

### SecurityConfig

Adicionar endpoint SSE (`/api/v1/clients/*/jobs/stream`) como permitido (ou autenticado via query param em vez de header).

### Endpoints finais (RF05 adiciona 2)

| Método | Endpoint | Descrição | Novo? |
|---|---|---|---|
| PATCH | `/api/v1/jobs/{id}/status` | Atualizar status (drag-drop) | **Sim** |
| GET | `/api/v1/clients/{clientId}/jobs/stream` | SSE stream de eventos | **Sim** |

### Testes Backend

**Unitários:**
- `JobServiceImplTest` — `should_updateStatus_when_managerMoves`, `should_throwForbidden_when_creativeMovesOtherJob`, `should_returnSkippedSteps_when_forwardSkipWithoutConfirm`, `should_applyStatus_when_confirmedSkip`, `should_allowBackwardMove_withoutConfirm`
- `JobSseServiceTest` — `should_publishToSubscribedEmitters`, `should_removeEmitterOnError`, `should_heartbeatAllEmitters`

**Integração:**
- `JobControllerTest` — `should_patchStatus_200`, `should_patchStatus_return_skippedSteps`
- `JobSseControllerTest` — `should_openSseStream_200` (smoke test)

---

## Frontend

### Nova dependência

```bash
npm install @angular/cdk
```

Usado: `CdkDrag`, `CdkDropList`, `CdkDropListGroup`, `moveItemInArray`, `transferArrayItem`.

### Novos componentes

```
features/jobs/
├── components/
│   ├── kanban-board/        # orquestra 6 colunas, gerencia drag-drop + SSE
│   ├── kanban-column/       # uma coluna do board, drop zone
│   └── kanban-card/         # card individual de um job
├── services/
│   └── job-sse.service.ts   # EventSource wrapper, expõe Observable<JobStatusEvent>
```

### `KanbanBoardComponent`

**Inputs:** `clientId`, `jobs` (Signal<JobListItem[]>), `currentUserId`, `canManage`

**Responsabilidades:**
- Agrupa jobs por status em 6 colunas via `computed` signal
- Gerencia drag-drop via `CdkDropListGroup`
- Optimistic update: move card localmente ANTES da resposta do server
- Se `skippedSteps`: mostra ConfirmDialog, reverte se cancelado
- Se erro (403/404): reverte com toast
- Escuta `JobSseService`: quando evento de outro user chega, move card na coluna certa
- Conecta/desconecta SSE em `ngOnInit`/`ngOnDestroy`

**Colunas hardcoded:**

```typescript
const KANBAN_COLUMNS = [
  { status: 'NOVO', label: 'Novo', color: 'bg-gray-100' },
  { status: 'EM_CRIACAO', label: 'Em Criação', color: 'bg-blue-50' },
  { status: 'REVISAO_INTERNA', label: 'Revisão Interna', color: 'bg-amber-50' },
  { status: 'AGUARDANDO_APROVACAO', label: 'Aguardando Aprovação', color: 'bg-purple-50' },
  { status: 'APROVADO', label: 'Aprovado', color: 'bg-emerald-50' },
  { status: 'PUBLICADO', label: 'Publicado', color: 'bg-indigo-50' },
];
```

### `KanbanColumnComponent`

**Inputs:** `status`, `label`, `jobs`, `canDrag: (job) => boolean`

**Outputs:** `jobDropped`, `jobClicked`

Layout: header com label + badge count, drop zone com scroll vertical (`max-height: calc(100vh - 280px)`), empty state com borda tracejada.

### `KanbanCardComponent`

**Input:** `job: JobListItem`

Card compacto (~250px largura):
- Top: código (mono) + tipo badge
- Middle: título (line-clamp-2)
- Bottom: prioridade badge + prazo (vermelho se overdue) + avatar criativo

Cards de jobs que o creative não pode mover: `opacity-50`, `cursor-default`, drag disabled.

### `JobSseService`

```typescript
@Injectable({ providedIn: 'root' })
export class JobSseService {
  connect(clientId: number): Observable<JobStatusEvent>
  disconnect(): void
}
```

- Usa `EventSource` nativo (zero dependência)
- Token JWT via query param
- Evento `job-status-changed` parseado do `event.data`
- Reconexão automática nativa; ao reconectar, emite evento especial pra full reload

### Toggle Lista/Kanban no `JobListComponent`

```typescript
readonly viewMode = signal<'list' | 'kanban'>(
  (localStorage.getItem('jobViewMode') as 'list' | 'kanban') ?? 'kanban'
);
```

Header ganha 2 botões de view toggle (ícones `pi-th-large` e `pi-list`). Conteúdo condicional via `@if (viewMode() === 'kanban')`.

### Filtro "Meus Jobs"

```typescript
readonly myJobsOnly = signal<boolean>(currentUser.role === 'CREATIVE');
```

- Default ON pra CREATIVE, OFF pra OWNER/MANAGER
- Quando ON: cards de outros criativos ficam ocultos (não renderizados)
- Quando OFF (creative): cards de outros aparecem com `opacity-50` e drag disabled

### `JobApiService` — novo método

```typescript
updateStatus(jobId: number, status: JobStatus, confirm = false): Observable<JobStatusResponse>
```

### Testes Frontend

- `kanban-board.component.spec.ts` — 6 colunas renderizadas, agrupa jobs, drag-drop move card, SSE event move card
- `kanban-column.component.spec.ts` — renderiza cards, empty state, drop zone
- `kanban-card.component.spec.ts` — renderiza code/title/type/priority/deadline/creative, overdue styling
- `job-sse.service.spec.ts` — mock EventSource, connect/disconnect/event parsing
- `job-list.component.spec.ts` — testes existentes + toggle view mode

---

## Permissões

| Ação | OWNER/MANAGER | CREATIVE |
|---|---|---|
| Ver kanban (todos os jobs do client) | Sim | Sim (filtrado por ClientMember) |
| Arrastar job atribuído a ele | Sim | Sim |
| Arrastar job de outro criativo | Sim | Não (drag disabled) |
| Arrastar job sem criativo | Sim | Não |
| Pular etapa pra frente | Sim (confirm) | Sim pra seus jobs (confirm) |
| Mover pra trás | Sim (sem confirm) | Sim pra seus jobs (sem confirm) |
| Toggle lista/kanban | Sim | Sim |
| Toggle "meus jobs" | Sim (default OFF) | Sim (default ON) |

---

## Edge Cases

| Cenário | Comportamento |
|---|---|
| Server rejeita drag (403/404) | Optimistic update revertido com animação + toast vermelho |
| Pular etapa sem confirmar | Card fica na coluna destino, ConfirmDialog aparece. Cancelar = reverte |
| SSE desconecta | EventSource reconecta nativo. Ao reconectar, frontend faz reload completo da lista |
| Dois users movem mesmo job | Last-writer-wins. SSE distribui estado final. Sem lock pessimista |
| Job arquivado no kanban | Não aparece — filtro `archived: false` aplicado |
| Coluna vazia | Drop zone com borda tracejada + "Arraste jobs aqui" |
| Muitos jobs por coluna | Scroll vertical independente por coluna |
| Drop na mesma coluna (reordenar) | Ignorado — sem persistência de ordem intra-coluna no MVP |
| Token JWT expira durante SSE | Backend fecha conexão. Frontend renova token via refresh e reconecta |
| Creative sem jobs atribuídos (myJobsOnly ON) | Todas as colunas vazias, mensagem: "Nenhum job atribuído a você" |

---

## Deferido para Features Futuras

| Feature | Quando | Motivo |
|---|---|---|
| Status customizáveis (tabela kanban_statuses, CRUD, reordenar) | P1 pós-MVP | Status padrão cobrem 90%; validar com users |
| Token de aprovação automático ao mover pra "Aguardando Aprovação" | RF07 | Sem portal, token é código morto |
| Filtros avançados (data range, multi-select, busca no board) | P1 pós-MVP | Filtros básicos resolvem |
| Reordenação intra-coluna | P1 pós-MVP | Requer campo position + persistência |
| WebSocket (substituir SSE) | P2 se SSE não escalar | SSE suficiente pra 2-15 users |
| Notificação por email ao mover status | RF09 | Feature separada de emails |

---

## Observações

- **Zero migration** — tudo usa infraestrutura existente do RF04
- **Angular CDK** é a única dependência nova (drag-drop), instalar via `npm install @angular/cdk`
- **SSE heartbeat a cada 30s** requer `@EnableScheduling` na application class
- **Token JWT no query param** é simplificação MVP (pós-MVP migra pra httpOnly cookie)
- O `JobStatus` enum no backend já inclui PUBLICADO (RF04 preparou pra RF05)
- Quando RF07 for implementado, o `updateJobStatus` ganha um hook pra gerar `ApprovalToken` ao mover pra `AGUARDANDO_APROVACAO` — sem refactor do RF05
