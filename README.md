# BriefFlow

Plataforma de gestao de producao criativa para agencias de marketing digital. Centraliza briefings estruturados, kanban de producao e aprovacao do cliente em um unico lugar — substituindo o fluxo fragmentado de WhatsApp + Trello + Excel.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Frontend | Angular 20, Standalone Components, Signals, Zoneless, PrimeNG 19+, Tailwind CSS v4 |
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, MapStruct, Flyway |
| Banco de Dados | PostgreSQL 16 |
| Infraestrutura | Docker, Docker Compose, Nginx |

## Pre-requisitos

- [Java 21+](https://adoptium.net/)
- [Node.js 22+](https://nodejs.org/)
- [Docker](https://www.docker.com/) e Docker Compose
- [Angular CLI](https://angular.dev/tools/cli) (`npm install -g @angular/cli`)

## Estrutura do Projeto

```
BriefFlow/
├── backend/         # API REST (Spring Boot)
├── frontend/        # SPA (Angular)
├── nginx/           # Configuracao do reverse proxy
├── docs/            # Especificacoes e documentacao
│   ├── spec/        # Design specs e brainstorms
│   └── plan/        # Planos de implementacao
├── docker-compose.yml       # Dev: PostgreSQL
└── docker-compose.prod.yml  # Prod: stack completa
```

## Desenvolvimento

### 1. Subir o banco de dados

```bash
docker-compose up -d
```

PostgreSQL disponivel em `localhost:5432` (user: `briefflow`, password: `briefflow`, db: `briefflow`).

### 2. Rodar o backend

```bash
cd backend
./mvnw spring-boot:run
```

API disponivel em http://localhost:8080

### 3. Rodar o frontend

```bash
cd frontend
npm install
ng serve
```

App disponivel em http://localhost:4200

### 4. Parar tudo

```bash
docker-compose down
```

## Producao

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

Aplicacao disponivel em http://localhost

## Arquitetura

### Backend

Arquitetura em camadas: Controller → Service → Repository → PostgreSQL, com MapStruct para mapeamento Entity ↔ DTO, JWT stateless para autenticacao, e isolamento multi-tenant por `workspace_id`.

### Frontend

Arquitetura por dominio (feature-based) com lazy loading. Cada feature contem suas pages, components, services e models. Core services (auth, API) sao singletons. Shared components sao reutilizaveis entre features.

## Funcionalidades (MVP)

- Autenticacao com JWT (registro, login, convite de membros)
- Workspace multi-tenant com papeis (Owner, Manager, Creative)
- Gestao de clientes da agencia
- Criacao de jobs com briefing estruturado por tipo
- Kanban de producao com drag & drop
- Upload de pecas (imagens, videos, PDFs — ate 50MB)
- Portal de aprovacao publica via link (sem login do cliente)
- Dashboard com metricas por status, cliente e criativo
- Notificacoes por email
