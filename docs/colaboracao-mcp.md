# Colaboração MCP — Crescer Bem

## Visão geral

O servidor MCP `orquestrador` é o quadro compartilhado entre Fable, Codex, Claude e Antigravity. Ele evita trabalho duplicado ao registrar tarefas, responsáveis, reservas de arquivos, mensagens e resultados de delegações.

```
Pedido do usuário
       |
       v
Criar tarefa no orquestrador
       |
       v
Assumir tarefa + reservar arquivos
       |
       +--> Fable: produto, requisitos e conteúdo
       +--> Claude: código, arquitetura e testes automatizados
       +--> Codex: interface e acessibilidade na UI
       +--> Antigravity: QA, regressão e reporte de falhas
       |
       v
Revisão do diff e validação
       |
       v
Concluir tarefa e liberar reservas
```

## Protocolo operacional

### 1. Início de sessão

Cada agente chama `painel` para verificar tarefas, execuções, mensagens e arquivos já reservados. Se houver mensagens não lidas, chama `ler_mensagens` antes de iniciar outra atividade.

### 2. Planejamento e delegação

Quem estiver coordenando quebra o pedido em tarefas pequenas, com critérios de aceite verificáveis e arquivos/pastas previstos. Use `criar_tarefa` e atribua `claude`, `codex`, `antigravity` ou `qualquer`. Enquanto não houver um conector de execução para o Fable, suas tarefas devem ser atribuídas como `qualquer` e identificadas no título ou descrição como trabalho do Fable.

Tarefas que possam ser feitas em paralelo devem usar conjuntos distintos de arquivos. Quando uma tarefa precisar de saída de outra, registre os IDs em `depende_de`.

### 3. Execução segura

O agente chama `assumir_tarefa` e confirma a reserva com `reservar_arquivos` antes de editar. Se houver conflito, não editar: usar `enviar_mensagem` para combinar a mudança ou redividir o trabalho.

Para trabalho autônomo, a coordenação pode usar `delegar` com `modo: leitura` para pesquisa/revisão ou `modo: escrita` para implementação isolada. Essa delegação está disponível para Claude, Codex e Antigravity; Fable participa por handoff documentado até que seu conector MCP seja adicionado. Toda saída delegada deve passar por revisão de diff e testes antes de ser concluída.

### 4. Comunicação

Use `enviar_mensagem` quando uma decisão afetar outro agente: contrato de API, estrutura de dados, mudança de rota, componente compartilhado, risco de privacidade ou bloqueio. A mensagem deve citar a tarefa e a ação esperada.

### 5. Revisão e término

Antes de encerrar, o responsável revisa as mudanças e executa a validação adequada. Em seguida, chama `concluir_tarefa` com resumo objetivo, comandos/resultados de validação, arquivos alterados e pendências. Isso libera as reservas.

## Convenções para as tarefas

| Campo | Regra |
| --- | --- |
| Título | Verbo no infinitivo + resultado, por exemplo: “Definir modelo de registro de peso” |
| Critérios | Observáveis e testáveis; evitar “ficar bom” |
| Arquivos | Informar arquivos ou pastas que serão tocados |
| Prioridade | `alta` para bloqueios, segurança e base do produto; `normal` por padrão |
| Status | `em_revisao` para trabalho delegado aguardando validação; `concluida` apenas após revisão |

## Dados de saúde

Por envolver recém-nascidos, decisões sobre alertas, armazenamento, compartilhamento e exclusão de dados precisam de revisão explícita de privacidade antes da implementação. O produto deve deixar claro que registros e lembretes não são orientação clínica ou diagnóstico.
