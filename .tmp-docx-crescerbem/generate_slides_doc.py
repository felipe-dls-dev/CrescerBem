from __future__ import annotations

import shutil
from pathlib import Path

import docx
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Inches, Pt, RGBColor


def build_document() -> docx.Document:
    doc = docx.Document()

    for section in doc.sections:
        section.page_width = Inches(8.27)
        section.page_height = Inches(11.69)
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    style = doc.styles["Normal"]
    style.font.name = "Arial"
    style.font.size = Pt(11)
    style.font.color.rgb = RGBColor(0x22, 0x22, 0x22)

    def add_p(
        text: str = "",
        bold_prefix: str = "",
        space_after: float = 5,
        bold: bool = False,
        italic: bool = False,
        size: float = 11,
        color: tuple[int, int, int] = (0x22, 0x22, 0x22),
        align: WD_ALIGN_PARAGRAPH = WD_ALIGN_PARAGRAPH.LEFT,
    ):
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(space_after)
        p.paragraph_format.line_spacing = 1.15
        p.alignment = align
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.bold = True
            r_pre.font.name = "Arial"
            r_pre.font.size = Pt(size)
            r_pre.font.color.rgb = RGBColor(*color)
        if text:
            r_txt = p.add_run(text)
            r_txt.bold = bold
            r_txt.italic = italic
            r_txt.font.name = "Arial"
            r_txt.font.size = Pt(size)
            r_txt.font.color.rgb = RGBColor(*color)
        return p

    # Cabeçalho
    add_p("Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas", bold=True, size=13, color=(0x1B, 0x3A, 0x4B), space_after=2)
    add_p("Roteiro de Slides: Apresentação de Projeto de Aplicativo", bold=True, size=11.5, color=(0x2F, 0x6F, 0x6A), space_after=3)
    add_p("Este roteiro foi pensado para uma apresentação dinâmica e informativa, dividida em seções claras para guiar sua audiência.", italic=True, size=10, color=(0x55, 0x55, 0x55), space_after=12)

    # Slide 1
    add_p("Slide 1: Título da Apresentação", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p(" Crescer Bem: Inovação e Impacto Social", bold_prefix="Título:", space_after=2)
    add_p(" Um Projeto para o Acompanhamento Integral da Saúde do Recém-Nascido e Apoio às Famílias", bold_prefix="Subtítulo:", space_after=2)
    add_p("Felipe de Lima Souza", bold=True, space_after=2)
    add_p(" Projetos (Projeto Integrado à Sociedade I - Profª. Mônica Norris Ribeiro)", bold_prefix="Disciplina:", space_after=2)
    add_p("Novembro de 2026", space_after=10)

    # Slide 2
    add_p("Slide 2: Sumário Executivo / Visão Geral", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p(" Aplicativo Android nativo, seguro e 100% offline para acompanhamento do cotidiano de saúde do bebê, com calendário integrado e geração local de relatórios em PDF.", bold_prefix="O que é o Crescer Bem?", space_after=2)
    add_p(" Dispersão e perda de registros essenciais de saúde na rotina exaustiva dos primeiros meses do bebê, dificultando o controle familiar e as conversas com profissionais de saúde.", bold_prefix="Problema que Resolvemos:", space_after=2)
    add_p(" Organização prática do cuidado diário, alívio da sobrecarga mental dos pais, consultas pediátricas mais precisas com dados consolidados e segurança absoluta da privacidade infantil.", bold_prefix="Impacto Esperado:", space_after=10)

    # Slide 3
    add_p("Slide 3: O Tema Escolhido para o Aplicativo", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p(" Saúde Digital Infantil e Apoio ao Cuidado Parental na Primeira Infância", bold_prefix="Tema Principal:", space_after=3)
    add_p("Detalhes do Tema:", bold=True, space_after=2)
    add_p(" Os primeiros meses de vida são cruciais para o desenvolvimento saudável da criança e exigem acompanhamento rigoroso de vacinas, peso e sintomas em um período de grande cansaço físico e mental dos cuidadores.", bold_prefix="Por que este tema é relevante?", space_after=2)
    add_p(" Curvas e medidas de crescimento (peso, altura, perímetro cefálico), calendário do Programa Nacional de Imunizações (PNI/SUS), consultas agendadas e realizadas com orientações médicas, registro de sintomas e mal-estar com orientações educativas sem diagnóstico, alimentação (aleitamento materno, fórmula e introdução alimentar) e linha do tempo de momentos com fotos locais.", bold_prefix="Quais aspectos específicos do tema seu aplicativo aborda?", space_after=2)
    add_p(" Pais, mães e outros responsáveis por recém-nascidos e bebês, em especial quem precisa registrar informações pelo celular com rapidez, em poucos passos e com apenas uma mão.", bold_prefix="Qual o nicho ou público-alvo principal que se beneficia com este tema?", space_after=10)

    # Slide 4
    add_p("Slide 4: A Importância do Assunto", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("Relevância Social/Econômica/Ambiental:", bold=True, space_after=2)
    add_p(" A caderneta física de saúde da criança frequentemente sofre desgaste, esquecimento ou preenchimento incompleto; atrasos na imunização ou demora na identificação de alterações de crescimento afetam diretamente os índices de saúde pública infantil.", bold_prefix="Por que este assunto é crucial para a sociedade atualmente?", space_after=2)
    add_p(" A maioria das ferramentas comerciais exige internet constante, criação de contas com termos invasivos de privacidade ou exibe interfaces complexas com anúncios publicitários. O Crescer Bem preenche a lacuna de um aplicativo 100% offline, ágil, seguro e focado na privacidade por design.", bold_prefix="Quais lacunas ou desafios existem neste campo que seu aplicativo busca preencher/resolver?", space_after=2)
    add_p(" Gera estresse e sobrecarga nos responsáveis, risco de perda de prazos de vacinas essenciais, esquecimento de orientações médicas e consultas pediátricas menos resolutivas pela falta de um histórico organizado.", bold_prefix="Como a falta de uma solução para este problema afeta o público-alvo?", space_after=10)

    # Slide 5
    add_p("Slide 5: Objetivo do Projeto e do Aplicativo", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("Objetivo Geral:", bold=True, space_after=2)
    add_p("Desenvolver um aplicativo Android que permita aos responsáveis registrar e consultar, com clareza, ergonomia e privacidade absoluta, o acompanhamento cotidiano da saúde de bebês.", space_after=3)
    add_p("Objetivos Específicos (do Aplicativo):", bold=True, space_after=2)
    add_p("• Registrar dados de crescimento, vacinas, consultas, sintomas, alimentação, fotos e momentos em poucos toques (em até 3 toques a partir da tela inicial).", space_after=2)
    add_p("• Organizar os registros por bebê, calendário mensal interativo e histórico cronológico, com relatórios em PDF gerados localmente para o período escolhido.", space_after=2)
    add_p("• Manter todos os dados somente no aparelho, com funcionamento 100% offline, interface acessível em alto contraste (Material 3) e alertas com linguagem educativa que orientam procurar um profissional quando necessário.", space_after=10)

    # Slide 6
    add_p("Slide 6: Pesquisa e Referências: Nossos Materiais de Estudo", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("Fontes de Pesquisa:", bold=True, space_after=2)
    add_p("1. Caderneta da Criança: Passaporte da Cidadania — Ministério da Saúde do Brasil, 2022.", space_after=2)
    add_p("2. WHO Child Growth Standards — World Health Organization (OMS), 2006.", space_after=2)
    add_p("3. Guia Prático de Atualização: Aleitamento Materno e Alimentação Complementar — Sociedade Brasileira de Pediatria (SBP), 2021.", space_after=2)
    add_p("4. Material Design 3 e Diretrizes de Acessibilidade Android (WCAG 2.1) — Google Android Developers, 2024.", space_after=2)
    add_p("5. Lei Geral de Proteção de Dados Pessoais (LGPD - Lei nº 13.709/2018, Art. 14: Tratamento de dados pessoais de crianças e dados sensíveis de saúde) — Presidência da República, 2018.", space_after=10)

    # Slide 7
    add_p("Slide 7: Aprendizados Chave da Pesquisa", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("O que os Materiais Mais Importantes te Ensinaram?", bold=True, space_after=2)
    add_p("• A Caderneta da Criança (MS) e o PNI mostraram como estruturar o calendário vacinal por faixas etárias sugeridas (ao nascer, 2, 3, 4, 5, 6, 9, 12 e 15 meses), mantendo a caderneta física e o pediatra como referências oficiais.", space_after=2)
    add_p("• Os padrões da OMS e da SBP revelaram os parâmetros antropométricos e reforçaram que alertas sobre sintomas (como febre em bebês com menos de 3 meses) devem ser puramente educativos e direcionar para avaliação médica, nunca emitindo diagnósticos.", space_after=2)
    add_p("• O Material Design 3 e as diretrizes de acessibilidade ensinaram que o design para cuidadores de bebês precisa considerar o cansaço extremo e o uso noturno com uma só mão, direcionando para áreas de toque mínimas de 48dp, seções recolhíveis e paleta calma em verde-sálvia.", space_after=2)
    add_p("• A LGPD e as normas de segurança demonstraram a importância da coleta mínima e da privacidade por design, fundamentando a decisão de não utilizar contas, não exigir internet e não enviar nenhum dado para fora do aparelho.", space_after=10)

    # Slide 8
    add_p("Slide 8: Metodologia de Desenvolvimento: Como Elaboramos o Aplicativo (Etapas)", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p(" Metodologia ágil e iterativa inspirada em Design Thinking (empatia com a rotina dos pais) e arquitetura em camadas limpas (Clean Architecture / MVVM).", bold_prefix="Visão Geral do Processo:", space_after=3)
    add_p("Etapas de Elaboração:", bold=True, space_after=2)
    add_p(" Levantamento das necessidades de saúde infantil, priorização em P0 (MVP essencial), P1 (evoluções) e P2 (futuro), e elaboração de User Stories com regras de negócio.", bold_prefix="Definição de Requisitos e Escopo:", space_after=2)
    add_p(" Consulta às normas da SBP, calendário do Ministério da Saúde e análise das deficiências de soluções comerciais concorrentes.", bold_prefix="Pesquisa e Análise:", space_after=2)
    add_p(" Desenho da navegação inferior em 5 telas principais (Hoje, Calendário, Bebê, Saúde e Alimentação) com acesso rápido ao botão central \"+ Registrar\".", bold_prefix="Prototipagem e Wireframing:", space_after=2)
    add_p(" Criação de um sistema visual acolhedor e limpo com Jetpack Compose e Material 3, botões grandes e seções expansíveis tipo accordion.", bold_prefix="Design de Interface (UI) e Experiência do Usuário (UX):", space_after=2)
    add_p(" Programação em Kotlin 2.2, Android SDK (API 36), banco de dados local Room com SQLite, Flow reativo, Coil para imagens locais e Canvas/PdfDocument nativo para relatórios.", bold_prefix="Desenvolvimento/Implementação:", space_after=2)
    add_p(" Testes automatizados de unidade com JUnit 4 (regras de vacinas, cálculo de idade, plausibilidade) e testes de integração com Robolectric (Room em memória, cascata e exclusão de fotos).", bold_prefix="Testes e Validação:", space_after=2)
    add_p(" Revisão textual cuidadosa para evitar termos médicos prescritivos, validação de contrastes WCAG e compilação do APK de distribuição.", bold_prefix="Iteração e Refinamento:", space_after=10)

    # Slide 9
    add_p("Slide 9: Contribuição para a Comunidade: Alinhamento com os ODS", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("Qual ODS (Objetivo de Desenvolvimento Sustentável) seu aplicativo apoia?", bold=True, space_after=2)
    add_p("• ODS principal: ODS 3 — Saúde e Bem-Estar (Meta 3.2: Redução da mortalidade neonatal e infantil).", space_after=2)
    add_p("• ODS secundário: ODS 10 — Redução das Desigualdades (Meta 10.2: Acesso inclusivo à tecnologia e à informação).", space_after=3)
    add_p("Como seu aplicativo contribui para este ODS?", bold=True, space_after=2)
    add_p(" Apoia famílias e cuidadores na prevenção primária em saúde, facilitando a adesão ao calendário vacinal, o controle rigoroso do ganho de peso e estatura, o incentivo ao aleitamento materno e a identificação precoce de sintomas que exigem atenção médica imediata.", bold_prefix="Contribuição para o ODS 3:", space_after=2)
    add_p(" O aplicativo é totalmente gratuito, sem anúncios comerciais, não consome franquia de dados móveis (100% offline) e opera com alta performance mesmo em smartphones mais simples (desde o Android 8.0), democratizando o acesso a uma ferramenta de apoio à saúde infantil sem distinção de renda.", bold_prefix="Contribuição para o ODS 10:", space_after=10)

    # Slide 10
    add_p("Slide 10: Próximos Passos e Chamada para Ação", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p("Próximos Passos do Projeto:", bold=True, space_after=2)
    add_p("• Desenvolvimento da versão P1: inclusão de gráficos visuais de curvas de crescimento (padrão OMS), notificações/lembretes locais de consultas e vacinas e sistema de backup local em arquivo criptografado.", space_after=2)
    add_p("• Testes com público ampliado: aplicação de testes de usabilidade com grupos de mães lactantes e profissionais da atenção básica de saúde.", space_after=2)
    add_p("• Evolução P2: módulo de controle de rotina de sono e fraldas, e autenticação biométrica local.", space_after=3)
    add_p("Visão de Futuro:", bold=True, space_after=2)
    add_p("Consolidar o Crescer Bem como uma ferramenta de referência em saúde assistiva digital e software público comunitário, provando que é viável criar tecnologia móvel de alto impacto com respeito estrito à privacidade.", space_after=3)
    add_p("Agradecimento e Contato:", bold=True, space_after=2)
    add_p('"Obrigado!"', italic=True, space_after=2)
    add_p("Felipe de Lima Souza — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas", space_after=2)
    add_p("Contato: felipe.lima@aluno.unifoa.edu.br", space_after=10)

    # Dicas
    add_p("Dicas para a Apresentação:", bold=True, size=11.5, color=(0x1B, 0x3A, 0x4B), space_after=3)
    add_p(" Mantenha os slides limpos, com pouco texto e muitas imagens ou ícones relevantes (utilizar capturas reais das telas de Hoje, Calendário e do PDF gerado).", bold_prefix="Visual:", space_after=2)
    add_p(" Enfatizar que o app foi planejado para momentos de exaustão dos pais e que a privacidade e o caráter educativo são os maiores diferenciais do projeto.", bold_prefix="Ponto-chave:", space_after=2)
    add_p(" Cronometre sua apresentação para garantir que você aborde todos os pontos no tempo disponível (cerca de 10 a 12 minutos no total).", bold_prefix="Tempo:", space_after=2)
    add_p(" Demonstre entusiasmo pelo seu projeto e pelo impacto positivo no cuidado das famílias!", bold_prefix="Paixão:", space_after=3)
    add_p("Boa sorte com a sua apresentação! Tenho certeza de que será um sucesso!", bold=True, italic=True, color=(0x2F, 0x6F, 0x6A), space_after=10)

    return doc


def main() -> None:
    doc = build_document()
    out_root = Path("Crescer Bem - Roteiro de Slides preenchido.docx")
    out_foa = Path("Foa/Crescer Bem - Roteiro de Slides preenchido.docx")

    doc.save(out_root)
    doc.save(out_foa)

    downloads_d = Path("D:/Downloads D")
    if downloads_d.exists():
        shutil.copy(out_root, downloads_d / out_root.name)

    print(f"Gerado com sucesso em: {out_root.resolve()}")
    print(f"Gerado com sucesso em: {out_foa.resolve()}")


if __name__ == "__main__":
    main()
