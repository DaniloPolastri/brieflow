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

## Git Rules

- **No Claude signatures.** Never add `Co-Authored-By: Claude` or any AI attribution to commits or PRs.
- **Human-style commits.** Write commit messages as a human developer would — concise, conventional commits (feat:, fix:, chore:, refactor:, docs:, test:).
- **No AI mentions in PRs.** Do not add "Generated with Claude Code" or similar footers to PR descriptions.

## MVP Scope Boundaries

Only P0 features. Explicitly excluded: WebSockets (use polling), GraphQL (REST only), Redis, Elasticsearch, Kubernetes, micro-frontends, i18n, mobile app, WhatsApp integration, billing/payments, white-label. File storage is local filesystem (no S3). See `docs/MVP-SCOPE (1).md` for the full exclusion list with rationale.
