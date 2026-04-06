# Lessons Learned

> Arquivo de auto-melhoria contínua. Cada entrada é uma regra acionável que previne erros repetidos entre sessões.
> Consultar este arquivo no início de cada sessão de implementação.

## [Angular] — Sempre invocar frontend-design antes de implementar UI
- **Erro:** Implementei login, register e dashboard pages direto do plano sem invocar a skill `frontend-design`
- **Regra:** ANTES de escrever qualquer HTML/template/estilo de componente Angular, invocar `frontend-design`. Sem exceções — mesmo que o plano já tenha o template pronto
- **Contexto:** Toda task que cria ou modifica interface visual (pages, modais, formulários, cards, dashboards)
