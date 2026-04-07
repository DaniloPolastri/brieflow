# RF03 — Gestão de Clientes: Design Spec

**Data:** 2026-04-07
**Status:** Aprovado

---

## Contexto

CRUD de clientes da agência com busca, filtro por status e upload opcional de logo. Cliente é um cadastro interno do workspace — representa a empresa/pessoa atendida pela agência. O cliente real **não tem conta** no BriefFlow e **nunca é membro** do workspace. Sua participação no sistema acontece exclusivamente via Portal de Aprovação (RF07), acessando um link público sem login.

---

## Decisões de Design

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Logo do cliente | Upload de imagem (max 2MB, JPG/PNG) com fallback de iniciais | Prepara infraestrutura para RF06, sem ser obrigatório |
| Permissões | OWNER/MANAGER criam/editam/desativam. CREATIVE apenas visualiza | Mesmo padrão do RF02 |
| Listagem | Cards compactos em grid 3 colunas | Visual mais rico que tabela, boa densidade de informação |
| Formulário | Dialog/Modal (PrimeNG Dialog) | Poucos campos, consistente com invite dialog do RF02 |
| Desativar/Reativar | Menu de 3 pontos no card + confirmação. Filtro separa ativos/inativos | Lista limpa, inativos acessíveis via filtro |
| Campos obrigatórios | Apenas `name`. Empresa, email, telefone e logo são opcionais | Cadastro simples de controle interno |

---

## Backend

### Entity: Client

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
}
```

### Migration: V4__create_clients_table.sql

```sql
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    logo_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_workspace_id ON clients(workspace_id);
CREATE INDEX idx_clients_workspace_active ON clients(workspace_id, active);
```

### DTOs

```java
// Request
public record ClientRequestDTO(
    @NotBlank String name,
    String company,
    @Email String email,
    String phone
) {}

// Response
public record ClientResponseDTO(
    Long id,
    String name,
    String company,
    String email,
    String phone,
    String logoUrl,
    Boolean active,
    String createdAt
) {}
```

### Endpoints

| Metodo | Endpoint | Descricao | Permissao |
|--------|----------|-----------|-----------|
| POST | `/api/v1/clients` | Criar cliente | OWNER, MANAGER |
| GET | `/api/v1/clients` | Listar clientes (query: `search`, `active`) | Todos |
| GET | `/api/v1/clients/{id}` | Detalhe do cliente | Todos |
| PUT | `/api/v1/clients/{id}` | Atualizar cliente | OWNER, MANAGER |
| PATCH | `/api/v1/clients/{id}/toggle` | Ativar/desativar | OWNER, MANAGER |
| POST | `/api/v1/clients/{id}/logo` | Upload de logo (multipart, max 2MB, JPG/PNG) | OWNER, MANAGER |
| DELETE | `/api/v1/clients/{id}/logo` | Remover logo | OWNER, MANAGER |

### Repository: ClientRepository

```java
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByWorkspaceId(Long workspaceId);

    List<Client> findByWorkspaceIdAndActive(Long workspaceId, Boolean active);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Client> searchByNameOrCompany(@Param("workspaceId") Long workspaceId,
                                       @Param("search") String search);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND c.active = :active " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Client> searchByNameOrCompanyAndActive(@Param("workspaceId") Long workspaceId,
                                                @Param("search") String search,
                                                @Param("active") Boolean active);

    Optional<Client> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
```

### Service: ClientService

- **Listar:** Combina filtro `search` (nome/empresa, LIKE case-insensitive) + filtro `active` (null = todos, true = ativos, false = inativos). Retorna lista ordenada por nome.
- **Criar/Editar:** Valida permissão do caller (OWNER/MANAGER). Verifica que o client pertence ao workspace.
- **Toggle:** Marca `active = !active`. Não deleta jobs vinculados — apenas oculta o cliente da listagem ativa.
- **Upload logo:** Aceita multipart (max 2MB, JPG/PNG). Salva em `uploads/logos/{clientId}.{ext}`. Atualiza `logoUrl` na entity. Deleta arquivo anterior se existir.
- **Remover logo:** Deleta arquivo do filesystem e limpa `logoUrl`.

### Mapper: ClientMapper

```java
@Mapper(componentModel = "spring")
public interface ClientMapper {
    ClientResponseDTO toResponseDTO(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Client toEntity(ClientRequestDTO dto);

    default String mapDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
```

### Upload de Logo — Infraestrutura

- Diretório: `uploads/logos/` (relativo ao working directory do backend)
- Nomeação: `{clientId}.{ext}` (ex: `42.png`) — sobrescreve anterior
- Servido via `ResourceHandler` do Spring MVC: `GET /uploads/logos/{filename}`
- Validação: tipo MIME (image/jpeg, image/png), tamanho max 2MB
- Configuração: `app.upload-dir=uploads` no `application.yml`

---

## Frontend

### Estrutura de Arquivos

```
features/clients/
├── pages/
│   └── client-list/
│       ├── client-list.component.ts
│       └── client-list.component.html
├── components/
│   └── client-form-dialog/
│       ├── client-form-dialog.component.ts
│       └── client-form-dialog.component.html
├── services/
│   └── client-api.service.ts
├── models/
│   └── client.model.ts
└── clients.routes.ts
```

### Tela: Listagem de Clientes (`/clients`)

**Header:**
- Barra de busca: input text com placeholder "Buscar por nome ou empresa..."
- Filtros em toggle group: Todos | Ativos (default) | Inativos
- Botao "+ Novo Cliente" (visivel apenas para OWNER/MANAGER)

**Cards Grid (3 colunas):**
- Avatar: iniciais do nome com cor gerada por hash, ou imagem de logo se existir
- Linha principal: avatar + nome + empresa (truncado) + badge status (verde "Ativo" / vermelho "Inativo")
- Linha secundaria: email e telefone em texto menor
- Menu de 3 pontos (OWNER/MANAGER): Editar, Desativar/Reativar
- Desativar mostra dialog de confirmacao com PrimeNG ConfirmDialog
- CREATIVE ve cards sem menu de acoes e sem botao "Novo Cliente"

**Estado vazio:** Mensagem centralizada "Nenhum cliente cadastrado" com botao de criar (para OWNER/MANAGER)

**Busca:** Filtra em tempo real via query param ao backend (debounce 300ms)

### Modal: Criar/Editar Cliente

- PrimeNG Dialog, mesmo padrao do invite dialog do RF02
- Campos com Reactive Forms:
  - Nome (obrigatorio, @NotBlank)
  - Empresa (opcional)
  - Email (opcional, validacao de formato)
  - Telefone (opcional)
  - Logo: area de upload com preview. Botao "Enviar imagem" ou drag & drop. Preview circular. Botao de remover se ja tem logo.
- Botoes: Cancelar | Salvar
- No modo edicao, campos preenchidos com dados atuais
- Upload de logo feito em chamada separada apos salvar o cliente (POST `/clients/{id}/logo`)

### Rota

```typescript
export const CLIENTS_ROUTES: Routes = [
  {
    path: '',
    component: ClientListComponent
  }
];
```

Rota `/clients` protegida por `authGuard`. Todos os papeis acessam. Restricao de acoes (criar, editar, desativar) feita no componente via verificacao de role.

### Model

```typescript
export interface Client {
  id: number;
  name: string;
  company: string | null;
  email: string | null;
  phone: string | null;
  logoUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface ClientRequest {
  name: string;
  company?: string;
  email?: string;
  phone?: string;
}
```

### Service: ClientApiService

```typescript
@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/clients`;

  list(params?: { search?: string; active?: boolean }): Observable<Client[]>;
  getById(id: number): Observable<Client>;
  create(request: ClientRequest): Observable<Client>;
  update(id: number, request: ClientRequest): Observable<Client>;
  toggleActive(id: number): Observable<Client>;
  uploadLogo(id: number, file: File): Observable<Client>;
  removeLogo(id: number): Observable<void>;
}
```

---

## Permissoes

| Acao | OWNER | MANAGER | CREATIVE |
|------|-------|---------|----------|
| Listar clientes | Sim | Sim | Sim |
| Ver detalhe | Sim | Sim | Sim |
| Criar cliente | Sim | Sim | Nao |
| Editar cliente | Sim | Sim | Nao |
| Desativar/reativar | Sim | Sim | Nao |
| Upload/remover logo | Sim | Sim | Nao |

---

## Observacoes

- Desativar cliente NAO deleta jobs vinculados. Jobs do cliente inativo continuam no kanban normalmente.
- A listagem default mostra apenas clientes ativos. Inativos acessiveis via filtro.
- Upload de logo é independente do CRUD — endpoint separado, chamado apos criar/editar.
- Esta infraestrutura de upload (filesystem local, validacao de tipo/tamanho) sera reutilizada e expandida no RF06 (Upload de Pecas).
