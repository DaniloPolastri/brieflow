# Lessons Learned

> Arquivo de auto-melhoria contínua. Cada entrada é uma regra acionável que previne erros repetidos entre sessões.
> Consultar este arquivo no início de cada sessão de implementação.

## [Angular] — Sempre invocar frontend-design antes de implementar UI (ERRO REINCIDENTE)
- **Erro:** Implementei login, register e dashboard pages direto do plano sem invocar a skill `frontend-design`. REINCIDÊNCIA na RF02: implementei 5 telas (register mod, invite dialog, member list, accept invite, settings) via subagentes sem invocar frontend-design para nenhuma delas
- **Regra:** ANTES de escrever qualquer HTML/template/estilo de componente Angular, invocar `frontend-design`. Sem exceções — mesmo que o plano já tenha o template pronto, mesmo que a task seja delegada a subagente. O subagente NÃO substitui a skill de design. A skill deve ser invocada pelo orquestrador ANTES de despachar o subagente implementador
- **Contexto:** Toda task que cria ou modifica interface visual (pages, modais, formulários, cards, dashboards). Inclui tasks delegadas via subagent-driven-development

## [Geral] — PROMOVIDA para regra formal no CLAUDE.md (seção Fluxo de Implementação)
- **Status:** Lição promovida após 3ª violação. Regra agora está no CLAUDE.md como obrigatória e inegociável.
- **Regra original:** Marcar `[x]` no plano imediatamente ao completar cada task + executar parallel groups em paralelo.

## [Angular] — Usar templateUrl com arquivo HTML separado
- **Erro:** Coloquei templates grandes (50+ linhas) inline no `template:` do componente, deixando o .ts verboso e difícil de ler
- **Regra:** Sempre usar `templateUrl: './nome.component.html'` com arquivo HTML separado. Inline `template:` só para componentes com menos de ~10 linhas de HTML
- **Contexto:** Todos os componentes Angular do projeto

## [Spring] — Sempre usar MapStruct para mapeamento Entity ↔ DTO
- **Erro:** Fiz mapeamento manual inline nos services (new DTO(...) com campos extraídos na mão) em vez de usar MapStruct, que já está configurado no projeto e é convenção do CLAUDE.md
- **Regra:** Toda conversão Entity → DTO ou DTO → Entity DEVE usar um `@Mapper(componentModel = "spring")` MapStruct. Criar o mapper no pacote `com.briefflow.mapper`. Injetar via construtor no service. Mapeamento manual só é aceitável quando há lógica de negócio envolvida (ex: gerar invite link com frontendUrl)
- **Contexto:** Todos os services que retornam DTOs — verificar na criação de cada service se já existe mapper ou se precisa criar

## [Spring] — Toda operação de escrita DEVE verificar permissão do caller
- **Erro:** `WorkspaceServiceImpl.updateWorkspace` não verificava se o caller era OWNER/MANAGER — qualquer membro autenticado podia renomear o workspace
- **Regra:** Endpoints que modificam dados (PUT, PATCH, DELETE, POST) DEVEM verificar o papel do caller no service layer. Padrão: buscar Member do caller via `memberRepository.findByUserIdAndWorkspaceId()`, checar `role`, lançar `ForbiddenException` se não autorizado. Nunca confiar apenas no `authGuard` do frontend — o backend é a fonte de verdade de permissões
- **Contexto:** Todo controller/service que modifica dados e tem restrição por papel (ver tabela de permissões na design spec)

## [JPA] — DTOs de request com campos String que representam enums devem usar @NotBlank
- **Erro:** Usei `@NotNull` em campos `role` e `position` do `InviteMemberRequestDTO`. `@NotNull` aceita string vazia (""), que depois causa `IllegalArgumentException` no `valueOf()` do enum
- **Regra:** Campos String em DTOs de request que serão convertidos para enum DEVEM usar `@NotBlank` (não `@NotNull`). Isso rejeita null E string vazia na camada de validação, antes de chegar ao service
- **Contexto:** Todos os DTOs de request que recebem valores de enum como String

## [JPA] — @NotBlank em todo campo obrigatório de request DTO (especialmente senhas)
- **Erro:** `AcceptInviteRequestDTO.password` tinha `@Size(min=8)` mas sem `@NotBlank`. Bean Validation ignora `@Size` em valores null, permitindo password null que causava NPE no `passwordEncoder.matches(null, ...)`
- **Regra:** Todo campo obrigatório em DTOs de request DEVE ter `@NotBlank` (para Strings) ou `@NotNull` (para outros tipos). `@Size` sozinho NÃO valida null. Combinação correta: `@NotBlank @Size(min=X, max=Y)`
- **Contexto:** Todos os DTOs de request — revisar especialmente campos de senha, email, nome

## [JPA] — Slug deve usar Normalizer para transliterar acentos (app em português)
- **Erro:** `generateSlug()` usava regex `[^a-z0-9\\s-]` que simplesmente removía caracteres acentuados ("Café" → "caf" em vez de "cafe")
- **Regra:** Sempre usar `java.text.Normalizer.normalize(str, NFD)` + `replaceAll("\\p{InCombiningDiacriticalMarks}+", "")` ANTES de remover caracteres não-ASCII. Isso converte acentos para base ASCII (é→e, ã→a, ç→c)
- **Contexto:** Toda geração de slug, URL-friendly string, ou normalização de texto no projeto (app 100% em português)

## [JPA] — Slug deve ser regenerado no @PreUpdate quando nome muda
- **Erro:** `Workspace.generateSlug()` só era chamado no `@PrePersist`. Ao renomear o workspace, o slug ficava desatualizado
- **Regra:** Se uma entity tem campo derivado (como slug derivado de name), o `@PreUpdate` DEVE regenerar esse campo. Não confiar apenas no `@PrePersist`
- **Contexto:** Workspace entity e qualquer entity futura com campos derivados

## [JPA] — Queries que retornam "primeiro" resultado devem ter ORDER BY explícito
- **Erro:** `MemberRepository.findFirstByUserId` retornava resultado não-determinístico — sem `ORDER BY`, o DB escolhe qualquer linha
- **Regra:** Toda query JPQL que usa `findFirst*` ou retorna `Optional` de uma coleção DEVE ter `ORDER BY` explícito. Padrão: ordenar por `createdAt ASC` para pegar o mais antigo
- **Contexto:** Todos os repositories com métodos `findFirst*` ou queries que limitam resultados

## [JPA] — Usar JOIN FETCH para evitar N+1 em listagens
- **Erro:** `listMembers` usava `findByWorkspaceId` que retorna entities com LAZY relations. Acessar `member.getUser().getName()` no loop disparava 1 query por membro (N+1)
- **Regra:** Quando um service lista entities e acessa campos de relações LAZY no mapeamento, DEVE usar query com `JOIN FETCH` (ex: `SELECT m FROM Member m JOIN FETCH m.user WHERE ...`). Criar método separado no repository (ex: `findByWorkspaceIdWithUser`)
- **Contexto:** Todo repository method usado em listagens que acessam relações lazy no service/mapper

## [Spring] — Invalidar tokens/convites antigos ao criar novos para o mesmo recurso
- **Erro:** `inviteMember` criava novo InviteToken sem invalidar convites pendentes anteriores para o mesmo email+workspace, acumulando convites duplicados
- **Regra:** Ao criar token/convite novo, buscar e invalidar os anteriores do mesmo contexto (email+workspace). Padrão: `findByXAndUsedFalse()` → marcar como `used=true` → salvar → criar novo
- **Contexto:** InviteToken e qualquer entity futura de token single-use (reset password, approval, etc.)

## [Angular] — Aplicar roleGuard nas rotas que têm restrição de papel
- **Erro:** A rota `/settings` não tinha `roleGuard` — CREATIVE podia acessar a tela mesmo sem permissão (backend rejeitaria, mas UX confusa)
- **Regra:** Toda rota frontend que corresponde a funcionalidade restrita por papel DEVE ter `canActivate: [roleGuard('OWNER', 'MANAGER')]` (ou os papéis permitidos). Consultar tabela de permissões da spec ao criar rotas
- **Contexto:** app.routes.ts e todos os feature routes com restrição de acesso

## [Angular] — Não usar `as any` quando o tipo já tem o campo
- **Erro:** `role.guard.ts` usava `(user as any)?.role` quando a interface `User` já tinha o campo `role`
- **Regra:** Nunca usar `as any` para acessar campos que existem na interface. Se o campo não existe, adicionar à interface — não contornar o type system
- **Contexto:** Todo código TypeScript — `as any` é code smell que indica type mismatch a ser corrigido

## [Angular] — Sempre tratar erros em subscribe de operações destrutivas
- **Erro:** `removeMember` no frontend só tinha handler `next` sem handler `error`. Se o backend retornasse 403, o erro era silenciosamente engolido
- **Regra:** Todo `subscribe()` de operações que podem falhar (DELETE, POST, PUT, PATCH) DEVE ter handler `error` que mostra feedback ao usuário (toast, mensagem, etc). No mínimo `console.error` + sinalizar o erro na UI
- **Contexto:** Todos os componentes que fazem chamadas HTTP destrutivas ou de escrita

## [API] — Usar DateTimeFormatter em vez de LocalDateTime.toString()
- **Erro:** Populei campos String de DTOs com `localDateTime.toString()` que produz formato inconsistente dependendo dos milissegundos (às vezes inclui, às vezes não)
- **Regra:** Sempre usar `DateTimeFormatter.ISO_LOCAL_DATE_TIME` (ou formato específico) para converter datas em String. Definir constante no service: `private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME`
- **Contexto:** Todo service que converte LocalDateTime para String em DTOs

## [Git] — Sempre criar branch antes de qualquer alteração de código
- **Erro:** Fiz refatoração (MapStruct mappers) direto na `main` sem criar branch, violando a regra do CLAUDE.md
- **Regra:** Toda alteração de código — feature, fix OU refactor — DEVE começar criando branch: `feature/*`, `fix/*` ou `refactor/*`. Nunca codar direto na `main`. Criar a branch é o PRIMEIRO passo, antes de qualquer edição
- **Contexto:** Qualquer sessão de implementação, refatoração ou correção de bug
