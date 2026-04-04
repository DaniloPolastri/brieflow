# BriefFlow — Backend Structure

**Stack:** Java 21 + Spring Boot 3+ + PostgreSQL  
**Padrão:** Arquitetura em camadas enterprise  
**Referência:** ESTRUTURA-BACKEND.md + java-spring-best-practices.md

---

## Estrutura de Pastas

```text
src/main/java/com/briefflow
│
├── BriefflowApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── SwaggerConfig.java
│   ├── WebConfig.java
│   └── MailConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── WorkspaceController.java
│   ├── MemberController.java
│   ├── ClientController.java
│   ├── JobController.java
│   ├── KanbanController.java
│   ├── ApprovalController.java
│   ├── DashboardController.java
│   └── FileController.java
│
├── service/
│   ├── AuthService.java
│   ├── WorkspaceService.java
│   ├── MemberService.java
│   ├── ClientService.java
│   ├── JobService.java
│   ├── KanbanService.java
│   ├── ApprovalService.java
│   ├── DashboardService.java
│   ├── FileStorageService.java
│   ├── EmailService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── WorkspaceServiceImpl.java
│       ├── MemberServiceImpl.java
│       ├── ClientServiceImpl.java
│       ├── JobServiceImpl.java
│       ├── KanbanServiceImpl.java
│       ├── ApprovalServiceImpl.java
│       ├── DashboardServiceImpl.java
│       ├── FileStorageServiceImpl.java
│       └── EmailServiceImpl.java
│
├── repository/
│   ├── UserRepository.java
│   ├── WorkspaceRepository.java
│   ├── MemberRepository.java
│   ├── ClientRepository.java
│   ├── JobRepository.java
│   ├── JobFileRepository.java
│   ├── KanbanStatusRepository.java
│   ├── ApprovalTokenRepository.java
│   ├── ApprovalHistoryRepository.java
│   └── RefreshTokenRepository.java
│
├── entity/
│   ├── User.java
│   ├── Workspace.java
│   ├── Member.java
│   ├── Client.java
│   ├── Job.java
│   ├── JobFile.java
│   ├── JobBriefing.java
│   ├── KanbanStatus.java
│   ├── ApprovalToken.java
│   ├── ApprovalHistory.java
│   ├── InviteToken.java
│   └── RefreshToken.java
│
├── dto/
│   ├── auth/
│   │   ├── RegisterRequestDTO.java
│   │   ├── LoginRequestDTO.java
│   │   ├── TokenResponseDTO.java
│   │   └── RefreshTokenRequestDTO.java
│   ├── workspace/
│   │   ├── WorkspaceResponseDTO.java
│   │   └── WorkspaceUpdateDTO.java
│   ├── member/
│   │   ├── InviteMemberRequestDTO.java
│   │   ├── MemberResponseDTO.java
│   │   └── AcceptInviteRequestDTO.java
│   ├── client/
│   │   ├── ClientRequestDTO.java
│   │   └── ClientResponseDTO.java
│   ├── job/
│   │   ├── JobCreateRequestDTO.java
│   │   ├── JobUpdateRequestDTO.java
│   │   ├── JobResponseDTO.java
│   │   ├── JobListResponseDTO.java
│   │   └── BriefingDTO.java
│   ├── kanban/
│   │   ├── KanbanStatusDTO.java
│   │   └── MoveJobRequestDTO.java
│   ├── approval/
│   │   ├── ApprovalPageResponseDTO.java
│   │   ├── ApproveRequestDTO.java
│   │   └── RevisionRequestDTO.java
│   ├── dashboard/
│   │   └── DashboardResponseDTO.java
│   └── common/
│       ├── ErrorResponseDTO.java
│       └── PageResponseDTO.java
│
├── mapper/
│   ├── UserMapper.java
│   ├── ClientMapper.java
│   ├── JobMapper.java
│   ├── MemberMapper.java
│   └── KanbanStatusMapper.java
│
├── enums/
│   ├── UserRole.java
│   ├── MemberRole.java
│   ├── JobType.java
│   ├── JobPriority.java
│   ├── JobStatus.java
│   ├── ApprovalAction.java
│   └── FileType.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── ForbiddenException.java
│   └── FileStorageException.java
│
├── security/
│   ├── JwtService.java
│   ├── JwtFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── SecurityUser.java
│
├── validation/
│   ├── ValidJobType.java
│   └── JobTypeValidator.java
│
└── util/
    ├── DateUtils.java
    ├── SlugUtils.java
    └── FileUtils.java
```

```text
src/main/resources/
│
├── application.yml
├── application-dev.yml
├── application-prod.yml
│
├── db/migration/
│   ├── V1__create_users_table.sql
│   ├── V2__create_workspaces_table.sql
│   ├── V3__create_members_table.sql
│   ├── V4__create_clients_table.sql
│   ├── V5__create_jobs_table.sql
│   ├── V6__create_kanban_statuses_table.sql
│   ├── V7__create_approval_tables.sql
│   └── V8__create_file_tables.sql
│
└── templates/
    └── email/
        ├── invite-member.html
        ├── new-job-assigned.html
        ├── approval-request.html
        ├── job-approved.html
        └── revision-requested.html
```

---

## Exemplos de Código

### ENUM — JobType.java

```java
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

### ENUM — MemberRole.java

```java
public enum MemberRole {

    OWNER,
    MANAGER,
    CREATIVE

}
```

### ENUM — JobPriority.java

```java
public enum JobPriority {

    BAIXA,
    NORMAL,
    ALTA,
    URGENTE

}
```

---

### ENTITY — Job.java

```java
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Member assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private KanbanStatus status;

    @Column(columnDefinition = "jsonb")
    private String briefing;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobFile> files;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters
}
```

### ENTITY — Client.java

```java
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

    // Getters e Setters
}
```

---

### DTO — JobCreateRequestDTO.java

```java
public record JobCreateRequestDTO(

    @NotBlank
    String title,

    @NotNull
    JobType type,

    @NotNull
    JobPriority priority,

    @NotNull
    Long clientId,

    Long assigneeId,

    LocalDate dueDate,

    @NotNull
    BriefingDTO briefing

) {}
```

### DTO — JobResponseDTO.java

```java
public record JobResponseDTO(

    Long id,
    String code,
    String title,
    JobType type,
    JobPriority priority,
    String clientName,
    String assigneeName,
    String statusName,
    Long statusId,
    LocalDate dueDate,
    Boolean isOverdue,
    BriefingDTO briefing,
    List<JobFileResponseDTO> files,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {}
```

### DTO — BriefingDTO.java

```java
public record BriefingDTO(

    String description,
    String copyText,
    String format,
    String colorPalette,
    String references,
    String additionalNotes,
    Integer numberOfSlides,
    String duration,
    String script

) {}
```

---

### REPOSITORY — JobRepository.java

```java
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<Job> findByWorkspaceIdAndStatusId(Long workspaceId, Long statusId);

    List<Job> findByWorkspaceIdAndClientId(Long workspaceId, Long clientId);

    List<Job> findByAssigneeId(Long assigneeId);

    Optional<Job> findByIdAndWorkspaceId(Long id, Long workspaceId);

    Optional<Job> findByCode(String code);

    @Query("SELECT j.status.name, COUNT(j) FROM Job j WHERE j.workspace.id = :workspaceId GROUP BY j.status.name")
    List<Object[]> countByStatusAndWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT j FROM Job j WHERE j.workspace.id = :workspaceId AND j.dueDate < :today AND j.status.name NOT IN ('Aprovado', 'Publicado')")
    List<Job> findOverdueJobs(@Param("workspaceId") Long workspaceId, @Param("today") LocalDate today);

    long countByWorkspaceIdAndClientId(Long workspaceId, Long clientId);

}
```

---

### SERVICE — JobService.java

```java
public interface JobService {

    JobResponseDTO create(JobCreateRequestDTO dto, Long workspaceId);

    JobResponseDTO update(Long jobId, JobUpdateRequestDTO dto, Long workspaceId);

    JobResponseDTO getById(Long jobId, Long workspaceId);

    List<JobListResponseDTO> listByWorkspace(Long workspaceId);

    List<JobListResponseDTO> listByAssignee(Long assigneeId);

    JobResponseDTO moveStatus(Long jobId, MoveJobRequestDTO dto, Long workspaceId);

    void delete(Long jobId, Long workspaceId);

}
```

### SERVICE IMPL — JobServiceImpl.java

```java
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final MemberRepository memberRepository;
    private final KanbanStatusRepository statusRepository;
    private final JobMapper jobMapper;
    private final EmailService emailService;

    public JobServiceImpl(
            JobRepository jobRepository,
            ClientRepository clientRepository,
            MemberRepository memberRepository,
            KanbanStatusRepository statusRepository,
            JobMapper jobMapper,
            EmailService emailService) {

        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.memberRepository = memberRepository;
        this.statusRepository = statusRepository;
        this.jobMapper = jobMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public JobResponseDTO create(JobCreateRequestDTO dto, Long workspaceId) {

        Client client = clientRepository.findByIdAndWorkspaceId(dto.clientId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        KanbanStatus initialStatus = statusRepository.findFirstByWorkspaceIdOrderBySortOrderAsc(workspaceId)
                .orElseThrow(() -> new BusinessException("Kanban not configured"));

        Job job = jobMapper.toEntity(dto);
        job.setClient(client);
        job.setStatus(initialStatus);
        job.setCode(generateJobCode(workspaceId));

        if (dto.assigneeId() != null) {
            Member assignee = memberRepository.findByIdAndWorkspaceId(dto.assigneeId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            job.setAssignee(assignee);
            emailService.sendNewJobAssigned(assignee, job);
        }

        jobRepository.save(job);

        return jobMapper.toResponseDTO(job);
    }

    private String generateJobCode(Long workspaceId) {
        long count = jobRepository.countByWorkspaceId(workspaceId);
        return String.format("JOB-%03d", count + 1);
    }

    // ... demais métodos
}
```

---

### CONTROLLER — JobController.java

```java
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> create(
            @Valid @RequestBody JobCreateRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId) {

        JobResponseDTO response = jobService.create(dto, workspaceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobListResponseDTO>> list(
            @RequestAttribute("workspaceId") Long workspaceId) {

        return ResponseEntity.ok(jobService.listByWorkspace(workspaceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getById(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId) {

        return ResponseEntity.ok(jobService.getById(id, workspaceId));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<JobResponseDTO> moveStatus(
            @PathVariable Long id,
            @Valid @RequestBody MoveJobRequestDTO dto,
            @RequestAttribute("workspaceId") Long workspaceId) {

        return ResponseEntity.ok(jobService.moveStatus(id, dto, workspaceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("workspaceId") Long workspaceId) {

        jobService.delete(id, workspaceId);
        return ResponseEntity.noContent().build();
    }
}
```

---

### MAPPER — JobMapper.java

```java
@Mapper(componentModel = "spring")
public interface JobMapper {

    Job toEntity(JobCreateRequestDTO dto);

    JobResponseDTO toResponseDTO(Job entity);

    JobListResponseDTO toListDTO(Job entity);

}
```

---

### EXCEPTION — GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Business Error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

---

## Fluxo de Dados

```text
Controller
    ↓
Service
    ↓
Mapper
    ↓
Repository
    ↓
PostgreSQL
    ↓
Repository
    ↓
Mapper
    ↓
Service
    ↓
Controller
```

---

## Endpoints da API (Resumo)

### Auth
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/v1/auth/register | Registro de novo usuário + workspace |
| POST | /api/v1/auth/login | Login, retorna JWT |
| POST | /api/v1/auth/refresh | Refresh token |
| POST | /api/v1/auth/logout | Invalida refresh token |

### Workspace & Members
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/v1/workspace | Dados do workspace |
| PUT | /api/v1/workspace | Atualizar workspace |
| POST | /api/v1/members/invite | Convidar membro |
| POST | /api/v1/members/accept | Aceitar convite |
| GET | /api/v1/members | Listar membros |
| DELETE | /api/v1/members/{id} | Remover membro |

### Clients
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/v1/clients | Criar cliente |
| GET | /api/v1/clients | Listar clientes |
| GET | /api/v1/clients/{id} | Detalhe do cliente |
| PUT | /api/v1/clients/{id} | Atualizar cliente |
| PATCH | /api/v1/clients/{id}/toggle | Ativar/desativar |

### Jobs
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/v1/jobs | Criar job |
| GET | /api/v1/jobs | Listar jobs (filtros via query params) |
| GET | /api/v1/jobs/{id} | Detalhe do job |
| PUT | /api/v1/jobs/{id} | Atualizar job |
| PATCH | /api/v1/jobs/{id}/move | Mover status |
| DELETE | /api/v1/jobs/{id} | Deletar job |
| POST | /api/v1/jobs/{id}/files | Upload de arquivo |

### Approval (público, sem auth)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/v1/approval/{token} | Dados da página de aprovação |
| POST | /api/v1/approval/{token}/approve | Aprovar |
| POST | /api/v1/approval/{token}/revision | Solicitar revisão |

### Dashboard
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /api/v1/dashboard | Métricas do dashboard |

---

## Padrões Utilizados

- DTO Pattern (Request/Response com records)
- Repository Pattern (Spring Data JPA)
- Service Layer Pattern (interface + impl)
- Mapper Pattern (MapStruct)
- Enum Pattern
- REST Pattern
- Global Exception Handler
- JWT Stateless Auth
