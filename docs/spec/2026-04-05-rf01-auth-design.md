# RF01 — Autenticacao: Design Spec

**Data:** 2026-04-05
**Status:** Aprovado
**Escopo:** Backend + Frontend — autenticacao completa (sem workspace/membros)

---

## Decisoes

| Decisao | Escolha |
|---------|---------|
| Confirmacao de email | Nao no RF01 — entra com RF09 (Notificacoes Email) |
| Escopo | Backend API + Frontend (login/register) — end-to-end |
| Rate limiting | Bucket4j (5 req/min por IP no login/register) |
| Refresh token storage | PostgreSQL (tabela refresh_tokens) com rotation |
| JWT lib | jjwt (io.jsonwebtoken) |
| Migrations | Flyway desde ja (V1 users, V2 refresh_tokens) |
| Registro | Cria apenas User — Workspace entra no RF02 |

---

## Database Schema

### V1__create_users_table.sql

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
```

### V2__create_refresh_tokens_table.sql

```sql
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

---

## Backend

### Entities

- **User** — JPA entity mapeando tabela `users`. Campos: id, name, email, password, active, createdAt, updatedAt.
- **RefreshToken** — JPA entity com `@ManyToOne(LAZY)` para User. Campos: id, user, token (UUID), expiresAt, revoked, createdAt.

### DTOs (records)

```java
// Request
record RegisterRequestDTO(@NotBlank String name, @Email String email,
                          @Size(min = 8) String password) {}
record LoginRequestDTO(@Email String email, @NotBlank String password) {}
record RefreshTokenRequestDTO(@NotBlank String refreshToken) {}

// Response
record TokenResponseDTO(String accessToken, String refreshToken,
                        long expiresIn, UserInfoDTO user) {}
record UserInfoDTO(Long id, String name, String email) {}
```

### Security Layer

- **JwtService** — Gera access token (HS256, 15min), valida token, extrai claims (userId, email). Usa jjwt.
- **JwtFilter** (OncePerRequestFilter) — Extrai Bearer token do header Authorization, valida via JwtService, seta `UsernamePasswordAuthenticationToken` no SecurityContext.
- **UserDetailsServiceImpl** — Busca User por email no banco para Spring Security.
- **SecurityConfig** — Atualiza filter chain existente: adiciona JwtFilter antes de `UsernamePasswordAuthenticationFilter`.

### Rate Limiting

- Bucket4j com `bucket4j-core` — filtro/anotacao custom
- 5 requests/minuto por IP nos endpoints `/api/v1/auth/login` e `/api/v1/auth/register`
- In-memory (ConcurrentHashMap de buckets por IP)

### Service Layer

**AuthService** (interface) + **AuthServiceImpl**:
- `register(RegisterRequestDTO)` — Valida email unico, hash BCrypt, salva User, gera tokens (access + refresh)
- `login(LoginRequestDTO)` — Valida credenciais, gera tokens
- `refresh(RefreshTokenRequestDTO)` — Valida refresh token no banco, rotation (revoga antigo, cria novo), gera novo access token
- `logout(RefreshTokenRequestDTO)` — Revoga refresh token no banco

### Controller

```
POST /api/v1/auth/register  -> 201 Created + TokenResponseDTO
POST /api/v1/auth/login     -> 200 OK + TokenResponseDTO
POST /api/v1/auth/refresh   -> 200 OK + TokenResponseDTO
POST /api/v1/auth/logout    -> 204 No Content
```

Todos os endpoints sao publicos (sem auth).

---

## Frontend

### Estrutura

```
src/
  core/
    services/
      auth.service.ts          # Estado do usuario (signal), login/logout/register
      storage.service.ts       # LocalStorage wrapper para tokens
    guards/
      auth.guard.ts            # Redireciona para /auth/login se nao autenticado
    interceptors/
      auth.interceptor.ts      # Adiciona Bearer token + auto-refresh em 401
    models/
      user.model.ts            # Interfaces: User, TokenResponse, LoginRequest, RegisterRequest

  features/
    auth/
      pages/
        login/
          login.component.ts     # Reactive form (email, senha), link para registro
        register/
          register.component.ts  # Reactive form (nome, email, senha, confirmar senha)
      auth.routes.ts             # /auth/login, /auth/register

  layout/
    public-layout/
      public-layout.component.ts  # Layout limpo: logo + formulario centralizado
```

### Fluxo

1. Usuario acessa rota protegida -> authGuard redireciona para `/auth/login`
2. Login/Registro -> AuthService chama API -> salva tokens no localStorage -> redireciona para `/dashboard`
3. Requests autenticados -> authInterceptor adiciona `Authorization: Bearer {token}`
4. Token expirado (401) -> interceptor tenta refresh automatico -> se falhar, redireciona para login

### Interceptor — Logica de refresh

- Intercepta resposta 401
- Tenta `POST /auth/refresh` com o refresh token
- Se sucesso: atualiza tokens, re-executa request original
- Se falhar: limpa tokens, redireciona para login
- Queue de requests pendentes enquanto refresh esta em andamento (evita multiplos refresh simultaneos)

### Componentes

- Reactive Forms com validacao (email format, senha minimo 8 chars, confirmar senha match)
- PrimeNG: InputText, Password, Button, Message (erros)
- Layout: logo BriefFlow no topo, card centralizado com formulario
- ChangeDetectionStrategy.OnPush em todos os componentes

### Roteamento

```typescript
// app.routes.ts
{
  path: 'auth',
  component: PublicLayoutComponent,
  children: [{ path: '', loadChildren: () => import('./features/auth/auth.routes') }]
},
{
  path: '',
  canActivate: [authGuard],
  children: [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', loadComponent: () => ... }  // placeholder
  ]
},
{ path: '**', redirectTo: 'auth/login' }
```

Dashboard placeholder: pagina simples "Bem-vindo ao BriefFlow" com dados do usuario logado. Substituida em RFs futuros.

authGuard: redireciona para `/auth/login` se sem token. Rotas auth: se ja autenticado, redireciona para `/dashboard`.

---

## Testes

### Backend (JUnit 5 + Mockito + Testcontainers)

| Camada | O que testar | Tipo |
|--------|-------------|------|
| AuthServiceImpl | register (sucesso, email duplicado), login (sucesso, credenciais invalidas), refresh (rotation, token expirado/revogado), logout | Unit (Mockito) |
| JwtService | geracao de token, validacao, extracao de claims, token expirado | Unit |
| UserRepository | findByEmail, save | Integration (Testcontainers) |
| RefreshTokenRepository | findByToken, revogar | Integration (Testcontainers) |
| AuthController | todos os endpoints com MockMvc | Integration (WebMvcTest) |
| Rate limiting | 5 requests OK, 6o bloqueado | Integration |

### Frontend (Vitest)

| Arquivo | O que testar |
|---------|-------------|
| auth.service.spec.ts | login/register/logout chamam API certa, armazena tokens |
| auth.interceptor.spec.ts | adiciona header, refresh em 401, redireciona em falha |
| auth.guard.spec.ts | permite com token, redireciona sem token |
| login.component.spec.ts | validacao do form, chama service, exibe erro |
| register.component.spec.ts | validacao do form, confirmar senha, chama service |

---

## Fora do Escopo (RF01)

- Confirmacao de email (RF09)
- Workspace e membros (RF02)
- Role-based guards (RF02 — apos ter Member/Role)
- Layout principal com sidebar/topbar (RF futuro)
- Recuperacao de senha / forgot password
