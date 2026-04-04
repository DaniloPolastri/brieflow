# BriefFlow

**Data:** Abril 2026  
**Autor:** Danilo  
**Status:** Validating

---

## 💡 Problema

**Em uma frase:**
> Agências de marketing pequenas/médias gerenciam briefings, produção criativa e aprovações de clientes usando 4+ ferramentas desconectadas (WhatsApp + Canva + Trello + Excel), causando retrabalho, informação perdida e zero visibilidade para o gestor.

**Contexto:**
O fluxo atual de uma agência típica é caótico: o cliente manda o briefing fragmentado pelo WhatsApp (muitas vezes incompleto — sem paleta, sem texto, sem referências), o designer executa no Canva, atualiza o status no Trello, e a peça final vai para um Excel compartilhado onde o cliente aprova ou pede revisão. Cada ferramenta é uma ilha. O dono da agência não tem visão real do que está acontecendo, quem está sobrecarregado, ou o que está atrasado. O designer perde tempo caçando informação em vez de criar.

---

## ✅ Solução

**Em uma frase:**
> BriefFlow centraliza briefings estruturados, produção via kanban e aprovação do cliente em uma única plataforma — eliminando o caos de ferramentas fragmentadas.

**Como funciona:**
O gestor cria um job com briefing estruturado (formulário guiado por tipo: post, stories, vídeo). O designer recebe o job com todas as informações organizadas, executa e faz upload da peça. O cliente recebe um link público onde vê a peça e aprova com um clique ou pede revisão com comentário. O gestor acompanha tudo via dashboard com visão de status, atrasos e carga por criativo.

---

## 👤 Público-Alvo

**Persona principal:**
> Dono/Gestor de agência de marketing digital com 2-15 pessoas. Precisa de visibilidade sobre a produção e um fluxo profissional de aprovação com o cliente. Hoje gerencia tudo "no grito" via WhatsApp e planilhas.

**Early adopters:**
> Agências pequenas (2-10 pessoas) que já usam Trello ou similar mas sentem que não resolve. Especialmente as que têm pelo menos 5 clientes ativos e sentem o caos escalar.

---

## 🎯 Proposta de Valor

**Por que escolher o BriefFlow?**
> Tudo que sua agência precisa entre "cliente pediu" e "cliente aprovou" — em um lugar só, sem complexidade.

**Alternativas atuais:**
- WhatsApp + Trello + Excel (gratuito mas caótico, sem visibilidade)
- Operand / iClips / Studio (completos mas caros e complexos para agência pequena)

**Seu diferencial:**
- Simples como Trello, mas feito especificamente para o fluxo de agência
- Portal de aprovação do cliente via link público (sem login, sem fricção)
- Preço acessível para agência pequena (a partir de R$79/mês)
- Design limpo e moderno (referência Operand/Linear)

---

## 💰 Modelo de Negócio

**Monetização:**
> SaaS com assinatura mensal recorrente, modelo freemium para captura + planos pagos por tamanho de equipe.

**Pricing inicial:**

| Plano | Preço | Target |
|-------|-------|--------|
| Free | R$0 | Freelancer ou agência testando (1 usuário, 2 clientes, 20 jobs/mês) |
| Pro | R$79/mês | Agência pequena (até 10 usuários, clientes ilimitados) |
| Agency | R$179/mês | Agência em crescimento (ilimitado, dashboard avançado, prioridade) |

---

## 📊 Métricas de Sucesso

**North Star Metric:**
> Jobs completados (briefing → aprovação) por semana por agência

**Metas iniciais (3 meses):**
- [ ] 10 agências usando ativamente (pelo menos 5 jobs/semana cada)
- [ ] 50+ clientes de agência usando o portal de aprovação
- [ ] Taxa de retenção 7 dias > 60%

---

## 🚀 MVP Scope

**O que entra:**
- Autenticação (email + senha com JWT)
- Cadastro e gestão de clientes da agência
- Criação de jobs com briefing estruturado por tipo
- Kanban de produção com status customizáveis
- Atribuição de jobs a criativos
- Upload de peças finalizadas
- Portal de aprovação via link público (sem login do cliente)
- Dashboard do gestor (jobs por status, atrasados, por cliente)
- Notificações por email

**O que NÃO entra:**
- Markup visual em imagens (v1.1)
- Timesheet / controle de horas (v1.1)
- Relatório mensal automático para cliente (v1.2)
- Integração com WhatsApp (v2)
- White-label (v2)
- App mobile nativo
- Integração com Canva

---

## 🛠 Stack

| Camada | Tecnologia |
|--------|------------|
| Frontend | Angular (Latest), Standalone Components, Signals, RxJS + Tailwind + PrimeNG |
| Backend | Java 21+, Spring Boot 3+, Spring Security, Spring Data JPA, JWT |
| Database | PostgreSQL |
| Deploy | A definir |

---

## ⏱ Timeline

| Marco | Data/Prazo |
|-------|------------|
| Setup + Auth + Base | Semana 1-2 |
| Core Features (Jobs + Kanban) | Semana 3-5 |
| Portal de Aprovação | Semana 6-7 |
| Dashboard + Polish | Semana 8-9 |
| Beta com esposa + agência dela | Semana 10 |

---

## ❓ Hipóteses a Validar

1. [ ] Agências pequenas realmente sofrem o suficiente com ferramentas fragmentadas para adotar uma nova ferramenta
2. [ ] O portal de aprovação via link público é mais eficiente que o Excel compartilhado
3. [ ] Donos de agência pagariam R$79-179/mês por essa solução
4. [ ] Briefings estruturados reduzem rodadas de revisão significativamente

---

## 🔗 Links

- Repo: [A definir]
- Docs: [A definir]
- Design: [A definir]
- Produção: [A definir]

---

## 📝 Notas

- A esposa do Danilo trabalha como designer em agência e será a primeira testadora
- O fluxo real observado: WhatsApp (briefing) → Canva (execução) → Trello (status) → Excel (aprovação do cliente)
- WhatsApp não será substituído — continuará para comunicação rápida. BriefFlow substitui Trello + Excel + organização do briefing
- Referência visual principal: Operand (design), Linear (UX), Resend (clean)
