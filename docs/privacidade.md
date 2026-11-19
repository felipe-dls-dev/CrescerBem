# Privacidade e dados sensíveis — Crescer Bem

Decisões da tarefa 14. Valem para o MVP e precisam de nova revisão antes de qualquer mudança que envie dados para fora do aparelho. A revisão final de segurança acontece na tarefa 23 (QA).

## 1. Coleta mínima

O aplicativo guarda apenas o que o responsável digita ou anexa. Nada é coletado automaticamente.

| Grupo | Dados | Obrigatório? |
| --- | --- | --- |
| Bebê | nome ou apelido, data de nascimento | sim |
| Bebê | sexo, hora do nascimento, tipo sanguíneo, observações, foto | não |
| Crescimento | data, tipo de medida, valor, local, observação | data, tipo e valor |
| Vacinas | nome, dose, data prevista ou aplicada, lote, local, reação, observação | nome e uma data |
| Consultas | título, data e hora, profissional, local, motivo, orientações | título e data |
| Sintomas | data e hora, sintomas, intensidade, temperatura, duração, o que foi feito | data e um sintoma |
| Alimentação | data e hora, tipo, lado, duração, quantidade, alimento, aceitação | data e tipo |
| Momentos | data e hora, título, descrição | data e título |
| Fotos | imagem e vínculo com o registro | não |

Não coletamos: nome dos pais, CPF, cartão SUS, endereço, telefone, e-mail, localização, contatos, identificadores de publicidade ou dados de uso (analytics).

## 2. Armazenamento

- Tudo fica **somente no aparelho**, em banco SQLite (Room) na área privada do aplicativo.
- Fotos são copiadas para a pasta privada do app (`files/photos`). Elas não aparecem na galeria do celular.
- PDFs gerados ficam na pasta privada do app (`files/reports`).
- Não existe conta, login, servidor, sincronização ou envio de dados no MVP.
- O aplicativo **não pede permissão de internet**.
- O backup automático do Android fica **desligado** (`allowBackup=false` e regras de extração vazias), para que dados de saúde não sejam copiados para a nuvem sem decisão explícita.

## 3. Permissões

| Recurso | Como funciona | Permissão |
| --- | --- | --- |
| Tirar foto | Abre o app de câmera do sistema, que grava direto no arquivo privado do Crescer Bem | nenhuma declarada pelo app |
| Escolher da galeria | Seletor de fotos do sistema (Photo Picker), que entrega só a imagem escolhida | nenhuma |
| Compartilhar PDF | Folha de compartilhamento do Android, com acesso temporário só ao arquivo | nenhuma |

O app não declara câmera, armazenamento, localização, contatos nem internet.

## 4. Retenção e exclusão

- Os dados ficam até o responsável apagar. Não há expiração automática.
- Excluir um registro remove também suas fotos (linha no banco e arquivo).
- Excluir um bebê remove todos os registros, fotos e o próprio cadastro, com confirmação.
- "Apagar todos os dados" em Ajustes limpa banco, fotos e PDFs gerados.
- Desinstalar o aplicativo apaga tudo, porque nada fica fora da área privada.

## 5. Exportação

- O único meio de exportar é o **relatório em PDF**, gerado localmente a pedido do responsável.
- O PDF só sai do aparelho quando o responsável toca em "Compartilhar" e escolhe o destino.
- Fotos no PDF ficam **desligadas por padrão**. O responsável liga se quiser.
- Todas as páginas trazem o aviso: "Registro feito pela família no Crescer Bem. Não substitui documentos médicos."

## 6. Conteúdo educativo e alertas

- Alertas nunca diagnosticam. Eles descrevem um sinal de atenção e orientam procurar um profissional.
- Texto padrão: "O Crescer Bem ajuda a organizar registros. Ele não substitui a avaliação de um profissional de saúde."
- O calendário vacinal sugerido é marcado como referência. A caderneta e o pediatra são a fonte oficial.

## 7. Riscos e mitigações

| Risco | Mitigação no MVP | Próximo passo |
| --- | --- | --- |
| Perda ou troca do aparelho apaga o histórico | Aviso em Ajustes; PDF como cópia | Backup local criptografado (P1) |
| Outra pessoa com o celular desbloqueado vê os dados | Dados só na área privada | Bloqueio do app por biometria (P2) |
| PDF compartilhado para destino errado | Compartilhar exige ação explícita; fotos desligadas por padrão | Revisar texto da tela de compartilhamento |
| Fotos com dados de terceiros (caderneta, receitas) | Ficam só no app, nunca na galeria | Orientar no texto de ajuda |
| Dado real em teste, log ou captura | Regras da seção 8 | Revisão no QA (tarefa 23) |
| Alerta interpretado como diagnóstico | Linguagem revisada e aviso fixo | QA de linguagem (tarefa 22) |

## 8. Regras para desenvolvimento e QA

1. Nunca usar dados reais de crianças em testes, exemplos, prints, issues ou documentação. Use nomes fictícios como "Bebê Teste".
2. Nunca registrar dados de saúde em `Log`, crash reports ou mensagens de erro.
3. Não adicionar bibliotecas de analytics, publicidade ou crash reporting sem nova revisão de privacidade.
4. Não adicionar a permissão de internet sem nova revisão de privacidade.
5. Arquivos temporários de foto descartados no formulário devem ser apagados.
6. Qualquer mudança nesta política exige registro no orquestrador e revisão do QA.
