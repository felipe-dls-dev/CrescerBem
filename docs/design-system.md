# Sistema visual — Crescer Bem

Este guia orienta as telas de **Hoje**, **Calendário** e **Registros**. A interface deve ser calma, acolhedora e rápida de usar, inclusive com uma mão e à noite. O app organiza informações; não diagnostica nem substitui acompanhamento profissional.

## Cor

O tema usa verde-sálvia como cor principal, pêssego para ações de apoio e lavanda para informação complementar. Os valores abaixo foram medidos pela fórmula de contraste WCAG 2.x contra os fundos padrão: claro `#FAF8F4` e escuro `#121413`. A razão `texto` é da combinação indicada no token; a razão `claro/escuro` é para marcadores, ícones e estados fixos.

| Token | Hex | Uso | Contraste medido |
| --- | --- | --- | --- |
| `primary` claro | `#2F6F6A` | Ação principal | branco: 5,83:1 |
| `secondary` claro | `#9A4A38` | Ação de apoio | branco: 6,16:1 |
| `tertiary` claro | `#63559A` | Informação complementar | branco: 6,37:1 |
| `onSurface` claro | `#1B1C1A` | Texto principal | fundo claro: 16,13:1 |
| `onSurfaceVariant` claro | `#45474A` | Texto secundário | fundo claro: 8,79:1 |
| `error` claro | `#B3261E` | Erro de campo | branco: 6,54:1 |
| `primary` escuro | `#96D3CB` | Ação principal | fundo escuro: 10,99:1 |
| `secondary` escuro | `#FFB4A2` | Ação de apoio | fundo escuro: 10,84:1 |
| `tertiary` escuro | `#CBBEFF` | Informação complementar | fundo escuro: 10,88:1 |
| `onSurface` escuro | `#E3E3DF` | Texto principal | fundo escuro: 14,38:1 |
| `onSurfaceVariant` escuro | `#C6C7C3` | Texto secundário | fundo escuro: 10,89:1 |
| `error` escuro | `#F2B8B5` | Erro de campo | fundo escuro: 10,83:1 |

### Categorias e status

As cores abaixo são reservadas para marcadores do calendário, ícones e chips. Todas superam 3:1 nos dois fundos; em tela clara também superam 4,5:1. Não use texto pequeno sobre essas cores sem verificar a combinação específica.

| Token | Hex | Claro / escuro |
| --- | --- | --- |
| `CategoryColors.moment` | `#C2185B` | 5,54:1 / 3,15:1 |
| `CategoryColors.vaccine` | `#2E7D32` | 4,83:1 / 3,61:1 |
| `CategoryColors.appointment` | `#1565C0` | 5,42:1 / 3,22:1 |
| `CategoryColors.health` | `#B94600` | 5,03:1 / 3,47:1 |
| `CategoryColors.growth` | `#9333EA` | 5,07:1 / 3,44:1 |
| `CategoryColors.feeding` | `#00796B` | 5,02:1 / 3,48:1 |
| `StatusColors.done` | `#2E7D32` | 4,83:1 / 3,61:1 |
| `StatusColors.pending` | `#9A5A00` | 5,16:1 / 3,38:1 |
| `StatusColors.overdue` | `#C62828` | 5,30:1 / 3,29:1 |
| `StatusColors.neutral` | `#626563` | 5,56:1 / 3,14:1 |

## Tipografia

A fonte é a do sistema Android, sem download de fontes. Os tamanhos usam `sp`, portanto acompanham a escala de fonte do sistema.

| Estilo | Tamanho / entrelinha | Uso |
| --- | --- | --- |
| `headlineSmall` | 24sp / 32sp, semibold | Título da tela |
| `titleLarge` | 22sp / 28sp, semibold | Destaque em cartão |
| `titleMedium` | 18sp / 24sp, semibold | Título de seção |
| `titleSmall` | 16sp / 22sp, medium | Subtítulo e item destacado |
| `bodyLarge` | 16sp / 24sp | Texto principal e formulário |
| `bodyMedium` | 14sp / 20sp | Texto de apoio |
| `bodySmall` | 12sp / 16sp | Metadados, nunca como única instrução crítica |
| `labelLarge` | 14sp / 20sp, semibold | Botões e chips |

## Formas, espaço e toque

- Raio pequeno: 8dp; médio: 16dp; grande: 24dp.
- A grade é de 8dp: use 8, 16, 24 ou 32dp para separações principais; 4dp somente entre elementos intimamente relacionados.
- Margem horizontal de tela: 16dp.
- Todo controle acionável tem área mínima de 48×48dp, mesmo quando o ícone for menor.
- Prefira cartões com espaço interno de 16dp e respiro de 8dp entre título, conteúdo e ação.

## Padrões de componentes

### Estrutura das telas-chave

| Tela | Hierarquia proposta | Ação principal |
| --- | --- | --- |
| Hoje | Barra superior com bebê ativo → título/idade → cartão de próximo compromisso → seções recolhíveis de registros e lembretes → aviso educativo quando necessário | “Registrar” |
| Calendário | Título e mês → filtros por chips → grade mensal com marcadores e hoje destacado → cartão “Dia selecionado” com a linha do tempo | “Adicionar neste dia” |
| Registro | Título do tipo → campos obrigatórios primeiro → data e hora → detalhes opcionais em seção recolhível → fotos → aviso educativo, se aplicável | “Registrar” |

Em telas longas, deixe a ação principal acessível sem encobrir o último campo e apresente o aviso educativo antes do botão quando ele for relevante ao registro.

### Hoje, calendário e registros

- **Seção recolhível em cartão:** título, resumo de estado e chevron no cabeçalho inteiro tocável. Preserve o estado expandido ao voltar; informe expandido/recolhido para leitor de tela.
- **Chips de escolha:** use para filtros de calendário, sintomas e intensidade. Cada chip tem rótulo textual, seleção clara e não ultrapassa uma linha sem alternativa de quebra ou rolagem horizontal acessível.
- **Data e hora:** campos separados, com rótulo visível, valor atual e seletor do sistema ao toque. Pré-preencha hoje/agora quando fizer sentido, mas permita editar.
- **Botão “Registrar”:** ação principal evidente e com rótulo, disponível na tela inicial e como ação de adicionar no dia selecionado. Depois de salvar, confirme em texto e mantenha os dados visíveis.
- **Barra inferior:** cinco destinos — Hoje, Calendário, Bebê, Saúde e Alimentação. Ícone e rótulo sempre juntos; o destino atual tem estado selecionado além da cor.

### Cartões educativos e de atenção

- **Aviso educativo:** use superfície tonal suave, ícone informativo e texto curto: “O Crescer Bem ajuda a organizar registros. Ele não substitui a avaliação de um profissional de saúde.”
- **Sinais de atenção:** use `StatusColors.overdue` como apoio visual, ícone de atenção e título “Esse sinal merece atenção”. O texto orienta procurar um profissional de saúde o quanto antes, sem diagnosticar ou usar “normal/anormal” e “grave”.

### Vacinas

| Status | Aparência e texto obrigatório |
| --- | --- |
| Aplicada | Ícone de confirmação, `StatusColors.done`, rótulo “Aplicada”. |
| Prevista | Ícone de calendário, `StatusColors.pending`, rótulo “Prevista”. |
| Atrasada | Ícone de atenção, `StatusColors.overdue`, rótulo “Atrasada”. |
| Não se aplica | Ícone riscado, `StatusColors.neutral`, rótulo “Não se aplica”. |

No topo da lista, mantenha: “Sugestão baseada no calendário nacional. A caderneta e o pediatra são a referência oficial.”

## Estados vazios

Estados vazios explicam o que falta e oferecem a próxima ação em uma frase. Exemplos:

- “Nenhuma mamada hoje ainda. Toque em + para registrar.”
- “Não há registros neste dia. Toque em Adicionar neste dia.”
- “Ainda não há medidas de peso. Registre a primeira medida quando tiver o valor.”

Evite ilustrações que escondam a ação ou exijam rolagem para alcançá-la.

## Acessibilidade

- Forneça `contentDescription` útil para imagens e ícones de ação; use `null` apenas em ícones puramente decorativos.
- A ordem de leitura deve seguir a ordem visual e de tarefa: título, contexto, conteúdo, ação principal e navegação.
- Não dependa somente da cor: categorias e status sempre incluem texto, ícone ou padrão.
- Mantenha rótulos persistentes nos campos; placeholder não substitui rótulo.
- Respeite escala de fonte, contraste e configurações de tema do sistema. Em fonte ampliada, permita que cartões cresçam e evite altura fixa para texto.
- Não use cor de erro para comunicar alerta educativo sem o texto de contexto correspondente.

## Atualização de 17/11/2026: blocos, marca d'água e tema

Pedido do responsável pelo produto (tarefa 33).

| Token | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| `surfaceContainerLow` | `#E5ECE9` | `#1F2423` | Fundo das seções recolhíveis e do resumo do dia |
| `surfaceContainer` | `#DFE7E3` | `#242A28` | Barra inferior |
| `surfaceContainerHigh` | `#D8E1DD` | `#2C3230` | Superfícies elevadas |
| `surfaceContainerHighest` | `#D2DBD7` | `#363C3A` | Cartões padrão do Material |
| `outlineVariant` | `#BFCAC5` | `#3F4644` | Borda de 1dp dos blocos |

- Os blocos usam sálvia acinzentado com borda fina, para se destacarem do fundo creme `#FAF8F4`.
- A logo (coração com broto, `ui/theme/LogoMark.kt`) fica centralizada ao fundo de todas as telas, com 8% de opacidade no claro e 10% no escuro. Os blocos ficam por cima e a leitura não é afetada.
- O botão de sol ou lua na barra superior alterna entre claro e escuro. Em Ajustes há "Automático", "Claro" e "Escuro". A escolha fica salva no aparelho.
- No modo escuro, ícones de categoria e etiquetas de status são clareados automaticamente para manter o contraste.
