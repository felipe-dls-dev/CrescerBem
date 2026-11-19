# Crescer Bem

Aplicativo Android para pais e responsáveis registrarem o dia a dia de saúde do recém-nascido: momentos, vacinas, consultas, peso e altura, sintomas, alimentação e fotos, com calendário e relatório em PDF.

O aplicativo organiza registros. Ele não diagnostica e não substitui o acompanhamento de um profissional de saúde.

## O que o app faz

| Tela | Conteúdo |
| --- | --- |
| Hoje | Idade do bebê, último peso e altura, próximos compromissos, registros do dia e resumo da alimentação |
| Calendário | Mês com marcadores coloridos por tipo; toque num dia para ver e adicionar registros |
| Bebê | Perfil, peso, altura e perímetro cefálico com gráfico, e momentos |
| Saúde | Vacinas agrupadas por idade, consultas próximas e histórico, sintomas com filtro |
| Alimentação | Botões rápidos, resumo do dia, aleitamento, fórmula, introdução alimentar e alimentos já experimentados |
| Relatório | Escolha de período e tipos, geração do PDF e compartilhamento |
| Ajustes | Bebês, apagar dados, privacidade e sobre |

Todo formulário tem "Tirar foto" e "Galeria". As fotos ficam só na área privada do app.

## Privacidade

- Dados e fotos ficam apenas no aparelho. Não há conta, servidor nem permissão de internet.
- O backup automático do Android está desligado para estes dados.
- O PDF só sai do celular quando o responsável toca em Compartilhar.

Detalhes em [docs/privacidade.md](docs/privacidade.md).

## Como compilar

Requisitos: JDK 17 ou mais novo e Android SDK com a plataforma 36.

1. Crie `local.properties` na raiz com o caminho do SDK, por exemplo `sdk.dir=C:/Android`.
2. Rode os comandos abaixo na raiz do projeto.

```
gradlew testDebugUnitTest   # testes de unidade e de banco (Robolectric)
gradlew assembleDebug       # gera app/build/outputs/apk/debug/app-debug.apk
```

Para instalar num celular com depuração USB ativa: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Documentação

- [Escopo do MVP e fluxos](docs/escopo-mvp.md)
- [Arquitetura](docs/arquitetura.md)
- [Privacidade](docs/privacidade.md)
- [Sistema visual](docs/design-system.md)
- [Colaboração entre agentes](docs/colaboracao-mcp.md)
