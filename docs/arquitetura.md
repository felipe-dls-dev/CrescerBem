# Arquitetura — Crescer Bem (Android)

Decisões da tarefa 16. Base para as tarefas de implementação (18 a 21, 25 a 28), para o sistema visual (17, Codex) e para o QA (22, 23, Antigravity).

## 1. Pilha técnica

| Item | Escolha | Motivo |
| --- | --- | --- |
| Linguagem | Kotlin 2.2.20 | Padrão do Android |
| Interface | Jetpack Compose + Material 3 (BOM 2025.10.01) | Telas declarativas, acessibilidade e tema dinâmico |
| Navegação | Navigation Compose 2.9.5 | Barra inferior + formulários como destinos |
| Banco | Room 2.8.1 com KSP | SQLite local, consultas por intervalo de datas, testes com Robolectric |
| Imagens | Coil 3 (sem módulo de rede) e ExifInterface | Miniaturas de arquivos locais e correção de orientação |
| PDF | `android.graphics.pdf.PdfDocument` | Nativo, offline, sem dependência externa |
| Build | AGP 8.13.0, Gradle 8.14.3, JDK 17+ | Compatível com o SDK instalado (plataforma 36) |
| SDK | minSdk 26, target/compile 36 | `java.time` nativo e Photo Picker com fallback |

O projeto não declara permissão de internet. Ver `docs/privacidade.md`.

## 2. Estrutura de pastas

```
app/src/main/java/br/com/crescerbem/
├── CrescerBemApp.kt / AppContainer.kt / MainActivity.kt
├── data/
│   ├── model/      entidades Room e enums (Baby, GrowthMeasurement, Vaccine, Appointment,
│   │               HealthEvent, Feeding, Moment, Photo)
│   ├── db/         AppDatabase, DAOs, conversores de data
│   └── repo/       repositórios (bebê, registros, linha do tempo, fotos, apagar tudo)
├── domain/         regras puras, sem Android: idade, calendário vacinal, sinais de atenção,
│                   resumo de alimentação, plausibilidade, grade do calendário,
│                   linha do tempo, montagem do relatório, rótulos e textos fixos
├── photos/         PhotoStorage: câmera via FileProvider, importação da galeria
├── report/         PdfReportWriter e ReportFiles (pasta, compartilhar, abrir)
└── ui/
    ├── theme/      cores, tipografia, formas (Codex, tarefa 17)
    ├── components/ seções recolhíveis, campos, fotos, gráfico, diálogos
    ├── navigation/ rotas, Scaffold com barra inferior, seletor de bebê
    └── screens/    today, calendar, baby, health, feeding, growth, vaccines,
                    appointments, moments, report, settings
```

Camadas: `ui` → `data/repo` → `data/db`. A pasta `domain` não depende de Android e é usada por repositórios, telas e relatório.

## 3. Modelo de dados

Todas as tabelas de registro têm `babyId` com chave estrangeira em cascata para `babies`.

| Tabela | Campos principais | Data usada no calendário |
| --- | --- | --- |
| babies | name, birthDate, birthTime, sex, bloodType, notes | birthDate |
| growth_measurements | date, type (peso/altura/perímetro), value | date |
| vaccines | name, dose, recommendedAgeMonths, scheduledDate, appliedDate, status, lot, place, reaction | appliedDate ou scheduledDate |
| appointments | title, dateTime, status, professional, place, reason, guidance | dateTime |
| health_events | dateTime, symptoms (lista), otherSymptom, intensity, temperatureCelsius, duration, actionsTaken, soughtCare | dateTime |
| feedings | dateTime, type, breastSide, durationMinutes, amountMl, food, texture, acceptance, firstTime, reaction | dateTime |
| moments | dateTime, title, description | dateTime |
| photos | ownerType + ownerId (vínculo polimórfico), fileName, addedAt | — |

Convenções:

- `LocalDate` é gravado como dia epoch; `LocalDateTime` como segundos epoch tratando a hora local como UTC. Assim um registro não muda de dia se o aparelho trocar de fuso.
- Status "pendente" e "atrasada" de vacina são calculados (`domain/VaccineRules.kt`), nunca gravados.
- Fotos: o arquivo fica em `files/photos`. O repositório apaga o arquivo junto com o registro. Ao apagar o bebê, os arquivos são removidos antes e as linhas somem pela cascata.
- O esquema é exportado em `app/schemas` para futuras migrações.

## 4. Fluxo de dados

- Repositórios expõem `Flow` do Room; ViewModels combinam com o bebê ativo (`flatMapLatest`) e entregam `StateFlow` de estado imutável.
- Injeção de dependências manual em `AppContainer`, entregue às telas por `LocalAppContainer`.
- Bebê ativo: só o id fica em SharedPreferences.
- Formulários usam `BaseFormViewModel` (carregar, salvar, excluir, fotos). Fotos novas ficam pendentes no `PhotoDraft` e só são vinculadas ao salvar. Se o formulário é abandonado, os arquivos novos são apagados.
- Relatório: `TimelineRepository.loadReportData` → `ReportBuilder.build` (puro, filtra de novo por bebê e período) → `PdfReportWriter`.

## 5. Navegação

Barra inferior com Hoje, Calendário, Bebê, Saúde e Alimentação. Relatório e Ajustes ficam no topo. O botão "Registrar" abre a lista de tipos. Formulários usam uma rota genérica `form/{tipo}?id=&date=&preset=`.

## 6. Estratégia de testes

| Nível | Ferramenta | O que cobre |
| --- | --- | --- |
| Unidade (JVM puro) | JUnit 4 | idade, calendário vacinal e status, sinais de atenção, resumo de alimentação, plausibilidade, grade do calendário, formatação, montagem do relatório, redimensionamento de imagem, conversores |
| Integração (JVM) | Robolectric + Room em memória | CRUD de cada tabela, consultas por intervalo, cascata ao apagar bebê, sincronização e exclusão de fotos, linha do tempo, apagar tudo, paginação do relatório em bitmaps |
| Instrumentado (aparelho) | Pendente | fluxos de tela, câmera e compartilhamento; depende de emulador ou aparelho |

Regra: nenhum dado real em teste. Use nomes como "Bebê Teste".

Comando:

```
gradlew testDebugUnitTest assembleDebug
```

## 7. Como rodar

1. Instale o Android Studio ou o SDK com a plataforma 36.
2. Crie `local.properties` com `sdk.dir=<caminho do SDK>` (o arquivo não é versionado).
3. `gradlew assembleDebug` gera `app/build/outputs/apk/debug/app-debug.apk`.

O caminho do projeto contém "ê". Duas configurações em `gradle.properties` tratam isso no Windows:

- `android.overridePathCheck=true` faz o AGP aceitar o caminho.
- `-Dfile.encoding=COMPAT` no daemon faz o Gradle gravar o classpath dos testes na codificação nativa. Sem isso, com JDK 18 ou mais novo, todos os testes falham com `ClassNotFoundException`.

Se o build falhar com "Could not move temporary workspace", basta repetir o comando: é um conflito intermitente do Gradle com o antivírus ao mover arquivos de cache.

## 7.1 PDF e testes

`PdfReportWriter` desenha as páginas por meio da interface `PageSink`. No app, o destino é o `PdfDocument` do Android. Nos testes, o destino são bitmaps, porque o `PdfDocument` nativo não existe no Robolectric. Assim a paginação, as tabelas e o rodapé são testados na JVM. A gravação do arquivo PDF em si só é verificável num aparelho ou emulador.

## 8. Fora desta versão

Lembretes locais (P1), curvas de crescimento da OMS (P1), backup criptografado (P1), bloqueio por biometria (P2) e qualquer sincronização em nuvem, que exige nova revisão de privacidade.
