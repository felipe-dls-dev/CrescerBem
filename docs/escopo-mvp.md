# Escopo do MVP e fluxos principais — Crescer Bem

Documento de produto da tarefa 15. Define o que entra na primeira versão do aplicativo Android, como as telas se organizam e quais fluxos o responsável percorre. Serve de base para a arquitetura (tarefa 16), o protótipo visual (tarefa 17) e as implementações seguintes.

## 1. Objetivo e público

- **Objetivo**: permitir que pais e responsáveis registrem, em poucos toques, o dia a dia de saúde de um recém-nascido e consultem tudo depois, inclusive em um relatório em PDF para levar ao pediatra.
- **Público**: responsáveis cansados, muitas vezes usando o celular com uma mão só, à noite. Linguagem simples, botões grandes, poucos passos.
- **Limite claro**: o aplicativo é um diário organizado. Ele não diagnostica, não prescreve e não substitui o pediatra nem a caderneta oficial da criança.

## 2. Princípios de produto

1. Registrar em até 3 toques a partir da tela inicial.
2. Tudo funciona sem internet e sem criar conta. Os dados ficam só no aparelho.
3. Cada registro pode ter data/hora, observação livre e fotos.
4. Toda mensagem de alerta é educativa e termina orientando a procurar um profissional quando houver dúvida.
5. Nenhum dado real em testes, exemplos, capturas de tela ou logs.

## 3. Organização em telas e grupos

Navegação por barra inferior com cinco destinos. Dentro de cada destino, os assuntos aparecem em seções recolhíveis (dropdown/accordion) ou abas, para que a tela não fique longa.

| Destino | O que mostra | Seções (dropdown) |
| --- | --- | --- |
| **Hoje** | Resumo do dia: próximos compromissos, últimas vacinas, último peso, botão grande "Registrar" | Compromissos próximos · Registros de hoje · Lembretes |
| **Calendário** | Mês com marcadores coloridos por tipo de registro; ao tocar em um dia, lista o que houve e permite adicionar | Dia selecionado · Adicionar registro neste dia |
| **Bebê** | Perfil e crescimento | Dados básicos · Peso · Altura · Perímetro cefálico · Fotos do perfil |
| **Saúde** | Vacinas, consultas e eventos de saúde | Vacinas · Consultas · Sintomas/mal-estar · Medicamentos (P2) |
| **Alimentação** | Mamadas e introdução alimentar | Aleitamento materno · Fórmula · Introdução alimentar · Água/outros |
| **Relatórios** (acessível pelo menu de Hoje e pelo perfil) | Escolha de período e tipos, pré-visualização e geração do PDF | Período · Tipos de registro · Gerar PDF |
| **Ajustes** (ícone no topo) | Preferências, exportar/apagar dados, sobre e avisos | Dados e privacidade · Sobre |

Ação flutuante **"+ Registrar"** disponível em todas as telas: abre uma lista de tipos (Momento, Vacina, Consulta, Sintoma, Medida, Alimentação) e pré-preenche a data com hoje ou com o dia selecionado no calendário.

## 4. Entidades do MVP

| Entidade | Campos essenciais | Campos opcionais |
| --- | --- | --- |
| Bebê | nome ou apelido, data de nascimento | sexo, hora do nascimento, peso e altura ao nascer, tipo sanguíneo, foto |
| Medida de crescimento | data, tipo (peso / altura / perímetro cefálico), valor | observação, local (casa, posto, consultório), fotos |
| Vacina | nome, data aplicada **ou** data prevista, status (prevista / aplicada / atrasada / não se aplica) | dose, lote, local, reação observada, observação, foto da caderneta |
| Consulta | título ou especialidade, data e hora, status (agendada / realizada / cancelada) | profissional, local, motivo, orientações recebidas, fotos de receitas/exames |
| Evento de saúde (sintoma/mal-estar) | data e hora, lista de sintomas (pré-definidos + livre), intensidade (leve / moderada / forte) | temperatura, duração, o que foi feito, procurou atendimento?, fotos |
| Alimentação | data e hora, tipo (materno / fórmula / fruta / papa salgada / água / outro) | lado (E/D) e duração para materno; quantidade em ml para fórmula/água; alimento e aceitação para introdução alimentar; fotos |
| Momento (acontecimento do dia) | data e hora, título | descrição, fotos |
| Foto/anexo | caminho local, data, entidade vinculada | legenda |

Todo registro pertence a um bebê. O MVP suporta **vários bebês** no mesmo aparelho (gêmeos são comuns), com um bebê ativo selecionado no topo da tela.

## 5. Fluxos principais

### 5.1 Primeiro uso e cadastro do bebê
1. Tela de boas-vindas com aviso curto: "O Crescer Bem organiza registros. Ele não substitui o pediatra."
2. Formulário mínimo: nome ou apelido e data de nascimento. Botão "Começar".
3. Opcional, na mesma tela, seção recolhida "Mais dados do nascimento": peso, altura, perímetro cefálico, sexo, hora. Preencher aqui cria as primeiras medidas de crescimento.
4. Ao salvar, o app sugere o calendário vacinal brasileiro (ver 5.4) como vacinas **previstas** com base na data de nascimento. O responsável pode aceitar, editar ou pular.
5. Entra em **Hoje**.

### 5.2 Calendário
1. Abre no mês atual, com a data de hoje destacada e o dia do nascimento marcado.
2. Cada dia mostra pontos coloridos por tipo: vacina, consulta, sintoma, crescimento, alimentação, foto.
3. Tocar em um dia abre a lista de registros daquele dia (ordenada por hora) e o botão "Adicionar neste dia".
4. Tocar em um registro abre a edição; deslizar ou menu permite excluir com confirmação.
5. Filtro por tipo no topo (chips) para esconder, por exemplo, alimentação quando se quer ver só vacinas.

### 5.3 Dados básicos e crescimento
1. Em **Bebê**, cartão com nome, idade em dias/semanas/meses calculada automaticamente, foto.
2. Seção **Peso**: última medida em destaque, botão "Nova medida", lista cronológica e gráfico simples de evolução.
3. Mesmo padrão para **Altura** e **Perímetro cefálico**.
4. Formulário de medida: data (padrão hoje), valor com unidade fixa (kg com 3 casas, cm com 1 casa), observação, fotos.
5. Validação educativa: valores fora de faixa plausível (ex.: peso abaixo de 0,5 kg ou acima de 30 kg) pedem confirmação, sem bloquear.

### 5.4 Vacinas
1. Lista agrupada por idade recomendada (ao nascer, 2 meses, 3 meses, 4 meses...), cada grupo em dropdown.
2. Cada vacina tem status visual: prevista (cinza), aplicada (verde), atrasada (âmbar, prevista com data passada há mais de 30 dias), não se aplica (riscada).
3. Marcar como aplicada pede data (padrão hoje), dose, lote e foto da caderneta; tudo opcional exceto a data.
4. "Adicionar vacina" permite nome livre para vacinas da rede privada ou campanhas.
5. Texto fixo no topo: "Sugestão baseada no calendário nacional. A caderneta e o pediatra são a referência oficial."

Calendário sugerido no MVP (referência educativa, editável): BCG e Hepatite B ao nascer; aos 2 meses Penta, VIP, Pneumocócica 10 e Rotavírus; aos 3 meses Meningocócica C; aos 4 meses Penta, VIP, Pneumocócica 10 e Rotavírus; aos 5 meses Meningocócica C; aos 6 meses Penta e VIP; aos 9 meses Febre amarela; aos 12 meses Tríplice viral, Pneumocócica 10 (reforço) e Meningocócica C (reforço); aos 15 meses DTP, Hepatite A e Tetra viral. A lista fica em um arquivo de dados versionado para atualização sem mudar código.

### 5.5 Consultas
1. Lista em duas abas: **Próximas** e **Realizadas**.
2. Criar consulta: título/especialidade, data e hora, local, motivo. Status inicial "agendada".
3. Na data, aparece em **Hoje**. Depois dela, o app pergunta se foi realizada e abre campo "orientações recebidas" e fotos (receitas, pedidos de exame).
4. Lembrete local opcional 1 dia antes e 2 horas antes (P1).

### 5.6 Sintomas e mal-estar
1. "Registrar sintoma" abre lista de chips: febre, tosse, coriza, vômito, diarreia, cólica, irritabilidade, sono alterado, pele (assadura, manchas), recusa alimentar, outro.
2. Campos: data e hora, intensidade, temperatura (se febre), duração, o que foi feito, procurou atendimento.
3. Ao salvar com sinais de atenção (ex.: febre em bebê com menos de 3 meses, temperatura de 38 °C ou mais, vômitos repetidos, dificuldade para respirar), exibe cartão educativo: "Esses sinais merecem avaliação de um profissional de saúde o quanto antes." Nunca diagnostica.
4. Histórico cronológico com filtro por sintoma.

### 5.7 Alimentação
1. Tela com dropdowns por tipo. Botões rápidos no topo: "Mamou agora" (materno), "Fórmula", "Comida".
2. Aleitamento materno: lado iniciado (E/D), duração (cronômetro opcional ou minutos digitados), observação.
3. Fórmula e água: quantidade em ml, marca/preparo opcional.
4. Introdução alimentar (a partir de 6 meses, mas sem bloquear): alimento, forma (amassado, pedaço, papa), aceitação (aceitou bem / pouco / recusou), reação observada, foto.
5. Resumo do dia: número de mamadas, total de ml, alimentos novos experimentados.

### 5.7.1 Momentos
1. Registro livre para acontecimentos do dia a dia: primeiro sorriso, primeiro banho, visita dos avós.
2. Campos: data e hora, título, descrição e fotos.
3. Aparece no calendário e em Hoje. A lista completa fica em **Bebê**, na seção "Momentos".

### 5.8 Fotos
1. Em qualquer formulário, botões **"Tirar foto"** e **"Escolher da galeria"**. Permissões pedidas só no momento do toque.
2. Fotos são copiadas para a pasta privada do app e vinculadas ao registro. Nunca são gravadas na galeria pública sem pedido explícito.
3. Pré-visualização em miniaturas com opção de remover. Toque amplia.
4. Excluir um registro exclui suas fotos.

### 5.9 Relatório em PDF
1. Em **Relatórios**, escolher bebê, período (atalhos: últimos 7 dias, 30 dias, desde o nascimento, personalizado) e tipos de registro (todos marcados por padrão).
2. Pré-visualização em lista do que será incluído e contagem por tipo.
3. "Gerar PDF" cria o arquivo localmente com: capa (nome, idade, período), resumo de crescimento com tabela e último valor, vacinas (aplicadas e pendentes), consultas, sintomas, alimentação (resumo diário) e miniaturas de fotos (opcional, desligado por padrão para reduzir tamanho).
4. Ao final, botão "Compartilhar" usa a folha de compartilhamento do Android. O arquivo também fica salvo na pasta do app.
5. Rodapé fixo em todas as páginas: "Registro feito pela família no Crescer Bem. Não substitui documentos médicos."

### 5.10 Dados e privacidade (Ajustes)
1. Apagar um bebê e todos os registros, com confirmação em duas etapas.
2. Apagar todos os dados do app.
3. Exportar backup local (P1).
4. Texto "Sobre" com os avisos de uso.

## 6. Priorização

**P0 — sem isso o MVP não existe**
- Cadastro do bebê, múltiplos bebês, idade calculada.
- Calendário mensal com marcadores e lista por dia.
- Crescimento: peso, altura e perímetro cefálico com histórico.
- Vacinas com calendário sugerido, status e registro de aplicação.
- Consultas agendadas/realizadas.
- Sintomas/mal-estar com cartão educativo.
- Alimentação: materno, fórmula, introdução alimentar.
- Momentos do dia a dia com fotos.
- Fotos por câmera e galeria em qualquer registro.
- Relatório em PDF por período com compartilhamento.
- Exclusão de registros, do bebê e de todos os dados.

**P1 — logo depois**
- Lembretes locais para consultas e vacinas previstas.
- Gráficos de crescimento com curvas de referência (OMS), apresentadas como referência educativa.
- Backup e restauração local (arquivo).
- Cronômetro de mamada.

**P2 — mais tarde**
- Medicamentos e horários.
- Sono e fraldas.
- Compartilhamento entre responsáveis / sincronização em nuvem (exige nova revisão de privacidade).
- Tema escuro e tamanhos de fonte ampliados (acessibilidade do sistema já respeitada no MVP).

**Fora do escopo**
- Qualquer função de diagnóstico, triagem automática ou conselho clínico individualizado.
- Conta de usuário, login e envio de dados a servidores.

## 7. Voz do produto e textos padrão

- Tom: calmo, direto, acolhedor. Frases curtas. Sem jargão. "Você" em vez de "o usuário".
- Nunca usar: "diagnóstico", "normal/anormal", "grave". Preferir: "merece atenção", "vale conversar com o pediatra".
- Aviso padrão (telas de sintoma e relatório): "O Crescer Bem ajuda a organizar registros. Ele não substitui a avaliação de um profissional de saúde."
- Estados vazios ensinam: "Nenhuma mamada hoje ainda. Toque em + para registrar."

## 8. Critérios de aceite do MVP

1. Responsável cadastra bebê e vê a idade correta em Hoje.
2. Cada tipo de registro pode ser criado, editado e excluído, com data no calendário correta.
3. Fotos tiradas ou escolhidas ficam vinculadas ao registro e desaparecem ao excluí-lo.
4. Relatório em PDF de um período contém somente registros do período e do bebê selecionado.
5. Nenhum dado sai do aparelho sem ação explícita de compartilhar.
6. Textos revisados pelo QA de linguagem (tarefa 22) e privacidade (tarefa 23).
