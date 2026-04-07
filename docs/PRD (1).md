# BriefFlow — PRD

**Autor:** Danilo  
**Data:** Abril 2026  
**Status:** Draft

---

## Overview

BriefFlow é uma plataforma de gestão de produção criativa para agências de marketing digital. Centraliza briefings, kanban de produção e aprovações de clientes em um único lugar, substituindo o fluxo fragmentado de WhatsApp + Trello + Excel.

Este projeto será desenvolvido utilizando **Angular no frontend** e **Java Spring Boot no backend**, com arquitetura REST stateless e autenticação baseada em JWT.

---

## Problem

### O que está acontecendo?

Agências de marketing pequenas e médias operam com um fluxo de produção criativa totalmente fragmentado. O ciclo típico funciona assim:

1. O cliente manda o pedido pelo WhatsApp — geralmente incompleto (sem paleta de cores, sem texto final, sem referências visuais, sem formato especificado)
2. O designer recebe a demanda no grupo do WhatsApp e precisa caçar informações: perguntar ao gestor, buscar em conversas antigas, pedir referências ao cliente
3. O designer executa no Canva e atualiza o status manualmente no Trello
4. A peça finalizada é colocada em um Excel compartilhado onde o cliente acessa para aprovar ou fazer observações
5. Se há revisão, o ciclo recomeça — frequentemente com informações perdidas

Cada etapa usa uma ferramenta diferente. Nenhuma conversa com a outra.

### Quem é afetado?

**Gestor/Dono da agência:** Não tem visibilidade real do que está em produção, quem está sobrecarregado, o que está atrasado. Gasta tempo cobrando status por WhatsApp.

**Designer/Editor (criativo):** Perde tempo caçando informações de briefing em vez de criar. Recebe demandas incompletas e precisa ir atrás do que falta. Gerencia seus próprios clientes isoladamente.

**Cliente da agência:** Precisa acessar um Excel para ver peças e aprovar. Não tem visão clara do que está em andamento. A experiência é amadora.

### Qual o custo de não resolver?

- Retrabalho por briefings incompletos (estima-se 20-30% da capacidade produtiva perdida)
- Tempo do gestor gasto cobrando status em vez de gerenciando
- Experiência amadora para o cliente (Excel de aprovação)
- Impossibilidade de escalar — quanto mais clientes, mais caos
- Informação perdida em conversas do WhatsApp

### Como resolvem hoje?

- **WhatsApp:** Recebimento de briefings e comunicação com cliente (grupos por cliente)
- **Trello:** Controle de status das demandas (cada criativo gerencia os seus)
- **Excel/Google Sheets:** Planilha compartilhada com o cliente para upload de peças e aprovação
- **Canva:** Execução das peças criativas
- Algumas agências maiores usam Operand, iClips ou Studio — mas são caros e complexos para equipes pequenas

---

## Goals

- [ ] **Goal 1:** Criativo receber briefing completo e estruturado sem precisar caçar informação
  → Métrica: < 2 minutos entre receber o job e começar a criar

- [ ] **Goal 2:** Cliente aprovar ou solicitar revisão de peça em < 30 segundos
  → Métrica: Tempo médio de interação no portal de aprovação

- [ ] **Goal 3:** Gestor visualizar status de toda produção em tempo real
  → Métrica: Dashboard carrega em < 2 segundos com visão completa

- [ ] **Goal 4:** Reduzir rodadas de revisão por briefing incompleto
  → Métrica: Média de revisões por job < 2

---

## Non-Goals

Nesta versão NÃO será incluído:

- ❌ Aplicativo mobile nativo
- ❌ Integração com WhatsApp (API Business)
- ❌ Integração com Canva
- ❌ Markup visual em imagens (comentários posicionados)
- ❌ Timesheet / controle de horas
- ❌ Relatório mensal automático para cliente
- ❌ White-label
- ❌ API pública
- ❌ Billing / cobrança automatizada
- ❌ Multi-idioma (i18n)

---

## Personas

### Persona 1: Gestor da Agência — "Rafael"

**Quem é:** Dono ou gerente de uma agência de marketing digital com 2-15 pessoas. Gerencia clientes, distribui demandas para o time criativo e é responsável por entregas no prazo.

**Dia a dia:** Recebe pedidos de clientes pelo WhatsApp, repassa para designers/editores, cobra status, faz controle de qualidade e envia peças para aprovação. Usa WhatsApp o dia todo.

**Dores:**
- Não sabe o status real da produção sem perguntar para cada pessoa
- Perde tempo repassando informações que o cliente mandou pelo WhatsApp
- Não consegue identificar gargalos ou sobrecarga no time
- A planilha de aprovação com o cliente é amadora

**Job to be Done:** Quero ter visão total da produção da minha agência e oferecer uma experiência profissional de aprovação para meus clientes.

**É quem decide a compra da ferramenta.**

---

### Persona 2: Designer/Editor — "Camila"

**Quem é:** Designer gráfico ou editor de vídeo que trabalha na agência. Recebe demandas, executa peças criativas e entrega para revisão/aprovação.

**Dia a dia:** Recebe jobs pelo WhatsApp (grupo do cliente), busca informações necessárias (textos, paletas, referências), executa no Canva/Premiere, atualiza status no Trello, sobe a peça no Excel compartilhado.

**Dores:**
- Recebe briefings incompletos e precisa ficar perguntando
- Informações espalhadas em múltiplas conversas do WhatsApp
- Gerencia status manualmente em Trello — é overhead, não ajuda a criar
- Não tem histórico organizado dos jobs por cliente

**Job to be Done:** Quero receber tudo que preciso para criar em um lugar só, executar, entregar e seguir para o próximo job sem perder tempo com burocracia.

**É a usuária diária mais frequente.**

---

### Persona 3: Cliente da Agência — "Fernando"

**Quem é:** Dono de negócio ou gerente de marketing que contratou a agência. Precisa aprovar peças antes da publicação.

**Dia a dia:** Recebe peças da agência para aprovar (via Excel ou WhatsApp), dá feedback, aprova ou pede ajustes. Tem pouco tempo e quer resolver rápido.

**Dores:**
- Precisa abrir Excel para ver peças (experiência ruim)
- Não sabe o que está em produção — só vê quando está pronto
- Feedback fica perdido entre WhatsApp e Excel
- Não tem histórico fácil do que já foi aprovado

**Job to be Done:** Quero ver as peças da minha agência e aprovar ou pedir ajustes de forma rápida e sem complicação.

**Não faz login no sistema — acessa via link público.**

---

## User Stories

### Persona: Gestor (Rafael)

**Autenticação e Workspace:**
- Como gestor, quero criar minha conta para acessar o BriefFlow
- Como gestor, quero fazer login com email e senha para acessar meu workspace
- Como gestor, quero convidar membros do meu time para que eles acessem a plataforma
- Como gestor, quero definir o papel de cada membro (gestor ou criativo) para controlar permissões

**Gestão de Clientes:**
- Como gestor, quero cadastrar os clientes da minha agência para organizar jobs por cliente
- Como gestor, quero editar informações de um cliente para manter dados atualizados
- Como gestor, quero ver a lista de todos os meus clientes para ter visão geral
- Como gestor, quero desativar um cliente sem perder o histórico de jobs

**Criação de Jobs:**
- Como gestor, quero criar um novo job selecionando o cliente e tipo (post, stories, vídeo, etc.) para iniciar uma demanda
- Como gestor, quero preencher um briefing estruturado com campos específicos por tipo de job para que o criativo tenha todas as informações
- Como gestor, quero anexar arquivos ao briefing (referências, logos, textos) para que nada se perca
- Como gestor, quero definir um prazo para o job para controlar entregas
- Como gestor, quero atribuir o job a um criativo específico para distribuir trabalho

**Acompanhamento:**
- Como gestor, quero ver todos os jobs em um kanban para entender o status da produção
- Como gestor, quero filtrar jobs por cliente, criativo ou status para encontrar rapidamente o que preciso
- Como gestor, quero ver um dashboard com jobs atrasados, em andamento e concluídos para ter visão gerencial
- Como gestor, quero receber notificação por email quando um cliente aprova ou pede revisão

**Aprovação:**
- Como gestor, quero gerar um link de aprovação para o cliente para que ele veja e aprove a peça sem precisar de login
- Como gestor, quero ver o histórico de aprovações e revisões de cada job para rastrear decisões

---

### Persona: Criativo (Camila)

**Acesso:**
- Como criativa, quero aceitar o convite do gestor e criar minha conta para acessar a plataforma
- Como criativa, quero fazer login para ver meus jobs

**Execução:**
- Como criativa, quero ver apenas os jobs atribuídos a mim para focar no meu trabalho
- Como criativa, quero abrir um job e ver o briefing completo com todas as informações e arquivos em um lugar só
- Como criativa, quero mover o job entre status do kanban (ex: em criação, revisão interna, enviado para cliente) para manter o gestor atualizado
- Como criativa, quero fazer upload da peça finalizada no job para que seja enviada para aprovação
- Como criativa, quero fazer upload de uma nova versão quando o cliente pedir revisão para manter o histórico

**Visão:**
- Como criativa, quero ver o histórico de comentários e revisões de um job para entender o que foi solicitado
- Como criativa, quero receber notificação quando um novo job for atribuído a mim

---

### Persona: Cliente (Fernando)

**Aprovação:**
- Como cliente, quero acessar um link sem precisar de login para ver a peça que preciso aprovar
- Como cliente, quero ver a peça em tamanho adequado para avaliar a qualidade
- Como cliente, quero aprovar a peça com um clique para que a agência saiba que está OK
- Como cliente, quero solicitar revisão e escrever um comentário explicando o que precisa mudar
- Como cliente, quero ver o histórico de versões da peça para comparar com versões anteriores
- Como cliente, quero receber um email quando uma nova versão estiver pronta para aprovação

---

## Solution

### Visão Geral

A solução é composta por:

- **Frontend SPA em Angular** — painel da agência (gestor + criativos) e portal de aprovação (cliente)
- **Backend REST API em Spring Boot** — lógica de negócio, autenticação JWT, gestão de arquivos
- **Banco de dados PostgreSQL** — persistência de dados
- **Serviço de email** — notificações de status e aprovação

O frontend consumirá a API backend via HTTP. O portal de aprovação do cliente é uma rota pública do mesmo frontend Angular, autenticada por token único no link.

### Modelo de Acesso — Quem está dentro e quem está fora

O BriefFlow tem dois contextos de acesso completamente separados:

**1. Painel da Agência (autenticado via JWT)**
- Acessado por membros do workspace: OWNER, MANAGER e CREATIVE
- Esses são os **funcionários da agência** — designers, editores, gestores
- Requer cadastro (email/senha) e vínculo a um workspace
- Toda a gestão acontece aqui: clientes, jobs, kanban, dashboard

**2. Portal de Aprovação (acesso público via token UUID)**
- Acessado pelo **cliente da agência** — a empresa/pessoa atendida
- O cliente **não tem conta** no BriefFlow e **nunca é membro** do workspace
- Acessa via link único enviado por email (ex: `/approval/{token}`)
- Escopo limitado: visualizar a peça e aprovar ou solicitar revisão
- Sem login, sem senha, sem cadastro

**A entidade "Client" (RF03) é um cadastro interno da agência.** Representa o cliente atendido (nome, empresa, contato) para organizar jobs. O cliente real só interage com o sistema pelo Portal de Aprovação (RF07), sem conhecer o painel interno.

---

## Features Principais

| Feature | Descrição | Prioridade |
|---------|-----------|------------|
| Autenticação | Registro, login, convite de membros via JWT | Must Have (P0) |
| Gestão de Membros | Convite, papéis (gestor/criativo), listagem | Must Have (P0) |
| Gestão de Clientes | CRUD de clientes da agência | Must Have (P0) |
| Jobs com Briefing | Criação de jobs com formulário estruturado por tipo | Must Have (P0) |
| Kanban de Produção | Board com status customizáveis, drag & drop | Must Have (P0) |
| Upload de Peças | Upload de arquivos (imagens, vídeos, PDFs) no job | Must Have (P0) |
| Portal de Aprovação | Link público para cliente ver peça e aprovar/revisar | Must Have (P0) |
| Dashboard do Gestor | Visão geral: jobs por status, atrasados, por cliente | Must Have (P0) |
| Notificações Email | Alertas de novo job, aprovação, revisão | Must Have (P0) |
| Filtros e Busca | Filtrar kanban por cliente, criativo, prazo | Should Have (P1) |
| Configuração de Status | Gestor customiza os status do kanban | Should Have (P1) |
| Histórico de Versões | Manter versões anteriores das peças | Should Have (P1) |
| Markup Visual | Comentários posicionados na imagem | Could Have (P2) |
| Timesheet | Controle de horas por job | Could Have (P2) |

---

## User Flows

### Flow 1: Criação de Job (Gestor)

```
1. Gestor acessa o dashboard
2. Clica em "Novo Job"
3. Seleciona o cliente
4. Seleciona o tipo de job (post, stories, carrossel, vídeo, etc.)
5. Sistema exibe formulário de briefing específico para o tipo
6. Gestor preenche: descrição, texto da peça, referências visuais, paleta, prazo
7. Gestor anexa arquivos (logos, imagens de referência)
8. Gestor atribui a um criativo
9. Sistema cria o job no status "Novo"
10. Sistema envia email para o criativo atribuído
11. Job aparece no kanban
```

### Flow 2: Execução e Entrega (Criativo)

```
1. Criativo acessa seu kanban filtrado
2. Vê o novo job no status "Novo"
3. Abre o job e lê o briefing completo
4. Move para "Em Criação"
5. Executa a peça (fora do BriefFlow — Canva, Photoshop, etc.)
6. Volta ao BriefFlow e faz upload da peça finalizada
7. Move para "Revisão Interna" (gestor confere)
8. Gestor aprova internamente e move para "Aguardando Aprovação"
9. Sistema gera link de aprovação do cliente
```

### Flow 3: Aprovação (Cliente)

```
1. Cliente recebe email com link de aprovação
2. Clica no link — abre o portal sem necessidade de login
3. Vê a peça em destaque com informações do job
4. Opção A: Clica "Aprovar" → Job move para "Aprovado", email enviado ao gestor
5. Opção B: Clica "Solicitar Revisão" → Escreve comentário → Job volta para "Revisão", email enviado ao criativo
6. Se revisão: criativo faz nova versão, faz upload, ciclo repete
```

### Flow 4: Login

```
1. Usuário acessa BriefFlow
2. Insere email e senha
3. Angular envia POST /api/auth/login
4. Backend valida credenciais e retorna JWT (access + refresh token)
5. Angular armazena tokens
6. Usuário é redirecionado ao dashboard
```

---

## Requisitos Funcionais Detalhados

### RF01 — Autenticação

- Registro com email + senha (com confirmação de email)
- Login com JWT (access token + refresh token)
- Access token expira em 15 minutos, refresh token em 7 dias
- Logout invalida refresh token
- Senha com mínimo 8 caracteres
- Rate limiting: máximo 5 tentativas de login por minuto

### RF02 — Workspace e Membros

- Ao registrar, cria-se automaticamente um workspace (agência)
- Gestor pode convidar membros por email
- Convite gera um link com token único (expira em 48h)
- Papéis: OWNER (dono), MANAGER (gestor), CREATIVE (criativo)
- OWNER: tudo. MANAGER: tudo exceto deletar workspace. CREATIVE: ver/executar jobs atribuídos
- Máximo de membros conforme plano

### RF03 — Gestão de Clientes

- CRUD completo de clientes (nome, empresa, email, telefone, logo)
- Cliente pode ser ativo ou inativo
- Desativar cliente não deleta jobs — apenas oculta da lista ativa
- Listar clientes com busca por nome/empresa
- Um cliente pertence a um único workspace

### RF04 — Jobs e Briefing

- Criar job vinculado a um cliente
- Tipos de job: Post Feed, Stories, Carrossel, Reels/Vídeo, Banner, Logo, Outros
- Cada tipo tem campos específicos no formulário de briefing:
  - **Post Feed:** descrição, texto da legenda, formato (1:1, 4:5), paleta de cores, referências visuais
  - **Stories:** descrição, texto, formato (9:16), CTA, referências
  - **Carrossel:** descrição, número de slides, texto por slide, formato
  - **Reels/Vídeo:** descrição, duração, roteiro/storyboard, áudio, referências
  - **Banner:** descrição, dimensões, texto, CTA
  - **Logo:** descrição, estilo desejado, referências, cores
  - **Outros:** descrição livre
- Campos comuns: título, cliente, tipo, prazo, prioridade (baixa/normal/alta/urgente), criativo atribuído, descrição geral, arquivos anexos
- Upload de múltiplos arquivos no briefing (imagens, PDFs, vídeos até 50MB)
- Cada job tem um identificador único legível (ex: JOB-001)

### RF05 — Kanban de Produção

- Status padrão: Novo → Em Criação → Revisão Interna → Aguardando Aprovação → Aprovado → Publicado
- Gestor pode customizar nomes e ordem dos status (exceto "Novo" e "Aprovado" que são fixos)
- Drag & drop para mover jobs entre status
- Mover para "Aguardando Aprovação" gera automaticamente o link de aprovação
- Filtros: por cliente, por criativo, por prioridade, por prazo
- Indicador visual de jobs atrasados (prazo vencido)
- Criativo vê apenas seus jobs por padrão (pode ver todos se permitido)

### RF06 — Upload de Peças

- Upload de arquivos: imagens (PNG, JPG, WEBP), vídeos (MP4, MOV), PDFs
- Limite: 50MB por arquivo
- Múltiplos arquivos por job (ex: post + stories do mesmo job)
- Thumbnail gerado automaticamente para preview
- Ao fazer upload de nova versão, versão anterior é mantida no histórico
- Armazenamento em disco local (MVP) — migrar para S3 posteriormente

### RF07 — Portal de Aprovação do Cliente

- Link público com token UUID único (ex: /approve/abc123-def456)
- Token expira em 30 dias (configurável)
- Sem necessidade de login do cliente
- Exibe: nome do job, tipo, peça(s) em destaque, versão atual
- Ações: "Aprovar" (um clique) ou "Solicitar Revisão" (com campo de comentário obrigatório)
- Ao aprovar: job move para status "Aprovado", email ao gestor e criativo
- Ao pedir revisão: job volta para status de revisão, email ao criativo com o comentário
- Histórico de aprovações/revisões visível no link
- Design limpo e profissional — é a cara da agência para o cliente

### RF08 — Dashboard do Gestor

- Visão geral: total de jobs por status (cards ou gráfico de barras)
- Jobs atrasados (prazo vencido) em destaque
- Jobs por cliente (quantidade e status)
- Jobs por criativo (carga de trabalho)
- Filtro por período (última semana, último mês, customizado)
- Acesso rápido ao kanban a partir do dashboard

### RF09 — Notificações por Email

- Novo job atribuído → email para o criativo
- Job movido para "Aguardando Aprovação" → email para o cliente (com link de aprovação)
- Cliente aprovou → email para gestor e criativo
- Cliente solicitou revisão → email para gestor e criativo (com comentário)
- Prazo do job vencendo (24h antes) → email para criativo e gestor

---

## Requisitos Não-Funcionais

### Performance
- Tempo de resposta da API: < 300ms (P95)
- Tempo de carregamento do frontend: < 2s (first meaningful paint)
- Upload de arquivos: suporte a até 50MB sem timeout
- Dashboard com até 1000 jobs sem degradação perceptível

### Segurança
- Autenticação JWT com refresh token rotation
- Senhas hasheadas com BCrypt
- Rate limiting em endpoints sensíveis (login, registro)
- CORS configurado para domínios permitidos
- Validação de input em todos os endpoints (Bean Validation)
- Sanitização de comentários do cliente (XSS prevention)
- Token de aprovação com escopo limitado (apenas leitura + aprovação do job específico)

### Escalabilidade
- Multi-tenant simples: cada workspace (agência) é isolado por tenant_id em todas as queries
- Banco de dados: indexes em colunas frequentemente filtradas (workspace_id, client_id, status, assignee_id)
- File storage abstraído via interface (disco local no MVP, S3 na evolução)

### Disponibilidade
- Uptime target: 99.5%
- Logging estruturado com SLF4J
- Health check endpoint (/actuator/health)
- Tratamento global de exceções com respostas padronizadas

### Usabilidade
- Light mode only (MVP)
- Desktop-first, responsivo para tablet
- Acessibilidade WCAG AA (contraste, foco, ARIA)
- Onboarding simples: ao criar conta, sugerir criar primeiro cliente e primeiro job

---

## Casos de Borda e Edge Cases

### Jobs
- Job criado sem criativo atribuído → fica no kanban como "Não atribuído" (visível para todos)
- Job com prazo no passado ao criar → sistema alerta mas permite (pode ser urgência real)
- Criativo removido do workspace com jobs ativos → jobs ficam "Não atribuído", gestor notificado
- Upload falha no meio → arquivo descartado, usuário notificado para tentar novamente
- Job sem peça movido para "Aguardando Aprovação" → sistema bloqueia e pede upload

### Aprovação
- Link de aprovação acessado após expiração → mensagem amigável pedindo para contatar a agência
- Cliente tenta aprovar um job já aprovado → mensagem informando que já foi aprovado
- Cliente tenta revisar um job já em revisão → permite (pode ter nova observação)
- Múltiplas pessoas com o link tentam aprovar simultaneamente → primeiro a clicar prevalece, segundo vê status atualizado

### Workspace
- Membro tenta acessar workspace de outra agência → 403 Forbidden
- OWNER tenta deletar workspace com jobs ativos → precisa confirmar (soft delete)
- Convite enviado para email já cadastrado em outro workspace → permite (uma pessoa pode ser membro de múltiplas agências)

---

## Technical Approach

### Stack

**Frontend:**
- Angular (Latest) com Standalone Components
- TypeScript strict mode
- Signals para state management
- PrimeNG para componentes UI
- Tailwind CSS para estilização
- RxJS para operações assíncronas
- Angular Router com lazy loading

**Backend:**
- Java 21
- Spring Boot 3+
- Spring Security (JWT + stateless)
- Spring Data JPA + Hibernate
- PostgreSQL
- MapStruct para mapeamento DTO ↔ Entity
- Bean Validation
- Flyway para migrations

**Infraestrutura:**
- Docker + Docker Compose
- Nginx (reverse proxy)

### Arquitetura

```
[Browser]
    ↓
[Angular SPA]
    ↓ HTTP/REST
[Spring Boot API]
    ↓
[PostgreSQL]
```

### ⚠️ Documentação obrigatória de estrutura

Antes de iniciar o desenvolvimento, consulte obrigatoriamente:

- 📄 ESTRUTURA-FRONTEND.md
- 📄 ESTRUTURA-BACKEND.md
- 📄 angular-best-practices.md
- 📄 angular-style-guide.md
- 📄 folder-rules.md
- 📄 tailwind.md
- 📄 unit-tests.md
- 📄 java-spring-best-practices.md

---

## Segurança

**Autenticação:** JWT Token (access + refresh)
**Autorização:** Role-based (OWNER, MANAGER, CREATIVE)
**Spring Security:** Stateless, BCrypt, CORS explícito
**Angular:** HTTP Interceptor para JWT, Route Guards por role
**Portal do Cliente:** Token UUID no link, sem autenticação de usuário

---

## Integrações

- [ ] API REST Spring Boot ↔ Angular
- [ ] PostgreSQL via Spring Data JPA
- [ ] SMTP para envio de emails (Mailgun, SendGrid ou Amazon SES)
- [ ] File storage local (disco) com abstração para futura migração S3

---

## Constraints

- SPA frontend (Angular)
- Backend stateless (JWT, sem sessão)
- REST only (sem WebSocket no MVP — polling para atualização do kanban)
- File storage local no MVP (sem CDN)
- Sem mobile nativo
- Sem real-time collaboration

---

## Design Guidelines

Frontend Angular deverá seguir:

- Estilo visual Operand/Linear — clean, moderno, light mode
- PrimeNG customizado com Tailwind
- Inter (body) + JetBrains Mono (código/IDs)
- Ver documento DESIGN-GUIDELINES.md para especificações completas

---

## Success Metrics

| Métrica | Target |
|---------|--------|
| Tempo de resposta API (P95) | < 300ms |
| Tempo de carregamento frontend | < 2s |
| Erros backend (5xx) | < 1% |
| Jobs completados/semana por agência | > 5 |
| Revisões por job (média) | < 2 |
| Tempo de aprovação do cliente | < 24h |
| Retenção 7 dias | > 60% |

---

## Timeline

| Fase | Entrega |
|------|---------|
| Setup projeto | Angular + Spring Boot base, auth, deploy local |
| Core Features | Clientes, Jobs, Briefing, Kanban |
| Aprovação | Portal público, notificações email |
| Dashboard + Polish | Dashboard gestor, ajustes UX, bugs |
| Beta | Teste com agência da esposa |

---

## Risks & Assumptions

### Assumptions

- Usuários acessam via browser moderno (Chrome, Safari, Firefox atualizados)
- Agências pequenas aceitam adotar uma nova ferramenta se resolver a dor
- O gestor é quem decide a compra; o criativo é quem usa diariamente
- WhatsApp continuará sendo usado para comunicação rápida — BriefFlow não substitui isso
- Upload de arquivos até 50MB cobre 95%+ dos casos de uso

### Risks

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Scope creep (querer adicionar features) | Alta | Alto | Revisar MVP-SCOPE.md semanalmente |
| Agências não adotarem por já terem "processo" | Média | Alto | Beta gratuito + onboarding personalizado |
| Performance de upload de vídeos grandes | Média | Médio | Limite de 50MB, chunked upload futuro |
| Concorrência com Operand em evolução | Baixa | Médio | Focar no nicho de agência pequena e preço |
| Esposa como única testadora (viés) | Alta | Médio | Buscar 3-5 agências adicionais para beta |

---

## Open Questions

- Qual serviço de email usar? (Mailgun, SendGrid, SES)
- Qual provedor de hosting para deploy? (Railway, Render, AWS, DigitalOcean)
- Precisa de CDN para servir arquivos no portal de aprovação?
- Implementar notificação in-app além de email?
- Permitir que o cliente faça download das peças aprovadas direto do portal?

---

## Appendix

### Padrões Utilizados

**Backend:**
- REST API
- DTO Pattern (Request/Response)
- Repository Pattern
- Service Layer Pattern
- Mapper Pattern (MapStruct)
- Global Exception Handler

**Frontend:**
- Service Pattern
- Lazy Loading por feature
- Route Guards
- HTTP Interceptor (JWT)
- Signals para state management
- Standalone Components
