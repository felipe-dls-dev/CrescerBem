# Crescer Bem — instruções para agentes

Este repositório abriga o aplicativo **Crescer Bem**, voltado ao acompanhamento da saúde de recém-nascidos.

## Objetivo do produto

Permitir que responsáveis registrem e acompanhem calendário de eventos, vacinas, peso, sintomas/mal-estar e outros marcos de saúde do bebê. O aplicativo não substitui acompanhamento médico.

## Colaboração via MCP

Fable, Codex, Claude e Antigravity usam o servidor MCP **orquestrador** como fonte de verdade do trabalho. Antes de qualquer alteração:

1. Consulte `painel` e leia mensagens não lidas.
2. Assuma uma tarefa com `assumir_tarefa`.
3. Reserve os arquivos com `reservar_arquivos` (a tarefa pode já reservá-los).
4. Registre decisões, progresso e bloqueios com `atualizar_tarefa` e `enviar_mensagem`.
5. Ao terminar, valide a alteração, revise o diff e use `concluir_tarefa` com resumo, validação e arquivos alterados.

Nunca edite arquivo reservado para outro agente. Caso seja necessário, envie uma mensagem ao responsável e aguarde a liberação ou um acordo explícito.

## Papéis preferenciais

| Agente | Foco principal |
| --- | --- |
| Fable | Produto, requisitos, voz da marca e conteúdo educativo em linguagem simples |
| Claude | Desenvolvimento de código, arquitetura, integrações e testes automatizados |
| Codex | Interface do aplicativo: componentes, design system, responsividade e acessibilidade na UI |
| Antigravity | QA: estratégia de testes, testes de fluxo, regressão, qualidade e reporte de falhas |

Os papéis orientam a divisão inicial; qualquer agente pode assumir uma tarefa disponível que esteja dentro de sua capacidade e sem conflito de arquivos. O orquestrador atualmente executa diretamente apenas Claude, Codex e Antigravity; o trabalho do Fable é registrado no quadro como `qualquer`, até que seu conector de execução seja configurado.

## Princípios do projeto

- Tratar dados de saúde infantil como sensíveis: coletar o mínimo necessário e não registrar dados reais em testes, exemplos ou logs.
- Sempre exibir informação de caráter educativo; alertas não devem diagnosticar nem substituir profissional de saúde.
- Projetar para uso rápido por responsáveis cansados: linguagem simples, acessibilidade e poucos passos.
- Manter segredos fora do repositório e fornecer apenas arquivos `.example` para configuração.

## Padrão de handoff

Ao encerrar uma tarefa, informar no resumo:

- o que mudou;
- como foi validado;
- decisões que afetam outras partes;
- pendências, riscos ou próximos passos.

Consulte `docs/colaboracao-mcp.md` para o fluxo completo e `docs/backlog-inicial.md` para a divisão inicial do trabalho.
