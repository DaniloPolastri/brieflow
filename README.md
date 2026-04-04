# BriefFlow

Plataforma de gestao de producao criativa para agencias de marketing. Centraliza briefings, kanban de producao e aprovacao do cliente.

## Pre-requisitos

- Java 21+
- Node.js 22+
- Docker e Docker Compose
- Angular CLI (`npm install -g @angular/cli`)

## Desenvolvimento

### 1. Subir o banco de dados

```bash
docker-compose up -d
```

### 2. Rodar o backend

```bash
cd backend
./mvnw spring-boot:run
```

API disponivel em http://localhost:8080

### 3. Rodar o frontend

```bash
cd frontend
ng serve
```

App disponivel em http://localhost:4200

## Producao

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

App disponivel em http://localhost
