# BriefFlow — Technical Setup Design

**Data:** 2026-04-03
**Status:** Aprovado
**Escopo:** Setup técnico inicial do projeto (sem features)

---

## Decisões

| Decisão | Escolha |
|---------|---------|
| Repositório | Monorepo (`backend/` + `frontend/`) |
| Backend | Java 21 + Spring Boot 3 + Maven |
| Frontend | Angular 20 + Standalone + Zoneless + CSR |
| UI | PrimeNG 19+ (Aura theme) + Tailwind CSS v4 |
| Banco | PostgreSQL 16 via Docker |
| Pacotes | npm (frontend), Maven (backend) |
| Lombok | Sim — entities com Lombok, DTOs com records |
| Docker dev | Apenas PostgreSQL |
| Docker prod | PostgreSQL + Backend + Frontend + Nginx |
| CI/CD | Não agora |
| SSR | Não |
| Abordagem | Híbrida — CLI scaffold + estrutura manual |

---

## Estrutura do Repositório

```
BriefFlow/
├── backend/                  # Spring Boot 3 + Java 21 (Maven)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/briefflow/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   ├── Dockerfile
│   └── .mvn/
├── frontend/                 # Angular 20 (standalone, zoneless, CSR)
│   ├── src/
│   │   ├── app/
│   │   ├── core/
│   │   ├── shared/
│   │   ├── features/
│   │   ├── layout/
│   │   ├── environments/
│   │   └── assets/
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── Dockerfile
├── docker-compose.yml        # Dev: apenas PostgreSQL
├── docker-compose.prod.yml   # Prod: PostgreSQL + Backend + Frontend + Nginx
├── nginx/
│   └── nginx.conf
├── docs/
│   ├── spec/
│   ├── plan/
│   └── (specs existentes)
├── CLAUDE.md
├── .gitignore
└── README.md
```

---

## Backend — Spring Boot

### Dependências (Spring Initializr)

- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- Flyway Migration
- Validation (Bean Validation)
- Mail (JavaMailSender)
- Lombok
- MapStruct

### Estrutura de Pastas

```
src/main/java/com/briefflow/
├── BriefflowApplication.java
├── config/          # SecurityConfig, CorsConfig, SwaggerConfig, WebConfig, MailConfig
├── controller/      # REST endpoints (vazio no setup)
├── service/
│   └── impl/
├── repository/
├── entity/
├── dto/             # Records por domínio (auth/, job/, client/, etc.)
├── mapper/          # MapStruct mappers
├── enums/
├── exception/       # GlobalExceptionHandler + custom exceptions
├── security/        # JwtService, JwtFilter, UserDetailsServiceImpl
├── validation/
└── util/

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── db/migration/
└── templates/email/
```

### O que o setup cria (funcional)

- `BriefflowApplication.java` — entry point
- `SecurityConfig.java` — esqueleto JWT stateless, CORS, endpoints públicos
- `CorsConfig.java` — origens permitidas por profile
- `GlobalExceptionHandler.java` — handler com ResourceNotFoundException, BusinessException
- `application.yml` / `application-dev.yml` — conexão PostgreSQL, configs JWT
- Pastas vazias com `.gitkeep`

---

## Frontend — Angular 20

### Scaffold

`ng new frontend` com flags:
- `--standalone` (padrão Angular 20)
- `--style=css`
- `--ssr=false` (CSR puro)
- `--zoneless`

### Dependências adicionais

- `primeng` (v19+ latest)
- `@primeng/themes` (Aura theme)
- `tailwindcss` + `@tailwindcss/postcss` (v4)
- `primeicons`

### Estrutura de Pastas

```
src/
├── main.ts
├── index.html
├── styles.css                 # Tailwind imports + PrimeNG Aura theme
├── app/
│   ├── app.config.ts          # Providers: router, httpClient, animations, PrimeNG
│   ├── app.routes.ts          # Rotas com lazy loading
│   └── app.component.ts
├── core/
│   ├── services/
│   ├── guards/
│   ├── interceptors/
│   └── models/
├── shared/
│   ├── components/
│   ├── directives/
│   ├── pipes/
│   └── utils/
├── features/
│   ├── auth/
│   ├── clients/
│   ├── jobs/
│   ├── kanban/
│   ├── dashboard/
│   ├── members/
│   ├── approval/
│   └── settings/
├── layout/
│   ├── main-layout/
│   ├── sidebar/
│   ├── topbar/
│   └── public-layout/
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
└── assets/
```

### O que o setup cria (funcional)

- `app.config.ts` — providers configurados (router, httpClient, animations, PrimeNG)
- `app.routes.ts` — esqueleto de rotas com lazy loading
- `app.component.ts` — root component mínimo
- `styles.css` — Tailwind imports + PrimeNG Aura theme preset
- `environment.ts` / `environment.prod.ts` — apiUrl configurado
- Pastas vazias com `.gitkeep`

---

## Docker Compose

### `docker-compose.yml` (dev)

- PostgreSQL 16 na porta 5432
- Volume persistente `postgres_data`
- Credenciais: `briefflow` / `briefflow` / `briefflow`
- Health check com `pg_isready`

### `docker-compose.prod.yml` (prod)

- PostgreSQL 16 (sem porta exposta, rede interna)
- Backend (Dockerfile multi-stage: Maven build → JRE 21 runtime)
- Frontend (Dockerfile multi-stage: Node build → Nginx serve)
- Nginx reverse proxy (porta 80):
  - `/api/*` → backend:8080
  - `/*` → frontend static files

### Dockerfiles

- `backend/Dockerfile` — multi-stage (Maven build → JRE 21 slim)
- `frontend/Dockerfile` — multi-stage (Node + ng build → Nginx)

---

## Configurações

### `application.yml`

```yaml
spring.application.name: briefflow
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
jwt.secret: ${JWT_SECRET}
jwt.access-expiration: 900000        # 15 min
jwt.refresh-expiration: 604800000    # 7 dias
file.upload-dir: ./uploads
file.max-size: 52428800              # 50MB
```

### `application-dev.yml`

```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/briefflow
spring.datasource.username: briefflow
spring.datasource.password: briefflow
spring.jpa.show-sql: true
jwt.secret: dev-secret-key-min-256-bits-for-hmac-sha256
cors.allowed-origins: http://localhost:4200
```

### `application-prod.yml`

```yaml
spring.datasource.url: ${DATABASE_URL}
spring.datasource.username: ${DATABASE_USER}
spring.datasource.password: ${DATABASE_PASSWORD}
spring.jpa.show-sql: false
jwt.secret: ${JWT_SECRET}
cors.allowed-origins: ${CORS_ORIGINS}
```

---

## O que o setup entrega funcionando

1. `docker-compose up -d` → PostgreSQL rodando
2. `cd backend && ./mvnw spring-boot:run` → API rodando em :8080 (Security esqueleto, CORS, exception handler)
3. `cd frontend && ng serve` → App rodando em :4200 (PrimeNG + Tailwind configurados, rotas esqueleto)
4. Estrutura de pastas completa pronta para receber features

## O que NÃO entrega

- Nenhuma feature implementada (auth, jobs, kanban, etc.)
- Nenhuma migration Flyway
- Nenhum componente UI
