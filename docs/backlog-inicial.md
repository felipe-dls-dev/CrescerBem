# Backlog inicial — Crescer Bem

Estas são as primeiras frentes sugeridas para o quadro MCP. Crie-as no orquestrador quando o desenvolvimento começar; os IDs serão definidos pelo próprio servidor.

| Ordem | Tarefa | Responsável sugerido | Dependência | Critério de aceite |
| ---: | --- | --- | --- | --- |
| 1 | Definir escopo do MVP, fluxos e voz do produto | Fable | — | Fluxos de cadastro do bebê, calendário, vacinas, peso e mal-estar documentados |
| 2 | Definir princípios de privacidade e dados sensíveis | Antigravity (QA) | 1 | Política técnica de coleta, retenção, exportação e exclusão definida |
| 3 | Criar arquitetura inicial do aplicativo | Claude | 1, 2 | Estrutura do projeto, modelo de dados e estratégia de testes documentados |
| 4 | Criar sistema visual e protótipo das telas principais | Codex | 1 | Protótipo acessível para início, calendário e registros aprovado para implementação |
| 5 | Implementar base de dados e registros do bebê | Claude | 2, 3 | Criar, ler, editar e excluir registros com testes |
| 6 | Implementar calendário e lembretes | Claude | 3, 4, 5 | Eventos de vacina e acompanhamento aparecem no calendário |
| 7 | Implementar histórico de peso e crescimento | Claude | 3, 4, 5 | Peso pode ser lançado e visualizado cronologicamente |
| 8 | Implementar registro de mal-estar | Claude | 3, 4, 5 | Responsável registra data, sintomas, observações e orientação de procurar atendimento quando necessário |
| 9 | Executar QA de linguagem, acessibilidade e fluxos | Antigravity | 4, 6, 7, 8 | Navegação por teclado, contraste e textos claros verificados |
| 10 | Executar QA de segurança e prontidão do MVP | Antigravity | 2, 5, 6, 7, 8 | Riscos de privacidade e conteúdo sensível revisados |

## Primeira divisão recomendada

As tarefas 1 e 2 podem ocorrer em paralelo. A tarefa 4 começa após os fluxos da tarefa 1. A tarefa 3 depende das decisões de escopo e privacidade. A implementação começa somente depois de as decisões de dados e arquitetura estarem registradas. Até o conector do Fable ser disponibilizado, registre a tarefa 1 no orquestrador como `qualquer` e faça o handoff para o Fable fora da delegação automática.

## Frentes adicionadas em 16/11/2026 (escopo da tarefa 15)

O pedido do responsável pelo produto ampliou o MVP. As tarefas abaixo complementam a tabela inicial e seguem o mesmo padrão de dependências.

| Ordem | Tarefa | Responsável sugerido | Dependência | Critério de aceite |
| ---: | --- | --- | --- | --- |
| 11 | Implementar consultas agendadas e realizadas | Claude | 3, 5 | Consulta aparece em Hoje na data; pode ser marcada como realizada com orientações |
| 12 | Implementar registro de alimentação | Claude | 3, 5 | Materno, fórmula e introdução alimentar registrados com resumo diário |
| 13 | Implementar fotos por câmera e galeria nos registros | Claude | 3, 5 | Foto vinculada ao registro, armazenada em pasta privada, removida ao excluir |
| 14 | Implementar relatório em PDF por período | Claude | 5 a 8, 11 a 13 | PDF contém só o período e bebê escolhidos e pode ser compartilhado |
| 15 | Revisar UI das telas implementadas (design system, dropdowns, acessibilidade) | Codex | 4, 6 a 8, 11 a 14 | Telas seguem o protótipo e passam na revisão de acessibilidade |

Detalhes em `docs/escopo-mvp.md`.
