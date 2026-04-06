# Lessons Learned

> Arquivo de auto-melhoria contínua. Cada entrada é uma regra acionável que previne erros repetidos entre sessões.
> Consultar este arquivo no início de cada sessão de implementação.

## [Angular] — Sempre invocar frontend-design antes de implementar UI
- **Erro:** Implementei login, register e dashboard pages direto do plano sem invocar a skill `frontend-design`
- **Regra:** ANTES de escrever qualquer HTML/template/estilo de componente Angular, invocar `frontend-design`. Sem exceções — mesmo que o plano já tenha o template pronto
- **Contexto:** Toda task que cria ou modifica interface visual (pages, modais, formulários, cards, dashboards)

## [Geral] — Marcar tasks no plano em tempo real e executar parallel groups em paralelo
- **Erro:** Executei tasks do PARALLEL GROUP A sequencialmente e não marquei `[x]` no plano conforme completava cada task
- **Regra:** (1) Ao completar cada task, marcar imediatamente `[x]` no arquivo do plano e commitar. (2) Tasks no mesmo `⚡ PARALLEL GROUP` DEVEM ser despachadas em paralelo via agentes simultâneos. Nunca executar sequencialmente tasks marcadas como paralelas.
- **Contexto:** Toda execução de plano de implementação — vale para subagent-driven-development e executing-plans

## [Angular] — Usar templateUrl com arquivo HTML separado
- **Erro:** Coloquei templates grandes (50+ linhas) inline no `template:` do componente, deixando o .ts verboso e difícil de ler
- **Regra:** Sempre usar `templateUrl: './nome.component.html'` com arquivo HTML separado. Inline `template:` só para componentes com menos de ~10 linhas de HTML
- **Contexto:** Todos os componentes Angular do projeto
