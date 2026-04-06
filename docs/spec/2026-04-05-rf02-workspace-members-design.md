# RF02 — Workspace e Membros: Design Spec

## Objetivo

Implementar multi-tenancy via workspaces, com criacao automatica no registro, convite de membros por link, papeis de permissao (OWNER/MANAGER/CREATIVE) e cargos funcionais fixos.

---

## Decisoes de Design

| Decisao | Escolha | Motivo |
|---------|---------|--------|
| Nome da agencia no registro | Campo obrigatorio no formulario de registro (4 campos) | Menos fricao que onboarding separado, workspace nasce com nome real |
| Permissoes de membro | OWNER > MANAGER > CREATIVE (tabela abaixo) | MANAGER opera dia a dia, OWNER controla acesso sensivel |
| Cargos | Lista fixa de 10 cargos (enum) | Cobre 90% das agencias, sem complexidade de campo livre |
| Convite | Link copiavel (sem SMTP no MVP) | Mais rapido de implementar, SMTP fica pra depois |
| Limite de membros | Sem limite | Produto para qualquer tamanho de agencia |
| Tela de aceitar convite | Tela unica inteligente (detecta se tem conta) | Menos cliques, mais direto |

---

## Modelo de Dados

### Workspace

| Campo | Tipo | Regras |
|-------|------|--------|
| id | BIGSERIAL PK | |
| name | VARCHAR(150) NOT NULL | Nome da agencia |
| slug | VARCHAR(150) UNIQUE | Gerado do nome (URL-friendly) |
| created_at | TIMESTAMP NOT NULL | |
| updated_at | TIMESTAMP NOT NULL | |

### Member

| Campo | Tipo | Regras |
|-------|------|--------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | NOT NULL |
| workspace_id | BIGINT FK → workspaces | NOT NULL |
| role | VARCHAR(20) NOT NULL | OWNER, MANAGER, CREATIVE |
| position | VARCHAR(30) NOT NULL | Enum: cargo funcional |
| created_at | TIMESTAMP NOT NULL | |

- Constraint UNIQUE(user_id, workspace_id) — um usuario so pode ser membro uma vez por workspace.

### InviteToken

| Campo | Tipo | Regras |
|-------|------|--------|
| id | BIGSERIAL PK | |
| workspace_id | BIGINT FK → workspaces | NOT NULL |
| email | VARCHAR(255) NOT NULL | Email do convidado |
| role | VARCHAR(20) NOT NULL | Papel atribuido |
| position | VARCHAR(30) NOT NULL | Cargo atribuido |
| token | VARCHAR(255) UNIQUE | UUID, single-use |
| invited_by | BIGINT FK → users | Quem convidou |
| expires_at | TIMESTAMP NOT NULL | 48h apos criacao |
| used | BOOLEAN DEFAULT FALSE | Invalidado apos uso |
| created_at | TIMESTAMP NOT NULL | |

### Alteracao em User (RF01)

Nenhuma alteracao na tabela `users`. O vinculo user ↔ workspace e feito via tabela `members`.

### Alteracao no Registro (RF01)

- Adicionar campo `workspaceName` ao `RegisterRequestDTO`
- No `AuthServiceImpl.register()`: criar Workspace + criar Member(role=OWNER, position=DIRETOR_DE_ARTE como default — pode ser alterado depois em configuracoes)

---

## Enums

### MemberRole (permissoes)

```
OWNER, MANAGER, CREATIVE
```

### MemberPosition (cargos)

```
DESIGNER_GRAFICO
EDITOR_DE_VIDEO
SOCIAL_MEDIA
COPYWRITER
GESTOR_DE_TRAFEGO
DIRETOR_DE_ARTE
ATENDIMENTO
FOTOGRAFO
ILUSTRADOR
MOTION_DESIGNER
```

---

## Permissoes por Papel

| Acao | OWNER | MANAGER | CREATIVE |
|------|-------|---------|----------|
| Convidar membro | sim | sim | nao |
| Remover membro | sim | sim (exceto OWNER) | nao |
| Alterar papel de membro | sim | nao | nao |
| Ver lista de membros | sim | sim | sim |
| Editar workspace (nome) | sim | sim | nao |
| Deletar workspace | sim | nao | nao |

---

## Fluxos

### Fluxo 1: Registro com Workspace

1. Usuario acessa `/auth/register`
2. Preenche: nome completo, email, senha, **nome da agencia**
3. Backend cria: User → Workspace → Member(role=OWNER)
4. Retorna tokens JWT (fluxo RF01 existente)
5. Redireciona ao dashboard

### Fluxo 2: Convidar Membro

1. Gestor (OWNER/MANAGER) acessa `/members`
2. Clica "Convidar membro" → abre dialog modal
3. Preenche: email, papel (dropdown: MANAGER/CREATIVE), cargo (dropdown: 10 opcoes)
4. Clica "Gerar link de convite"
5. Backend cria InviteToken (UUID, expira 48h), retorna URL
6. Dialog mostra link + botao "Copiar"
7. Gestor copia e envia por WhatsApp/email manualmente
8. InviteToken aparece como "Pendente" na lista de membros

### Fluxo 3: Aceitar Convite — Novo Usuario

1. Convidado clica no link `/auth/accept-invite?token=xxx`
2. Frontend chama GET `/api/v1/invite/{token}` → recebe dados do convite + flag `userExists: false`
3. Tela mostra: email (read-only), campo nome, campo senha
4. Painel esquerdo mostra: workspace, papel, cargo, quem convidou
5. Convidado preenche nome + senha, clica "Aceitar convite e criar conta"
6. Backend: cria User → cria Member → invalida token → retorna JWT
7. Redireciona ao dashboard

### Fluxo 4: Aceitar Convite — Usuario Existente

1. Convidado clica no link `/auth/accept-invite?token=xxx`
2. Frontend chama GET `/api/v1/invite/{token}` → recebe dados do convite + flag `userExists: true`
3. Tela mostra: email (read-only), campo senha
4. Painel esquerdo mostra: workspace, papel, cargo, quem convidou
5. Convidado preenche senha, clica "Aceitar convite e entrar"
6. Backend: valida credenciais → cria Member → invalida token → retorna JWT
7. Redireciona ao dashboard

### Fluxo 5: Remover Membro

1. Gestor clica "Remover" na linha do membro
2. Dialog de confirmacao: "Remover [nome] do workspace?"
3. Backend remove o Member (soft ou hard delete)
4. Lista atualiza

---

## API Endpoints

| Metodo | Endpoint | Descricao | Permissao |
|--------|----------|-----------|-----------|
| GET | /api/v1/workspace | Dados do workspace atual | Qualquer membro |
| PUT | /api/v1/workspace | Atualizar nome do workspace | OWNER, MANAGER |
| GET | /api/v1/members | Listar membros + convites pendentes | Qualquer membro |
| POST | /api/v1/members/invite | Criar convite (retorna link) | OWNER, MANAGER |
| DELETE | /api/v1/members/{id} | Remover membro | OWNER, MANAGER (nao OWNER) |
| PATCH | /api/v1/members/{id}/role | Alterar papel do membro | OWNER |
| GET | /api/v1/invite/{token} | Dados do convite (publico) | Publico (sem auth) |
| POST | /api/v1/invite/{token}/accept | Aceitar convite | Publico (sem auth) |

### Modificacao no registro existente

- `POST /api/v1/auth/register` — adicionar campo `workspaceName` ao body

---

## Multi-tenancy

Todas as queries de dominio (clientes, jobs, membros, etc.) filtram por `workspace_id`. O `workspace_id` do usuario logado e extraido do token JWT ou do Member associado ao User.

**Estrategia:** adicionar `workspace_id` como claim no JWT (adicionado no login apos lookup do Member). Endpoints protegidos extraem do token via `@RequestAttribute("workspaceId")` ou similar.

**Nota:** No MVP o usuario pertence a um unico workspace. Multi-workspace (usuario em varias agencias) fica para versao futura.

---

## Telas Frontend

### 1. Registro (modificacao)

- Arquivo existente: `features/auth/pages/register/`
- Adicionar campo "Nome da agencia" (pInputText, obrigatorio)
- Enviar `workspaceName` no POST de registro

### 2. Lista de Membros (nova)

- Path: `/members`
- Feature: `features/members/`
- Componentes: `member-list` (page), `invite-member-dialog` (component)
- Tabela com colunas: Membro (avatar+nome+email), Cargo, Papel (badge), Status (Ativo/Pendente), Acoes
- Convites pendentes aparecem com fundo sutil, avatar "?" e status amarelo
- Botao "Convidar membro" abre dialog modal
- Botao "Remover" com confirmacao (nao aparece para OWNER)

### 3. Dialog de Convite (novo)

- Modal sobre a lista de membros
- Campos: email, papel (dropdown: MANAGER/CREATIVE), cargo (dropdown: 10 cargos)
- Dois estados:
  - **Formulario** → "Gerar link de convite"
  - **Link gerado** → campo monospace com link + botao "Copiar" + mensagem de confirmacao verde

### 4. Aceitar Convite (nova)

- Path: `/auth/accept-invite?token=xxx`
- Rota publica (sem auth guard)
- Usa o mesmo PublicLayout (split-panel) do login/register
- Painel esquerdo: dados do convite (workspace, papel, cargo, convidado por)
- Painel direito: formulario inteligente
  - `userExists: false` → campos: email (read-only), nome, senha → "Aceitar convite e criar conta"
  - `userExists: true` → campos: email (read-only), senha → "Aceitar convite e entrar"

### 5. Configuracoes do Workspace (nova — simplificada)

- Path: `/settings`
- Feature: `features/settings/`
- MVP: apenas campo para editar nome do workspace + botao salvar
- Acessivel por OWNER e MANAGER

---

## Testes

### Backend

- **Unit:** WorkspaceServiceImplTest, MemberServiceImplTest, InviteServiceImplTest
- **Integration:** MemberControllerTest, InviteControllerTest (Testcontainers)
- Cenarios: criar workspace no registro, convidar membro, aceitar convite (novo/existente), remover membro, permissoes por papel, token expirado, token ja usado

### Frontend

- **Unit:** member-list.component.spec.ts, invite-member-dialog.component.spec.ts, accept-invite.component.spec.ts
- Cenarios: renderizar lista, abrir dialog, copiar link, formulario inteligente (novo vs existente), validacao de campos

---

## Fora de Escopo (MVP)

- SMTP/email real (link copiavel apenas)
- Multi-workspace (usuario em varias agencias)
- Limite de membros
- Upload de logo do workspace
- Auditoria de acoes de membros
- Recuperacao de senha
