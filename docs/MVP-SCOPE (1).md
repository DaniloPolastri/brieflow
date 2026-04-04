# BriefFlow - MVP Scope

**Data:** Abril 2026  
**Versão:** 1.0

---

## Visão do MVP

**Em uma frase, o que o MVP faz?**

> Centraliza briefings, kanban de produção e aprovação do cliente em uma plataforma para agências de marketing pequenas.

**Qual hipótese estamos testando?**

> Agências de marketing pequenas (2-15 pessoas) adotariam uma ferramenta dedicada para substituir o fluxo fragmentado de WhatsApp + Trello + Excel, desde que seja simples e acessível.

**Como saberemos que funcionou?**

> Pelo menos 5 agências usando ativamente (5+ jobs/semana) após 30 dias de beta, com taxa de retenção 7 dias > 60%.

---

## Escopo: O que ENTRA

### Must Have (P0) - Sem isso não lança

| Feature | Descrição | Critério de Done |
|---------|-----------|-----------------|
| Autenticação | Registro + login com email/senha via JWT | Usuário registra, loga, recebe token, acessa dashboard |
| Workspace e Membros | Criar workspace ao registrar, convidar membros por email, papéis (owner/manager/creative) | Gestor convida criativo por email, criativo aceita e acessa |
| Gestão de Clientes | CRUD de clientes da agência (nome, empresa, email, logo) | Gestor cria, edita, lista e desativa clientes |
| Jobs com Briefing | Criar job com formulário estruturado por tipo (post, stories, vídeo, etc.), anexar arquivos, definir prazo e atribuir criativo | Job criado com briefing completo acessível pelo criativo |
| Kanban de Produção | Board com status padrão (Novo → Em Criação → Revisão → Aguardando Aprovação → Aprovado), drag & drop | Jobs movem entre status, filtro por cliente/criativo |
| Upload de Peças | Upload de imagens/vídeos/PDFs no job (até 50MB) | Criativo faz upload, peça aparece no job e no portal de aprovação |
| Portal de Aprovação | Link público com token UUID, cliente vê peça e aprova ou pede revisão com comentário | Cliente acessa link sem login, aprova ou comenta, status atualiza |
| Dashboard Gestor | Visão: jobs por status, atrasados, por cliente, por criativo | Dashboard carrega com dados reais, filtro por período |
| Notificações Email | Emails para: novo job atribuído, peça para aprovação, aprovado, revisão solicitada | Emails chegam nos eventos corretos com informações do job |

### Should Have (P1) - Importante, mas pode esperar v1.1

| Feature | Descrição | Por que não é P0 |
|---------|-----------|-----------------|
| Status Customizáveis | Gestor renomeia e reordena colunas do kanban | Status padrão cobre 90% dos casos no MVP |
| Filtros Avançados | Filtrar por prioridade, prazo, busca por texto | Filtro básico (cliente/criativo) já resolve no MVP |
| Histórico de Versões | Manter versões anteriores da peça com comparação | No MVP, nova versão substitui — histórico é nice-to-have |
| Prazo com Alerta | Email automático 24h antes do prazo vencer | Indicador visual de atraso no kanban já comunica no MVP |
| Perfil e Configurações | Foto, nome, configurações de notificação | Funcional sem isso |

### Could Have (P2) - Nice to have

| Feature | Descrição | Quando considerar |
|---------|-----------|------------------|
| Markup Visual | Comentários posicionados na imagem (tipo Figma) | v1.2 — quando validar que aprovação por texto não basta |
| Timesheet | Tempo gasto por job automaticamente | v1.2 — quando agências pedirem controle de horas |
| Relatório Mensal | PDF automático com entregas do mês por cliente | v1.2 — quando agências pedirem justificativa de fee |
| Templates de Briefing | Salvar briefings customizados como template | v1.1 — quando agências criarem jobs repetitivos |
| Duplicar Job | Clonar job existente para criar novo similar | v1.1 — conveniência |

---

## Escopo: O que NÃO ENTRA

### Explicitamente Fora do MVP

| Feature | Por que não entra | Quando reconsiderar |
|---------|------------------|---------------------|
| Integração WhatsApp | Complexidade alta (WhatsApp Business API), custo mensal, MVP valida sem isso | v2 — após validar que agências querem receber briefing pelo BriefFlow |
| App Mobile Nativo | Esforço grande, web responsivo resolve no MVP | v2 — se métricas mostrarem acesso mobile > 40% |
| White-label | Customização de marca para agência — esforço alto, pouco valor no MVP | v2 — como feature premium do plano Agency |
| API Pública | Nenhuma agência pequena precisa de API no início | v3 — quando houver demanda de integração |
| Billing Automático | Stripe/pagamento — no MVP, controle manual ou free tier | v1.2 — quando tiver 10+ agências prontas para pagar |
| Integração Canva | API do Canva para importar peças — complexo e desnecessário | v2 — explorar quando tiver tração |
| Multi-idioma (i18n) | Foco no mercado brasileiro primeiro | v2 — se expandir para mercado internacional |
| Real-time (WebSocket) | Polling resolve para o MVP, WebSocket é over-engineering | v1.2 — se UX de kanban exigir |

### Tentações Comuns a Evitar

- [x] Dashboard de admin elaborado → dashboard simples com cards de contagem
- [x] Analytics avançados → apenas contagens por status/cliente/criativo
- [x] Múltiplas integrações → zero integrações externas no MVP
- [x] Multi-tenancy complexo → isolamento por workspace_id em queries
- [x] Internacionalização → português apenas
- [x] Mobile app nativo → web responsivo
- [x] Marketplace/plugins → zero extensibilidade
- [x] Billing complexo → free tier ou controle manual

---

## Decisões de Simplificação

### Autenticação
- [x] Email + senha simples
- [x] Spring Boot Security + JWT

### Billing
- [x] Free only no MVP (sem cobrança)

### UI/UX
- [x] Light mode only
- [x] Desktop-first (responsivo para tablet)
- [x] PrimeNG customizado com Tailwind
- [x] Onboarding mínimo (tooltip na primeira vez)

### Features
- [x] CRUD básico primeiro
- [x] Sem bulk actions (mover múltiplos jobs de uma vez)
- [x] Sem export/import de dados
- [x] Sem histórico/versioning de peças (substitui versão)
- [x] Sem real-time (polling para atualizar kanban)
- [x] File storage local (sem S3/CDN)

---

## Personas no MVP

### Persona Principal (foco total)

**Nome:** Gestor da Agência (Rafael)  
**Quem é:** Dono ou gerente de agência de marketing digital com 2-15 pessoas  
**Job to be Done:** Ter visão total da produção e oferecer experiência profissional de aprovação ao cliente

### Persona Secundária (suportada)

**Nome:** Criativo (Camila)  
**Quem é:** Designer ou editor que executa os jobs  
**Job to be Done:** Receber briefing completo e entregar sem burocracia

### Persona Terciária (suportada com escopo limitado)

**Nome:** Cliente da Agência (Fernando)  
**Quem é:** Contratante da agência que precisa aprovar peças  
**Job to be Done:** Aprovar peças rapidamente sem complicação

### Personas FORA do MVP

| Persona | Por que não agora |
|---------|------------------|
| Gestor de Tráfego | Foco é produção criativa, não gestão de mídia paga |
| Atendimento/Account Manager | Em agência pequena, o próprio gestor faz esse papel |
| Financeiro | Billing e controle financeiro fora do escopo |

---

## Fluxos Críticos

### Fluxo 1: Criação e Execução de Job

```
1. Gestor clica "Novo Job"
2. Seleciona cliente e tipo de job
3. Preenche briefing estruturado + anexa arquivos
4. Define prazo e atribui ao criativo
5. Sistema cria job no status "Novo"
6. Sistema envia email ao criativo
7. Criativo abre o job, lê briefing completo
8. Criativo move para "Em Criação"
9. Criativo executa peça (fora do BriefFlow)
10. Criativo faz upload da peça no job
11. Criativo move para "Revisão Interna"
12. Gestor confere e move para "Aguardando Aprovação"
13. Sistema gera link de aprovação e envia email ao cliente
```

### Fluxo 2: Aprovação do Cliente

```
1. Cliente recebe email com link
2. Acessa portal de aprovação (sem login)
3. Vê peça em destaque
4a. Aprova → status "Aprovado", emails enviados
4b. Pede revisão com comentário → status volta, emails enviados
5. Se revisão: criativo ajusta, faz novo upload, ciclo repete
```

### Fluxo 3: Registro e Onboarding

```
1. Gestor acessa /register
2. Preenche nome, email, senha, nome da agência
3. Sistema cria conta + workspace
4. Redireciona ao dashboard vazio
5. Sugere: "Comece cadastrando seu primeiro cliente"
6. Após criar cliente: "Agora crie seu primeiro job"
7. Sugere convidar membros do time
```

---

## Stack do MVP

### Escolhas Definitivas

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| Frontend | Angular (Latest) + Standalone Components | Stack do Danilo, performance, DX |
| Styling | Tailwind + PrimeNG | Velocidade, consistência, componentes prontos |
| Backend | Java 21 + Spring Boot 3 | Stack do Danilo, enterprise-ready |
| Database | PostgreSQL | Robusto, gratuito, suporte a JSON |
| Auth | Spring Security + JWT | Standard, stateless |
| Email | SMTP (definir provedor) | Notificações essenciais |
| Migrations | Flyway | Versionamento de schema |
| Mapper | MapStruct | Performance, type-safe |
| Deploy | Docker + Docker Compose | Portável, reproduzível |

### O que NÃO usar (complexidade desnecessária)

- [x] GraphQL (REST é suficiente para todos os casos do MVP)
- [x] WebSocket (polling resolve para o volume do MVP)
- [x] Micro-frontends (monolito Angular é suficiente)
- [x] Kubernetes (Docker Compose resolve)
- [x] Multiple databases (um PostgreSQL resolve)
- [x] Message queues (email síncrono ou @Async resolve)
- [x] Redis (sem necessidade de cache distribuído no MVP)
- [x] Elasticsearch (busca com LIKE/trgm resolve)

---

## Timeline Estimado

| Fase | Duração | Entregáveis |
|------|---------|-------------|
| Setup | 5 dias | Projeto base (Angular + Spring Boot), auth (registro/login/JWT), Docker Compose, database schema inicial |
| Core: Clientes + Jobs | 7 dias | CRUD clientes, criação de jobs com briefing, upload de arquivos |
| Core: Kanban | 5 dias | Board com drag & drop, status, filtros básicos |
| Aprovação | 7 dias | Portal público, link com token, aprovação/revisão, notificações email |
| Dashboard | 4 dias | Dashboard do gestor, métricas por status/cliente/criativo |
| Polish | 5 dias | Ajustes UX, bugs, testes, responsividade tablet |
| Beta | 3 dias | Deploy, setup com agência da esposa, feedback |
| **Total** | **~36 dias** | |

---

## Definition of Done (MVP)

O MVP está pronto quando:

- [ ] Todas as features P0 funcionando end-to-end
- [ ] Fluxos críticos testados (criar job → aprovar)
- [ ] Deploy em ambiente acessível externamente
- [ ] Esposa do Danilo usando ativamente na agência
- [ ] Pelo menos 1 cliente da agência aprovou uma peça pelo portal
- [ ] Emails de notificação chegando corretamente
- [ ] Sem erros críticos ou bloqueantes

---

## Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Scope creep | Alta | Alto | Revisar este documento semanalmente, dizer "não" para qualquer feature fora do P0 |
| Tech debt | Média | Médio | Aceitar para MVP, documentar com TODO, refatorar na v1.1 |
| Upload de arquivos grandes | Média | Médio | Limite de 50MB, tratamento de erro claro, chunked upload na v1.1 |
| Agência não adotar (resistência a mudar) | Média | Alto | Beta gratuito, onboarding guiado, suporte direto |
| Apenas 1 testadora (viés) | Alta | Médio | Buscar 3-5 agências adicionais antes do launch público |
| Email em spam | Média | Médio | Configurar SPF/DKIM, usar provedor confiável |

---

## Hipóteses a Validar

| Hipótese | Como validar | Sucesso = |
|----------|-------------|-----------|
| Agências pequenas sofrem com ferramentas fragmentadas | Signups no beta + entrevistas | > 10 agências em 30 dias |
| Briefing estruturado reduz retrabalho | Comparar revisões antes/depois | Média < 2 revisões por job |
| Portal de aprovação é melhor que Excel | NPS do cliente da agência | NPS > 7 |
| Gestor valoriza visibilidade (dashboard) | Frequência de acesso ao dashboard | Acesso diário |
| Pagariam R$79-179/mês | Pesquisa de disposição a pagar pós-beta | > 30% dos beta users |

---

## Próximos Passos Pós-MVP

Depois de validar o MVP, considerar:

1. [ ] **v1.1:** Status customizáveis, histórico de versões, filtros avançados, templates de briefing
2. [ ] **v1.1:** Alerta de prazo por email, duplicar job
3. [ ] **v1.2:** Markup visual em imagens, timesheet, billing com Stripe
4. [ ] **v2:** Integração WhatsApp, white-label, app mobile, relatório mensal PDF

---

## Regra de Ouro

Quando em dúvida se algo entra no MVP, pergunte:

> "Posso validar se agências adotariam o BriefFlow SEM essa feature?"

Se sim → Não entra no MVP.
