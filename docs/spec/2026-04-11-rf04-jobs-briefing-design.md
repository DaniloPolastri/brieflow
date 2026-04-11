# RF04 — Jobs e Briefing: Design Spec

**Data:** 2026-04-11
**Status:** Aprovado

---

## Contexto

RF04 introduz a entidade central do BriefFlow: o **Job**. Um job representa uma demanda de peça criativa vinculada a um cliente, com briefing estruturado por tipo (Post Feed, Stories, Carrossel, Reels/Vídeo, Banner, Logo, Outros). É a conexão entre cliente (RF03) e criativo (RF02) e alimenta o resto dos RFs: kanban (RF05), upload de peças (RF06) e portal de aprovação (RF07).

Esta spec cobre apenas a gestão de jobs (CRUD + briefing dinâmico + upload de arquivos de briefing). Kanban, peça final e aprovação ficam explicitamente deferidos para RFs posteriores.

---

## Decisões de Design

| Decisão | Escolha | Justificativa |
|---|---|---|
| Escopo RF04 | Criar + listar + detalhar + editar + arquivar + filtros básicos | Edição é P0 de fato; kanban e peça final são RFs separados com escopo próprio |
| Schema do briefing | `briefing_data` JSONB no PostgreSQL | Evita 7 tabelas normalizadas; adicionar tipo novo = zero migration; sem necessidade de query analítica cruzada |
| Visibilidade para CREATIVE | Jobs dos clientes onde tem ClientMember (sem filtro por atribuição) | Consistente com modelo do RF03; "meus jobs" vira filtro default no kanban (RF05) |
| ID legível | `JOB-001` por workspace, sequencial via `job_counter` atômico | Numeração limpa por agência; `UPDATE ... RETURNING` é atômico no PostgreSQL (zero race condition) |
| Upload de arquivos | Endpoint separado pós-criação (padrão RF03 logo) | Consistência, sem estado temporário, tela de detalhe mostra progresso |
| UX do form | Página única com seções + sidebar sticky de resumo | Gestor cria vários jobs por semana — wizard seria fricção |
| Listagem | `p-table` PrimeNG com busca, filtros, ordenação, paginação | Listagem provisória até RF05; tabela densa é superior pra encontrar job específico |
| Deleção | Soft delete com campo `archived` | Preserva histórico e arquivos; consistente com "active" do RF03 |

---

## Modelo de Domínio

```
Workspace (1) ──< (N) Job >── (1) Client
                    │
                    ├── (1) Member [optional, assigned_creative]
                    │
                    └── (N) JobFile  [anexos de briefing]
```

---

## Backend

### Migration: `V8__create_jobs_tables.sql`

```sql
-- 1. Adicionar contador de jobs ao workspace (geração atômica de JOB-XXX)
ALTER TABLE workspaces ADD COLUMN job_counter INTEGER NOT NULL DEFAULT 0;

-- 2. Tabela principal de jobs
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    sequence_number INTEGER NOT NULL,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    client_id BIGINT NOT NULL REFERENCES clients(id),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description TEXT,
    deadline DATE,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    assigned_creative_id BIGINT REFERENCES members(id),
    status VARCHAR(30) NOT NULL DEFAULT 'NOVO',
    briefing_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    CONSTRAINT uq_jobs_workspace_sequence UNIQUE (workspace_id, sequence_number)
);

CREATE INDEX idx_jobs_workspace_id ON jobs(workspace_id);
CREATE INDEX idx_jobs_workspace_archived ON jobs(workspace_id, archived);
CREATE INDEX idx_jobs_client_id ON jobs(client_id);
CREATE INDEX idx_jobs_assigned_creative ON jobs(assigned_creative_id);
CREATE INDEX idx_jobs_deadline ON jobs(deadline);

-- 3. Tabela de arquivos de briefing
CREATE TABLE job_files (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_files_job_id ON job_files(job_id);
```

### Entity: `Job`

```java
@Entity
@Table(name = "jobs")
@Getter @Setter @NoArgsConstructor
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPriority priority = JobPriority.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_creative_id")
    private Member assignedCreative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.NOVO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "briefing_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> briefingData = new HashMap<>();

    @Column(nullable = false)
    private Boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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

### Entity: `JobFile`

Entity padrão com `@ManyToOne(fetch = LAZY)` para Job, campos `originalFilename`, `storedFilename`, `mimeType`, `sizeBytes`, `uploadedAt`.

### Enums

```java
public enum JobType {
    POST_FEED, STORIES, CARROSSEL, REELS_VIDEO, BANNER, LOGO, OUTROS
}

public enum JobPriority {
    BAIXA, NORMAL, ALTA, URGENTE
}

public enum JobStatus {
    NOVO, EM_CRIACAO, REVISAO_INTERNA, AGUARDANDO_APROVACAO, APROVADO, PUBLICADO
    // RF04 usa apenas NOVO. Outros valores existem para o RF05 (kanban)
}
```

### DTOs

```java
public record JobRequestDTO(
    @NotBlank String title,
    @NotNull Long clientId,
    @NotNull JobType type,
    String description,
    LocalDate deadline,
    @NotNull JobPriority priority,
    Long assignedCreativeId,
    @NotNull Map<String, Object> briefingData
) {}

public record JobResponseDTO(
    Long id,
    String code,                       // "JOB-001"
    String title,
    ClientSummaryDTO client,
    JobType type,
    String description,
    LocalDate deadline,
    JobPriority priority,
    MemberSummaryDTO assignedCreative,
    JobStatus status,
    Map<String, Object> briefingData,
    Boolean archived,
    List<JobFileDTO> files,
    String createdAt,
    String updatedAt,
    String createdByName
) {}

public record JobListItemDTO(
    Long id,
    String code,
    String title,
    String clientName,
    JobType type,
    LocalDate deadline,
    JobPriority priority,
    String assignedCreativeName,
    JobStatus status,
    Boolean isOverdue
) {}

public record JobFileDTO(
    Long id,
    String originalFilename,
    String mimeType,
    Long sizeBytes,
    String uploadedAt,
    String downloadUrl
) {}
```

### Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| POST | `/api/v1/jobs` | Criar job | OWNER, MANAGER |
| GET | `/api/v1/jobs` | Listar jobs (query: `search`, `clientId`, `type`, `priority`, `archived`, `assignedCreativeId`) | Todos (filtrado por ClientMember para CREATIVE) |
| GET | `/api/v1/jobs/{id}` | Detalhe completo | Todos (respeita visibilidade) |
| PUT | `/api/v1/jobs/{id}` | Atualizar job | OWNER, MANAGER |
| PATCH | `/api/v1/jobs/{id}/archive` | Arquivar/desarquivar | OWNER, MANAGER |
| POST | `/api/v1/jobs/{id}/files` | Upload arquivo de briefing (multipart, max 50MB) | OWNER, MANAGER |
| DELETE | `/api/v1/jobs/{id}/files/{fileId}` | Remover arquivo | OWNER, MANAGER |
| GET | `/api/v1/jobs/{id}/files/{fileId}/download` | Download | Todos (respeita visibilidade) |

### Service: `JobServiceImpl`

Responsabilidades principais:

- **`create(JobRequestDTO, callerUserId)`** — transação única que:
  1. Valida permissão OWNER/MANAGER
  2. Valida que o client pertence ao workspace e está ativo
  3. Se `assignedCreativeId != null`: valida Member CREATIVE do workspace E ClientMember com o cliente
  4. Valida `briefingData` via `BriefingValidator.validate(type, data)`
  5. Incrementa `workspaces.job_counter` via `UPDATE ... RETURNING`
  6. Persiste o Job e retorna DTO com código `JOB-XXX`

- **`list(filters, callerMember)`** — aplica filtros combinados:
  - Se `callerMember.role == CREATIVE`: `JOIN ClientMember` para filtrar jobs apenas dos clientes permitidos
  - Todos os filtros do query string
  - Ordena por `createdAt DESC` por padrão
  - `JOIN FETCH` em `client` e `assignedCreative.user` para evitar N+1

- **`findById(id, callerMember)`** — valida visibilidade:
  - OWNER/MANAGER: sempre
  - CREATIVE: só se tem ClientMember com o `job.client` (404 caso contrário — não vazar existência)

- **`update(id, dto, callerUserId)`** — OWNER/MANAGER, revalida briefingData, re-valida regras de atribuição

- **`archive(id, archivedFlag, callerUserId)`** — soft delete / restore

- **`uploadFile(jobId, multipartFile, callerUserId)`** — valida MIME (imagens, PDF, vídeos), tamanho ≤ 50MB, salva em `uploads/jobs/{jobId}/{uuid}.{ext}`, cria `JobFile`

- **`deleteFile(jobId, fileId, callerUserId)`** — remove do disco e do DB

### `BriefingValidator` — Validação Dinâmica por Tipo

Padrão strategy com um validator por tipo, registrados via Spring:

```java
@Component
public class BriefingValidator {
    private final Map<JobType, TypeBriefingValidator> validators;

    public BriefingValidator(List<TypeBriefingValidator> all) {
        this.validators = all.stream()
            .collect(Collectors.toMap(TypeBriefingValidator::supports, Function.identity()));
    }

    public void validate(JobType type, Map<String, Object> data) {
        var validator = validators.get(type);
        if (validator == null) {
            throw new BusinessException("Tipo de job não suportado: " + type);
        }
        validator.validate(data);
    }
}

public interface TypeBriefingValidator {
    JobType supports();
    void validate(Map<String, Object> data);
}
```

**Schemas por tipo (validados no backend e espelhados no frontend):**

| Tipo | Campos obrigatórios | Campos opcionais |
|---|---|---|
| `POST_FEED` | `captionText`, `format` ∈ {1:1, 4:5} | `colorPalette`, `visualReferences` |
| `STORIES` | `text`, `format` = 9:16 | `cta`, `visualReferences` |
| `CARROSSEL` | `slideCount` (2-10) | `slideTexts[]`, `format` ∈ {1:1, 4:5} |
| `REELS_VIDEO` | `duration`, `script` | `audioReference`, `visualReferences` |
| `BANNER` | `dimensions`, `text` | `cta` |
| `LOGO` | `desiredStyle` | `colorReferences`, `visualReferences` |
| `OUTROS` | `freeDescription` | — |

### Repository: `JobRepository`

```java
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j " +
           "JOIN FETCH j.client " +
           "LEFT JOIN FETCH j.assignedCreative ac " +
           "LEFT JOIN FETCH ac.user " +
           "WHERE j.workspace.id = :workspaceId " +
           "AND j.archived = :archived " +
           "ORDER BY j.createdAt DESC")
    List<Job> findByWorkspaceIdAndArchived(@Param("workspaceId") Long workspaceId,
                                           @Param("archived") Boolean archived);

    @Query("SELECT j FROM Job j " +
           "JOIN FETCH j.client c " +
           "LEFT JOIN FETCH j.assignedCreative ac " +
           "LEFT JOIN FETCH ac.user " +
           "WHERE j.workspace.id = :workspaceId " +
           "AND j.archived = :archived " +
           "AND c.id IN (SELECT cm.clientId FROM ClientMember cm WHERE cm.memberId = :memberId) " +
           "ORDER BY j.createdAt DESC")
    List<Job> findVisibleToCreative(@Param("workspaceId") Long workspaceId,
                                    @Param("memberId") Long memberId,
                                    @Param("archived") Boolean archived);

    Optional<Job> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Modifying
    @Query(value = "UPDATE workspaces SET job_counter = job_counter + 1 " +
                   "WHERE id = :workspaceId RETURNING job_counter", nativeQuery = true)
    Integer incrementAndGetJobCounter(@Param("workspaceId") Long workspaceId);
}
```

Filtros adicionais (`clientId`, `type`, `priority`, `search`) aplicados via `Specification<Job>` para composição dinâmica.

### Mapper: `JobMapper` (MapStruct)

```java
@Mapper(componentModel = "spring", uses = {ClientMapper.class, MemberMapper.class})
public interface JobMapper {
    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "files", source = "jobFiles")
    @Mapping(target = "createdByName", source = "createdBy.name")
    JobResponseDTO toResponseDTO(Job job, List<JobFile> jobFiles);

    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "assignedCreativeName", source = "assignedCreative.user.name")
    @Mapping(target = "isOverdue", expression = "java(isOverdue(job))")
    JobListItemDTO toListItemDTO(Job job);

    default boolean isOverdue(Job job) {
        return job.getDeadline() != null
            && job.getDeadline().isBefore(LocalDate.now())
            && job.getStatus() != JobStatus.APROVADO
            && job.getStatus() != JobStatus.PUBLICADO;
    }
}
```

### Upload — Infraestrutura

- Diretório: `uploads/jobs/{jobId}/`
- Nomeação: `{uuid}.{ext}`
- MIME types aceitos: `image/*`, `application/pdf`, `video/mp4`, `video/quicktime`
- Tamanho máximo: 50MB por arquivo (já configurado em `application.yml`)
- Servido via `ResourceHandler` com verificação de acesso via interceptor

---

## Frontend

### Estrutura de Arquivos

```
features/jobs/
├── pages/
│   ├── job-list/
│   ├── job-create/
│   ├── job-edit/
│   └── job-detail/
├── components/
│   ├── job-form/                 # shared form para create/edit
│   ├── briefing-fields/          # seção dinâmica por tipo
│   ├── job-summary-sidebar/      # sticky sidebar com resumo
│   └── job-file-uploader/        # upload com progresso
├── services/
│   └── job-api.service.ts
├── models/
│   ├── job.model.ts
│   └── briefing-schemas.ts
└── jobs.routes.ts
```

### Rotas

```typescript
export const JOBS_ROUTES: Routes = [
  { path: '',         component: JobListComponent },
  { path: 'new',      component: JobCreateComponent, canActivate: [roleGuard('OWNER','MANAGER')] },
  { path: ':id',      component: JobDetailComponent },
  { path: ':id/edit', component: JobEditComponent,   canActivate: [roleGuard('OWNER','MANAGER')] }
];
```

### Tela: Listagem (`/jobs`)

**Header:**
- Título "Jobs" + contador
- Busca por título ou código (debounce 300ms)
- Filtros inline: cliente, tipo, prioridade, criativo, toggle "Ver arquivados"
- Botão "+ Novo Job" (OWNER/MANAGER)

**Tabela `p-table`:**

| Coluna | Conteúdo |
|---|---|
| Código | `JOB-001` em JetBrains Mono |
| Título | truncate com ellipsis |
| Cliente | avatar + nome |
| Tipo | badge colorido |
| Criativo | avatar + nome (ou "—") |
| Prazo | data + ícone vermelho se `isOverdue` |
| Prioridade | badge (cinza/azul/amarelo/vermelho) |
| Ações | menu 3 pontos: Ver, Editar, Arquivar |

- Ordenação por coluna
- Paginação 20 por página
- CREATIVE: tabela sem coluna Ações e sem filtro de criativo
- Estado vazio: "Nenhum job cadastrado" + botão criar (OWNER/MANAGER)

### Tela: Criação (`/jobs/new`)

Layout 2 colunas (70% form / 30% sidebar sticky).

**Form em cards separados:**

1. **Informações Gerais:** título, cliente, tipo, prazo, prioridade, criativo atribuído, descrição geral
   - Ao mudar cliente, recarrega lista de criativos filtrada por ClientMember
   - Ao mudar tipo, re-renderiza seção de Briefing
2. **Briefing:** `briefing-fields` component renderiza campos dinamicamente baseado no tipo
3. **Anexos de briefing:** `job-file-uploader` em modo staging — arquivos ficam em memória até o submit

**Sidebar (sticky):**
- Resumo dos campos preenchidos em tempo real
- Indicador de campos obrigatórios faltando
- Botão "Salvar Job" (fixo)
- Botão "Cancelar"

**Fluxo de submit:**
1. Validar form (Reactive Forms)
2. `POST /api/v1/jobs` com payload
3. Receber `jobId`
4. Navegar para `/jobs/{id}`
5. Disparar uploads pendentes em paralelo (max 3 simultâneos) — progresso visível na tela de detalhe

### Tela: Edição (`/jobs/{id}/edit`)

- Reusa `job-form` component em modo "edit"
- Anexos existentes com botão de remover
- Novos anexos são uploadados imediatamente (job já existe)
- `PUT /api/v1/jobs/{id}` ao salvar

### Tela: Detalhe (`/jobs/{id}`)

Layout 2 colunas (70% conteúdo / 30% metadados).

**Header:**
- Código em mono + título
- Badge de status
- Ações (OWNER/MANAGER): Editar, Arquivar

**Conteúdo (esquerda):**
- Descrição Geral
- Briefing (view-only dos campos de `briefingData`)
- Anexos — grid de cards com preview (thumbnail imagens, ícone PDFs/vídeos)
  - Clicar baixa
  - OWNER/MANAGER: botão remover por card
  - Uploads em progresso: spinner + porcentagem

**Metadados (direita):**
- Cliente, tipo, prazo, prioridade, criativo atribuído, criado por, criado em

### Component: `briefing-fields` (dinâmico)

Recebe `type: Signal<JobType>` e `FormGroup` parent. Mantém dicionário `BRIEFING_SCHEMAS` declarativo:

```typescript
export const BRIEFING_SCHEMAS: Record<JobType, BriefingFieldSchema[]> = {
  POST_FEED: [
    { key: 'captionText', label: 'Texto da legenda', type: 'textarea', required: true },
    { key: 'format',      label: 'Formato',          type: 'select', options: ['1:1','4:5'], required: true },
    { key: 'colorPalette',label: 'Paleta de cores',  type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' }
  ],
  STORIES: [
    { key: 'text',   label: 'Texto',   type: 'textarea', required: true },
    { key: 'format', label: 'Formato', type: 'select', options: ['9:16'], required: true },
    { key: 'cta',    label: 'CTA',     type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' }
  ],
  CARROSSEL: [
    { key: 'slideCount', label: 'Número de slides', type: 'number', required: true, min: 2, max: 10 },
    { key: 'slideTexts', label: 'Texto por slide',  type: 'dynamic-list' },
    { key: 'format',     label: 'Formato',          type: 'select', options: ['1:1','4:5'] }
  ],
  REELS_VIDEO: [
    { key: 'duration', label: 'Duração (segundos)', type: 'number', required: true },
    { key: 'script',   label: 'Roteiro/Storyboard', type: 'textarea', required: true },
    { key: 'audioReference', label: 'Referência de áudio', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' }
  ],
  BANNER: [
    { key: 'dimensions', label: 'Dimensões', type: 'text', required: true },
    { key: 'text',       label: 'Texto',     type: 'textarea', required: true },
    { key: 'cta',        label: 'CTA',       type: 'text' }
  ],
  LOGO: [
    { key: 'desiredStyle',     label: 'Estilo desejado',     type: 'textarea', required: true },
    { key: 'colorReferences',  label: 'Referências de cor',  type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' }
  ],
  OUTROS: [
    { key: 'freeDescription', label: 'Descrição livre', type: 'textarea', required: true }
  ]
};
```

Ao mudar `type`, o component limpa controls antigos e adiciona os novos baseados no schema.

### Service: `JobApiService`

```typescript
@Injectable({ providedIn: 'root' })
export class JobApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/jobs`;

  list(filters?: JobListFilters): Observable<JobListItem[]>;
  getById(id: number): Observable<Job>;
  create(request: JobRequest): Observable<Job>;
  update(id: number, request: JobRequest): Observable<Job>;
  archive(id: number, archived: boolean): Observable<Job>;
  uploadFile(jobId: number, file: File): Observable<HttpEvent<JobFile>>;
  deleteFile(jobId: number, fileId: number): Observable<void>;
  downloadUrl(jobId: number, fileId: number): string;
}
```

`uploadFile` usa `reportProgress: true` + `observe: 'events'` para expor progresso ao uploader.

### Models

```typescript
export type JobType = 'POST_FEED' | 'STORIES' | 'CARROSSEL' | 'REELS_VIDEO' | 'BANNER' | 'LOGO' | 'OUTROS';
export type JobPriority = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';
export type JobStatus = 'NOVO' | 'EM_CRIACAO' | 'REVISAO_INTERNA' | 'AGUARDANDO_APROVACAO' | 'APROVADO' | 'PUBLICADO';

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

export interface JobFile {
  id: number;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  downloadUrl: string;
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

---

## Permissões

| Ação | OWNER | MANAGER | CREATIVE |
|---|---|---|---|
| Listar jobs | Sim (todos) | Sim (todos) | Sim (só dos clientes via ClientMember) |
| Ver detalhe | Sim | Sim | Sim (só dos clientes via ClientMember) |
| Criar job | Sim | Sim | Não |
| Editar job | Sim | Sim | Não |
| Arquivar/restaurar | Sim | Sim | Não |
| Upload de arquivo | Sim | Sim | Não |
| Remover arquivo | Sim | Sim | Não |
| Download de arquivo | Sim | Sim | Sim (só se vê o job) |
| Atribuir criativo | Sim | Sim | Não |

**Regras adicionais:**
- Atribuição de criativo: valida que o Member é CREATIVE E tem ClientMember com o cliente selecionado (rejeita 400 caso contrário)
- Visibilidade para CREATIVE: `findVisibleToCreative` via `JOIN ClientMember`
- Edge case: criativo atribuído que perde acesso via remoção de ClientMember — não vê mais na listagem, mas o job mantém a referência; OWNER/MANAGER pode re-atribuir

---

## Edge Cases Tratados

| Cenário | Comportamento |
|---|---|
| Criar job com prazo no passado | Permitido (urgência real); aviso visual amarelo |
| Criar job sem criativo atribuído | Permitido — fica como "Não atribuído"; OWNER/MANAGER e CREATIVEs do cliente veem |
| Atribuir criativo sem ClientMember com o cliente | Rejeitado (400) |
| Cliente inativo ao criar job | Rejeitado (400) |
| Cliente inativado após criar jobs | Jobs continuam visíveis (RF03 garante) |
| Upload > 50MB | Rejeitado (400) |
| MIME type não aceito | Rejeitado (400) |
| Deletar arquivo inexistente | Rejeitado (404) |
| Buscar job arquivado por ID | Permitido — detalhe sempre acessível |
| Listagem default | Mostra só `archived = false`; toggle revela arquivados |
| CREATIVE faz GET de job que não vê | Rejeitado (404 — não vazar existência) |
| `briefingData` com chave extra | Ignorado silenciosamente |
| `briefingData` sem campo obrigatório | Rejeitado (400) com mensagem clara do campo faltando |
| Counter > 999 | `JOB-1000` funciona normalmente (`%03d` é padding mínimo) |
| Concorrência no counter | `UPDATE ... RETURNING` é atômico — zero race condition |

### Fluxo de falha no upload pós-criação

1. Job fica criado com os arquivos que subiram com sucesso
2. Arquivos que falharam mostram card de erro na tela de detalhe com botão "Tentar novamente"
3. Usuário pode retentar individualmente ou abandonar
4. Sem rollback do job — ele existe e está utilizável mesmo sem todos os anexos

---

## Testes (TDD)

### Backend

**Unitários (`src/test/java/com/briefflow/unit/`):**
- `service/JobServiceImplTest` — create (permissão, assignment invalid, ClientMember missing, counter), list (visibilidade por role), update, archive, uploadFile (MIME/tamanho), deleteFile
- `mapper/JobMapperTest` — `toResponseDTO`, `toListItemDTO` (`isOverdue` com datas limite)
- `service/briefing/BriefingValidatorTest` + 7 testes individuais (um por tipo)

**Integração (`src/test/java/com/briefflow/integration/`):**
- `repository/JobRepositoryTest` (Testcontainers) — `findVisibleToCreative` respeita ClientMember, `incrementAndGetJobCounter` em transações concorrentes
- `controller/JobControllerTest` — todos endpoints, 201/200/400/403/404, upload multipart, download

### Frontend

**Services:**
- `job-api.service.spec.ts` — todos os métodos HTTP mockados

**Components:**
- `job-list.component.spec.ts` — render de tabela, filtros, busca com debounce, esconde ações para CREATIVE
- `job-form.component.spec.ts` — validação, submissão, integração com `briefing-fields`
- `briefing-fields.component.spec.ts` — muda schema ao mudar type, valida required, `CARROSSEL` gera N inputs para `slideTexts`
- `job-detail.component.spec.ts` — renderiza briefing correto por tipo, botões por role
- `job-file-uploader.component.spec.ts` — validação MIME/tamanho, barra de progresso

---

## Arquivos que Mudam no Repositório

### Backend (novos)
- `V8__create_jobs_tables.sql`
- `entity/Job.java`, `entity/JobFile.java`
- `entity/enums/JobType.java`, `JobPriority.java`, `JobStatus.java`
- `repository/JobRepository.java`, `JobFileRepository.java`
- `dto/JobRequestDTO.java`, `JobResponseDTO.java`, `JobListItemDTO.java`, `JobFileDTO.java`, `ClientSummaryDTO.java`, `MemberSummaryDTO.java`
- `mapper/JobMapper.java`
- `service/JobService.java`, `service/impl/JobServiceImpl.java`
- `service/briefing/BriefingValidator.java` + `TypeBriefingValidator.java` + 7 impls
- `controller/JobController.java`
- Testes conforme seção "Testes"

### Backend (alterados)
- `entity/Workspace.java` — adicionar `job_counter`

### Frontend (novos)
- `features/jobs/` completo
- Rota `/jobs` em `app.routes.ts` com lazy loading
- Item "Jobs" no sidebar

---

## Deferido para RFs Posteriores

Lista explícita do que **não** entra em RF04 e onde será coberto. Serve como lembrete ao iniciar os próximos refinos.

### RF05 — Kanban de Produção
- Board com drag & drop
- Colunas (status) customizáveis por workspace (tabela `kanban_statuses`)
- Transições de status (regras de quem pode mover)
- Indicador visual de atraso no card do kanban
- Filtros avançados no kanban
- Geração automática do link de aprovação ao mover para "Aguardando Aprovação" (integração RF07)
- Filtro default "Criativo vê apenas seus jobs por padrão"

### RF06 — Upload de Peças (Deliverable)
- Upload da peça final pelo criativo (diferente dos anexos de briefing do RF04)
- Histórico de versões da peça
- Thumbnails gerados automaticamente
- Nova tabela `job_deliverables`

### RF07 — Portal de Aprovação
- Link público com token UUID
- Página pública `/approval/{token}` sem login
- Aprovar / solicitar revisão com comentário
- Entities `ApprovalToken`, `ApprovalHistory`
- Expiração do token em 30 dias

### Notificações por email (RF consolidado futuro)
- Email ao criativo: novo job atribuído
- Email ao gestor: cliente aprovou / pediu revisão
- Email ao criativo: cliente pediu revisão
- Configuração SMTP

---

## Observações

- A infraestrutura de upload (filesystem local, validação MIME/tamanho) do RF03 é reutilizada aqui, agora com diretório por job (`uploads/jobs/{jobId}/`)
- O `BriefingValidator` é extensível: adicionar um tipo novo de job = criar um `TypeBriefingValidator` novo + atualizar enum + schema do frontend. Zero migration
- A entidade `Workspace` ganha `job_counter` — essa migration é destrutiva para dados existentes no sentido de que todos os workspaces passam a ter `job_counter = 0`, o que é correto (nenhum job foi criado ainda)
- O `JobStatus` enum já inclui todos os valores do kanban futuro (RF05) para evitar migration adicional depois
