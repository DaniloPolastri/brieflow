# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BriefFlow is a B2B SaaS platform for creative production management in digital marketing agencies. It centralizes structured briefings, production kanban, and client approval — replacing fragmented WhatsApp + Trello + Excel workflows. Target: small/medium agencies (2-15 people) in Brazil. Portuguese-only UI (no i18n).

## Project Status

**Pre-implementation.** The `docs/` folder contains specifications only — no source code exists yet. Reference these docs when building:

- `docs/PRD (1).md` — Full product requirements
- `docs/BRIEF (1).md` — Executive summary, stack, timeline
- `docs/MVP-SCOPE (1).md` — MVP feature scope (P0/P1/P2), personas, critical flows, simplification decisions
- `docs/BACKEND-STRUCTURE.md` — Backend architecture, code examples, API endpoints
- `docs/FRONTEND-STRUCTURE.md` — Frontend architecture, code examples, conventions
- `docs/DESIGN-GUIDELINES (1).md` — Design system (colors, typography, spacing, component specs)
- `docs/LANDING-PAGE-SPEC.md` — Public landing page design

## Tech Stack

| Layer | Tech |
|-------|------|
| Frontend | Angular 20 + Standalone + Zoneless + CSR + PrimeNG 19+ (Aura) + Tailwind CSS v4 |
| Backend | Java 21+ + Spring Boot 3+ + Spring Security + Spring Data JPA + MapStruct + Flyway |
| Database | PostgreSQL |
| Testing | Vitest (frontend), JUnit (backend) |
| Deploy | Docker + Docker Compose + Nginx |

## Build & Run Commands

### Backend (Spring Boot)
```bash
./mvnw spring-boot:run                    # Run dev server
./mvnw test                               # Run all tests
./mvnw test -Dtest=ClassName              # Run single test class
./mvnw test -Dtest=ClassName#methodName   # Run single test method
./mvnw clean package                      # Build JAR
```

### Frontend (Angular)
```bash
ng serve                    # Dev server (http://localhost:4200)
ng test                     # Run tests (Vitest)
ng build                    # Production build
ng generate component <path> --standalone   # Generate component
```

### Docker
```bash
docker-compose up -d        # Start all services (backend + postgres + nginx)
docker-compose down         # Stop all services
```

## Architecture

### Backend — Layered Architecture

```
Controller → Service (interface) → ServiceImpl → Repository → PostgreSQL
                                 → Mapper (MapStruct) for Entity ↔ DTO
```

- **Package root:** `com.briefflow`
- **Patterns:** Service interface + Impl, DTO records (request/response), MapStruct mappers, `@RestControllerAdvice` global exception handler
- **Auth:** JWT (access 15min + refresh 7 days), stateless Spring Security, `@RequestAttribute("workspaceId")` for multi-tenant isolation
- **API base:** `/api/v1/` — public endpoints: `/auth/*`, `/approval/{token}/*`
- **Migrations:** Flyway in `src/main/resources/db/migration/` (V1 through V8)
- **Profiles:** `application.yml`, `application-dev.yml`, `application-prod.yml`

### Frontend — Feature-Based Architecture

```
src/
├── core/        # Singletons: AuthService, ApiService, guards, interceptors (never depends on features/shared)
├── shared/      # Reusable components, directives, pipes (used by features and layout)
├── features/    # Domain modules: auth, clients, jobs, kanban, dashboard, members, approval, settings
│   └── <feature>/  → pages/, components/, services/, models/, <feature>.routes.ts
├── layout/      # MainLayout, Sidebar, Topbar, PublicLayout (uses shared and core)
└── environments/
```

**Dependency rules:** `core` → independent | `shared` → used by features/layout | `features` → independent from each other | `layout` → uses shared + core

### Angular Conventions

- Standalone components only (no NgModules)
- `input()` / `output()` / `computed()` / `signal()` — not decorators
- `inject()` — not constructor injection
- `@if` / `@for` / `@switch` — not structural directives
- `ChangeDetectionStrategy.OnPush` on all components
- Reactive Forms (not template-driven)
- Lazy loading for all feature routes
- Vitest with global functions (no imports needed)

### Spring Boot Conventions

- Service interface + `@Service` Impl class
- DTOs as Java `record` types
- `@Mapper(componentModel = "spring")` for MapStruct
- `@Transactional` on service methods that modify data
- `FetchType.LAZY` on all `@ManyToOne` relations
- Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`) on DTOs
- Custom exceptions: `ResourceNotFoundException`, `BusinessException`, `UnauthorizedException`, `ForbiddenException`

## Domain Model

**Core entities:** User → Workspace (multi-tenant) → Member (OWNER/MANAGER/CREATIVE) → Client → Job → JobFile, JobBriefing, KanbanStatus, ApprovalToken, ApprovalHistory

**Key enums:** `JobType` (POST_FEED, STORIES, CARROSSEL, REELS_VIDEO, BANNER, LOGO, OUTROS), `JobPriority` (BAIXA, NORMAL, ALTA, URGENTE), `MemberRole` (OWNER, MANAGER, CREATIVE)

**Multi-tenancy:** All queries filter by `workspace_id`. Each workspace is fully isolated.

## Critical Flows

1. **Job lifecycle:** Manager creates job with structured briefing → Creative receives email → Creative executes externally → Uploads deliverable → Manager moves to "Awaiting Approval" → System generates approval token link → Client approves or requests revision via public portal (no login)
2. **Kanban statuses:** Novo → Em Criação → Revisão → Aguardando Aprovação → Aprovado (default pipeline)

## Design System

- **Primary:** `#6366F1` (Indigo-500). Semantic: green/amber/red/blue for success/warning/danger/info
- **Typography:** Inter (body), JetBrains Mono (codes/IDs)
- **Spacing:** 4px base unit
- **References:** Operand (design), Linear (UX), Resend (clean)
- Light mode only. Desktop-first, responsive for tablet.

## Document Organization

- **`docs/spec/`** — All brainstorming and design spec outputs (from superpowers:brainstorming skill). Always save brainstorm results here.
- **`docs/plan/`** — All implementation plans (from superpowers:writing-plans skill). Always save plans here.
- **`docs/`** — Original project specifications (PRD, MVP scope, architecture, design guidelines).

### Checklist de Progresso nos Planos

Todo plano de implementação DEVE ter um checklist `[ ]` / `[x]` na seção Task Summary. Regras:
- Cada task do plano tem um checkbox: `- [ ] Task N: Descrição`
- Ao **completar** cada task durante a execução, marcar imediatamente como `- [x] Task N: Descrição` no arquivo do plano e fazer commit.
- **Nunca** deixar para marcar tudo no final. Marcar task por task, simultaneamente com a codificação.
- Isso garante que se a sessão for interrompida, o plano reflete exatamente onde parou.

### Paralelismo nas Tasks do Plano

Todo plano DEVE identificar quais tasks podem ser executadas em paralelo. Regras:

- Na seção Task Summary, agrupar tasks paralelizáveis com marcação explícita:
  ```
  - [ ] Task 1: Setup do projeto (sequencial — base para tudo)
  - [ ] Task 2: Entity User + Repository ⚡ PARALLEL GROUP A
  - [ ] Task 3: Entity Workspace + Repository ⚡ PARALLEL GROUP A
  - [ ] Task 4: Auth Service (sequencial — depende de Task 2)
  - [ ] Task 5: User Controller ⚡ PARALLEL GROUP B
  - [ ] Task 6: Workspace Controller ⚡ PARALLEL GROUP B
  ```
- Tasks no mesmo `PARALLEL GROUP` não têm dependência entre si e DEVEM ser executadas em paralelo usando a skill `dispatching-parallel-agents` ou lançando múltiplos Agent tool calls simultaneamente.
- Tasks sem marcação `⚡ PARALLEL GROUP` são sequenciais e devem ser executadas na ordem.
- **Na escrita do plano** (`writing-plans`): o autor DEVE analisar o grafo de dependências das tasks e marcar os grupos paralelos. Se todas as tasks forem sequenciais, declarar explicitamente: "Nenhuma task paralelizável — todas têm dependência sequencial."
- **Na execução do plano** (`executing-plans`): SEMPRE despachar tasks paralelas ao mesmo tempo via agentes. Nunca executar sequencialmente tasks que estão marcadas como paralelas.
- Cada agente paralelo trabalha em worktree isolado quando há risco de conflito de arquivos. Caso contrário, pode trabalhar no mesmo workspace se os arquivos são independentes.

## Git Rules

- **No Claude signatures.** Never add `Co-Authored-By: Claude` or any AI attribution to commits or PRs.
- **Human-style commits.** Write commit messages as a human developer would — concise, conventional commits (feat:, fix:, chore:, refactor:, docs:, test:).
- **No AI mentions in PRs.** Do not add "Generated with Claude Code" or similar footers to PR descriptions.
- **Branch naming:**
  - Features: `feature/<nome-descritivo>` (ex: `feature/auth-jwt`, `feature/kanban-board`)
  - Bug fixes: `fix/<nome-descritivo>` (ex: `fix/token-refresh-loop`)
  - Sempre criar branch nova antes de executar um plano de implementação.

## Superpowers Workflow

### Fluxo de Implementação (features)

```
1. brainstorming          → Explorar ideia, definir design, salvar em docs/spec/
2. writing-plans          → Criar plano de implementação (TDD), salvar em docs/plan/
3. executing-plans        → Criar branch feature/*, executar task por task (test-first)
4. requesting-code-review → Ao final da execução, solicitar code review
5. receiving-code-review  → Processar feedback do review com rigor técnico
6. finishing-a-development-branch → Decidir merge, PR ou cleanup
```

**Regras:**
- Toda execução de plano DEVE começar criando uma branch `feature/<nome>` a partir de `main`.
- Ao terminar a execução do plano, SEMPRE invocar `requesting-code-review` antes de qualquer merge/PR.
- Após receber o review, usar `receiving-code-review` para processar o feedback.
- Finalizar com `finishing-a-development-branch` para decidir como integrar o trabalho.
- Nunca pular etapas. Cada implementação passa pelo fluxo completo.

### Fluxo de Correção de Bugs

- **Bug simples** (causa óbvia, fix direto): corrigir diretamente, sem skill especial.
- **Bug complexo** (precisa investigação, causa não óbvia, múltiplos arquivos):
  1. `systematic-debugging` → Investigar root cause com método estruturado (reproduzir → isolar → diagnosticar → corrigir)
  2. `verification-before-completion` → Verificar que o fix realmente resolve o problema, rodar testes, confirmar que nada quebrou
- Criar branch `fix/<nome>` para bugs que precisam de debugging complexo.

## Test-Driven Development (TDD)

Este projeto segue TDD como padrão. Toda implementação de feature ou bugfix segue o ciclo:

```
Red → Green → Refactor
1. Escrever o teste que falha
2. Implementar o mínimo para o teste passar
3. Refatorar mantendo os testes verdes
```

**Usar a skill `test-driven-development` em toda implementação de código.**

### Backend — Estrutura de Testes (JUnit 5 + Mockito + Testcontainers)

```
backend/src/test/java/com/briefflow/
├── unit/                        # Testes unitários (mocks, rápidos)
│   ├── service/                 # Testa lógica de negócio isolada
│   │   └── JobServiceImplTest.java
│   ├── mapper/                  # Testa mapeamentos Entity ↔ DTO
│   │   └── JobMapperTest.java
│   └── security/                # Testa JWT, filters
│       └── JwtServiceTest.java
├── integration/                 # Testes de integração (banco real via Testcontainers)
│   ├── repository/              # Testa queries JPA com PostgreSQL real
│   │   └── JobRepositoryTest.java
│   └── controller/              # Testa endpoints REST completos
│       └── JobControllerTest.java
└── e2e/                         # Testes end-to-end de fluxos completos
    └── JobLifecycleTest.java
```

**Convenções backend:**
- Testes unitários: `@ExtendWith(MockitoExtension.class)`, mocks para dependências
- Testes de integração: `@SpringBootTest` + `@Testcontainers` com PostgreSQL real
- Testes de controller: `@WebMvcTest` + `MockMvc` para unitário, `@SpringBootTest` + `TestRestTemplate` para integração
- Nomenclatura: `should_expectedBehavior_when_condition()` (ex: `should_createJob_when_validRequest()`)
- Cada service DEVE ter testes unitários. Cada controller DEVE ter testes de integração.

### Frontend — Estrutura de Testes (Vitest)

```
frontend/src/
├── core/
│   └── services/
│       ├── auth.service.ts
│       └── auth.service.spec.ts       # Teste junto do arquivo
├── shared/
│   └── components/
│       └── status-badge/
│           ├── status-badge.component.ts
│           └── status-badge.component.spec.ts
├── features/
│   └── jobs/
│       ├── services/
│       │   ├── job-api.service.ts
│       │   └── job-api.service.spec.ts
│       ├── pages/
│       │   └── job-create/
│       │       ├── job-create.component.ts
│       │       └── job-create.component.spec.ts
│       └── components/
│           └── briefing-form/
│               ├── briefing-form.component.ts
│               └── briefing-form.component.spec.ts
```

**Convenções frontend:**
- Arquivo de teste ao lado do arquivo fonte: `*.spec.ts`
- Vitest com funções globais (`describe`, `it`, `expect` — sem imports)
- Usar `TestBed` para testes de componentes Angular
- Mock de HttpClient com `provideHttpClientTesting()`
- Nomenclatura: `it('should do X when Y')` (ex: `it('should display error when login fails')`)
- Cada service DEVE ter testes. Componentes com lógica DEVEM ter testes.

## MVP Scope Boundaries

Only P0 features. Explicitly excluded: WebSockets (use polling), GraphQL (REST only), Redis, Elasticsearch, Kubernetes, micro-frontends, i18n, mobile app, WhatsApp integration, billing/payments, white-label. File storage is local filesystem (no S3). See `docs/MVP-SCOPE (1).md` for the full exclusion list with rationale.
