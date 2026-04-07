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

## Consulta de Documentação (context7)

Sempre usar o plugin **context7** (`context7-plugin:docs` / `context7-plugin:documentation-lookup`) para consultar documentação de bibliotecas, frameworks e ferramentas antes de implementar. Isso inclui:
- APIs do Angular, PrimeNG, Tailwind CSS, Spring Boot, Spring Security, Flyway, MapStruct, etc.
- Sintaxe, configuração, migrations, breaking changes entre versões
- Quando houver dúvida sobre uso correto de qualquer dependência do projeto

**Regra:** Não confiar apenas no conhecimento de treinamento — a documentação pode ter mudado. Consultar context7 primeiro, especialmente para Angular 20, PrimeNG 19+ e Tailwind v4 que são versões recentes.

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
- **Path Aliases** — Usar aliases do `tsconfig.json` em vez de caminhos relativos longos. Regras:
  - Se o import tem **2 ou mais níveis de `../`** (ex: `../../services/foo`), DEVE usar alias
  - Imports relativos curtos (`./` ou `../`) dentro do mesmo feature/módulo são aceitáveis
  - Aliases padrão:
    - `@core/*` → `src/core/*`
    - `@shared/*` → `src/shared/*`
    - `@features/*` → `src/features/*`
    - `@layout/*` → `src/layout/*`
    - `@env/*` → `src/environments/*`
  - Se o alias necessário **não existir** no `tsconfig.json`, criar antes de usar (adicionar em `compilerOptions.paths`)

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

### Estrutura de Tasks no Plano (camadas + paralelismo)

Todo plano DEVE separar tasks por camada e marcar paralelismo. Formato obrigatório:

```
## Task Summary

### Backend
- [ ] Task B1: Entity User + Repository ⚡ PARALLEL GROUP A
- [ ] Task B2: Entity Workspace + Repository ⚡ PARALLEL GROUP A
- [ ] Task B3: Auth Service (sequencial — depende de B1)
- [ ] Task B4: Auth Controller + endpoints (depende de B3)

### Frontend
- [ ] Task F1: AuthService + HTTP interceptor (depende de B4 backend)
- [ ] Task F2: Login page ⚡ PARALLEL GROUP B
- [ ] Task F3: Register page ⚡ PARALLEL GROUP B
- [ ] Task F4: Auth guard + redirect (depende de F1)
```

**Regras de organização:**
- Prefixo `B` para backend, `F` para frontend — facilita referência cruzada
- Backend vem primeiro (APIs precisam existir antes do frontend consumir)
- Dependências cross-layer são explícitas (ex: "depende de B4 backend")
- Se a feature toca apenas uma camada, usar apenas a seção relevante

**Regras de paralelismo:**
- Tasks no mesmo `⚡ PARALLEL GROUP` não têm dependência entre si e DEVEM ser executadas em paralelo via agentes
- Tasks sem marcação `⚡` são sequenciais e devem ser executadas na ordem
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

## Self-Improvement Loop

Este projeto usa um arquivo `docs/lessons.md` como memória persistente de erros e aprendizados. Objetivo: **zero erros repetidos entre sessões.**

### Como funciona

1. **Capturar:** Após QUALQUER correção do usuário (erro de implementação, padrão errado, abordagem rejeitada), registrar imediatamente em `docs/lessons.md`
2. **Prevenir:** Cada entrada deve ser uma **regra acionável** que previna o mesmo erro no futuro — não apenas uma descrição do que aconteceu
3. **Revisar:** No início de cada sessão de implementação, ler `docs/lessons.md` antes de começar a codar
4. **Iterar:** Atualizar e refinar as lições conforme o projeto evolui. Remover lições que não se aplicam mais

### Formato de `docs/lessons.md`

```markdown
# Lessons Learned

## [Categoria] — Título curto da lição
- **Erro:** O que foi feito errado
- **Regra:** O que fazer em vez disso (imperativo, direto)
- **Contexto:** Onde/quando essa regra se aplica

## [Angular] — Não usar decorators legados
- **Erro:** Usei `@Input()` decorator em vez de `input()` signal
- **Regra:** Sempre usar `input()`, `output()`, `signal()`, `computed()` — nunca decorators
- **Contexto:** Todos os componentes Angular do projeto

## [Spring] — FetchType padrão
- **Erro:** Esqueci de colocar `FetchType.LAZY` em `@ManyToOne`
- **Regra:** TODO `@ManyToOne` e `@OneToMany` DEVE ter `fetch = FetchType.LAZY`
- **Contexto:** Todas as entities JPA
```

### Regras

- **Registrar na hora.** Não esperar o fim da sessão — registrar assim que o erro for corrigido
- **Uma lição por erro.** Cada entrada é atômica e independente
- **Regra, não narrativa.** A entrada deve ser imperativa ("Sempre fazer X", "Nunca fazer Y"), não descritiva ("Aconteceu X")
- **Categorizar.** Usar categorias como `[Angular]`, `[Spring]`, `[JPA]`, `[Git]`, `[TDD]`, `[PrimeNG]`, `[Tailwind]`, `[Docker]`, `[API]`, `[Geral]`
- **Sem duplicatas.** Antes de adicionar, verificar se já existe uma lição similar. Se existir, refinar a existente
- **Limpeza periódica.** Se uma lição foi incorporada no CLAUDE.md como regra formal, remover de `docs/lessons.md` para evitar redundância

## Superpowers Workflow

### Fluxo de Implementação (features)

```
1. brainstorming          → Explorar ideia, definir design, salvar em docs/spec/
2. writing-plans          → Criar plano via agente orquestrador (ver abaixo), salvar em docs/plan/
3. executing-plans        → Criar branch feature/*, executar task por task (test-first)
4. requesting-code-review → Ao final da execução, solicitar code review
5. receiving-code-review  → Processar feedback do review com rigor técnico
6. finishing-a-development-branch → Decidir merge, PR ou cleanup
```

**Regras:**
- Toda execução de plano DEVE começar criando uma branch `feature/<nome>` a partir de `main`.
- **OBRIGATÓRIO — Marcar progresso no plano em tempo real:** Ao completar cada task, marcar IMEDIATAMENTE `[x]` no arquivo do plano (`docs/plan/`) e fazer commit. NÃO esperar a próxima task, NÃO acumular marcações. Isso é a garantia de retomada se a sessão for interrompida. Violação recorrente — esta é regra formal e inegociável.
- Ao terminar a execução do plano, SEMPRE invocar `requesting-code-review` antes de qualquer merge/PR.
- Após receber o review, usar `receiving-code-review` para processar o feedback.
- Finalizar com `finishing-a-development-branch` para decidir como integrar o trabalho.
- Nunca pular etapas. Cada implementação passa pelo fluxo completo.

### Code Review → Extração de Lições (obrigatório)

Durante o `requesting-code-review`, o agente de review DEVE incluir uma seção final de **análise de lições** no output. Essa análise verifica:

1. **Erros recorrentes:** Padrões que violam convenções do CLAUDE.md (ex: decorator legado, FetchType.EAGER, constructor injection)
2. **Inconsistências:** Diferenças entre o que foi planejado e o que foi implementado que indicam confusão ou desconhecimento
3. **Padrões frágeis:** Código que funciona mas é propenso a quebrar (ex: hardcoded values, falta de validação em boundary, queries N+1)
4. **Desvios de arquitetura:** Violações das regras de dependência entre camadas (core/shared/features/layout)

**Formato da seção no review:**

```markdown
## Lições Identificadas

### Novas lições para `docs/lessons.md`
- **[Categoria] — Título:** Descrição do erro + regra acionável
- ...

### Lições existentes reforçadas
- Lição X foi violada novamente → considerar promover para CLAUDE.md como regra formal

### Nenhuma lição identificada
(se o código seguiu todas as convenções corretamente)
```

**Regras:**
- O reviewer DEVE ler `docs/lessons.md` antes de revisar, para verificar se erros passados se repetiram
- Se identificar novas lições, registrar em `docs/lessons.md` imediatamente após o review
- Se uma lição existente foi violada pela 3ª vez, propor sua promoção para regra formal no CLAUDE.md
- Lições devem ser **acionáveis e preventivas**, não descritivas (seguir o formato existente de lessons.md)

### Escrita de Planos — Agent Team Orquestrador (obrigatório)

Toda invocação de `writing-plans` DEVE usar **Agent Teams** (TeamCreate + teammates). Isso é obrigatório, sem exceções.

**Arquitetura da equipe:**

```
Team Lead (você) — cria a equipe, atribui tasks, consolida o plano final
├── Teammate "backend-architect"  → Analisa spec, propõe entities, services, endpoints, migrations, testes
├── Teammate "frontend-architect" → Analisa spec, propõe components, pages, services, routes, testes
└── Team Lead consolida           → Unifica os dois planos, resolve dependências cruzadas, salva em docs/plan/
```

**Como executar:**

1. **Criar a equipe** via `TeamCreate`:
   ```
   team_name: "plan-<nome-da-feature>"
   description: "Planejamento de implementação para <feature>"
   ```

2. **Criar tasks na task list compartilhada** via `TaskCreate`:
   - Task 1: "Arquitetura Backend — analisar spec e propor entities, services, endpoints, DTOs, migrations e lista de tasks com testes"
   - Task 2: "Arquitetura Frontend — analisar spec e propor pages, components, services, routes e lista de tasks com testes"
   - Task 3: "Consolidar plano unificado" (depende de Task 1 e 2, atribuída ao Team Lead)

3. **Spawnar 2 teammates em paralelo** via Agent tool com `team_name`:
   - **`backend-architect`**: recebe a spec + instrução para ler `docs/BACKEND-STRUCTURE.md` + contexto do domínio. Deve produzir: entities envolvidas, endpoints, services, DTOs, migrations, e lista de tasks backend com testes. Ao finalizar, marcar sua task como completed.
   - **`frontend-architect`**: recebe a spec + instrução para ler `docs/FRONTEND-STRUCTURE.md` + `docs/DESIGN-GUIDELINES (1).md` + contexto do domínio. Deve produzir: pages, components, services, routes, e lista de tasks frontend com testes. Ao finalizar, marcar sua task como completed.

4. **Aguardar teammates** — as mensagens dos teammates chegam automaticamente quando terminam (não fazer polling). Esperar ambos completarem antes de prosseguir.

5. **Consolidar** o plano unificado com:
   - Task Summary com checklist `[ ]` e marcação de `⚡ PARALLEL GROUP` onde aplicável
   - Dependências claras entre tasks backend e frontend (ex: "Task 5 frontend depende de Task 3 backend")
   - Ordem de execução respeitando: backend-first para APIs que o frontend consome

6. **Salvar** o plano consolidado em `docs/plan/`

7. **Encerrar teammates** via SendMessage com `shutdown_request` e depois **limpar a equipe** via `TeamDelete`.

**Requer:** `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` no settings (já configurado).

**Por que:** Planos escritos por um único agente tendem a ser superficiais em uma das camadas. Teammates especializados com context window própria produzem análises mais profundas, e a task list compartilhada garante coordenação sem conflitos.

### Fluxo de Correção de Bugs

- **Bug simples** (causa óbvia, fix direto): corrigir diretamente, sem skill especial.
- **Bug complexo** (precisa investigação, causa não óbvia, múltiplos arquivos):
  1. `systematic-debugging` → Investigar root cause com método estruturado (reproduzir → isolar → diagnosticar → corrigir)
  2. `verification-before-completion` → Verificar que o fix realmente resolve o problema, rodar testes, confirmar que nada quebrou
- Criar branch `fix/<nome>` para bugs que precisam de debugging complexo.

### Frontend Design (skill obrigatória)

Toda task que envolva criação ou modificação de interface frontend DEVE invocar a skill `frontend-design` antes de implementar. Isso inclui:
- Criar componentes visuais (páginas, modais, formulários, dashboards, cards, etc.)
- Alterar layout, estilo ou estrutura visual de componentes existentes
- Implementar telas descritas em specs ou planos
- Qualquer trabalho que resulte em mudanças visíveis ao usuário na UI

**Regra:** Se a task toca HTML/template/estilo de componente Angular → invocar `frontend-design`. Sem exceções.

### Visual Companion — Visualização no Browser (obrigatório)

Durante brainstorming e refino de telas, SEMPRE usar o **Visual Companion** para renderizar previews no browser. Isso inclui:
- Brainstorming de design de telas (`brainstorming` skill) — ao definir layout, componentes visuais ou estilo
- Refino de UI durante `frontend-design` — ao iterar sobre a aparência de componentes
- Qualquer momento em que se discute ou decide sobre design visual de interfaces

**Regra:** Não aprovar design de tela sem antes visualizar no browser via Visual Companion. O feedback visual é obrigatório para validar decisões de layout, espaçamento, cores e hierarquia antes de partir para implementação.

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
