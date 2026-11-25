from __future__ import annotations

import shutil
from pathlib import Path

import pptx
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Inches, Pt

# ==============================================================================
# PALETA DE CORES E IDENTIDADE VISUAL (CRESCER BEM)
# ==============================================================================
BG_PAGE = RGBColor(0xF6, 0xF8, 0xF7)          # Fundo sutilmente esverdeado/acolhedor
COLOR_PRIMARY = RGBColor(0x23, 0x56, 0x51)    # Verde-sálvia escuro principal (#235651)
COLOR_PRIMARY_LIGHT = RGBColor(0xE6, 0xF1, 0xEF) # Fundo de tags/pílulas primárias
COLOR_ACCENT = RGBColor(0xBF, 0x51, 0x3B)     # Pêssego/Terracota de destaque (#BF513B)
COLOR_ACCENT_LIGHT = RGBColor(0xFD, 0xF0, 0xED) # Fundo de destaque suave
COLOR_PURPLE = RGBColor(0x56, 0x48, 0x7E)     # Lavanda/Roxo escuro de apoio (#56487E)
COLOR_PURPLE_LIGHT = RGBColor(0xF0, 0xED, 0xF8) # Fundo lavanda suave

COLOR_CARD_BG = RGBColor(0xFF, 0xFF, 0xFF)    # Branco puro nos cartões
COLOR_CARD_BORDER = RGBColor(0xDC, 0xE7, 0xE4)# Borda sutil nos cartões
COLOR_CARD_BORDER_ACCENT = RGBColor(0xE8, 0xC4, 0xBC) # Borda suave terracota

TEXT_TITLE = RGBColor(0x15, 0x33, 0x30)       # Títulos escuros profundos
TEXT_MAIN = RGBColor(0x23, 0x2A, 0x31)        # Texto principal (Dark Slate)
TEXT_MUTED = RGBColor(0x5D, 0x6B, 0x75)       # Texto secundário (Muted Slate)
TEXT_WHITE = RGBColor(0xFF, 0xFF, 0xFF)       # Texto branco

FONT_HEADING = "Arial"
FONT_BODY = "Arial"

SLIDE_WIDTH = Inches(13.333)
SLIDE_HEIGHT = Inches(7.5)


def add_run(p, text: str, bold: bool = False, italic: bool = False, size: float | None = None, color: RGBColor | None = None):
    r = p.add_run()
    r.text = text
    r.font.name = FONT_BODY
    if bold:
        r.font.bold = True
    if italic:
        r.font.italic = True
    if size is not None:
        r.font.size = Pt(size)
    if color is not None:
        r.font.color.rgb = color
    return r


def add_rounded_card(
    slide,
    left: float,
    top: float,
    width: float,
    height: float,
    bg_color: RGBColor = COLOR_CARD_BG,
    border_color: RGBColor | None = COLOR_CARD_BORDER,
    border_width: float = 1.0,
):
    shape = slide.shapes.add_shape(
        MSO_SHAPE.ROUNDED_RECTANGLE,
        Inches(left),
        Inches(top),
        Inches(width),
        Inches(height),
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = bg_color
    if border_color:
        shape.line.color.rgb = border_color
        shape.line.width = Pt(border_width)
    else:
        shape.line.fill.background()
    return shape


def add_header(
    slide,
    category_eyebrow: str,
    title_text: str,
    slide_index: int,
    total_slides: int = 10,
):
    # Eyebrow / Categoria
    eyebrow_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.45), Inches(9.5), Inches(0.35))
    tf_eye = eyebrow_box.text_frame
    tf_eye.word_wrap = True
    tf_eye.margin_left = tf_eye.margin_top = tf_eye.margin_right = tf_eye.margin_bottom = 0
    p_eye = tf_eye.paragraphs[0]
    p_eye.text = category_eyebrow.upper()
    p_eye.font.name = FONT_HEADING
    p_eye.font.size = Pt(10)
    p_eye.font.bold = True
    p_eye.font.color.rgb = COLOR_ACCENT

    # Título Principal
    title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.78), Inches(10.2), Inches(0.85))
    tf_title = title_box.text_frame
    tf_title.word_wrap = True
    tf_title.margin_left = tf_title.margin_top = tf_title.margin_right = tf_title.margin_bottom = 0
    p_title = tf_title.paragraphs[0]
    p_title.text = title_text
    p_title.font.name = FONT_HEADING
    p_title.font.size = Pt(21)
    p_title.font.bold = True
    p_title.font.color.rgb = TEXT_TITLE

    # Indicador de Número do Slide no topo direito
    num_box = slide.shapes.add_textbox(Inches(11.2), Inches(0.55), Inches(1.33), Inches(0.5))
    tf_num = num_box.text_frame
    tf_num.margin_left = tf_num.margin_top = tf_num.margin_right = tf_num.margin_bottom = 0
    p_num = tf_num.paragraphs[0]
    p_num.alignment = PP_ALIGN.RIGHT
    p_num.text = f"{slide_index:02d} / {total_slides:02d}"
    p_num.font.name = FONT_HEADING
    p_num.font.size = Pt(11)
    p_num.font.bold = True
    p_num.font.color.rgb = COLOR_PRIMARY


def set_notes(slide, notes_text: str):
    notes_slide = slide.notes_slide
    text_frame = notes_slide.notes_text_frame
    text_frame.text = notes_text.strip()


def build_presentation() -> pptx.Presentation:
    prs = pptx.Presentation()
    prs.slide_width = SLIDE_WIDTH
    prs.slide_height = SLIDE_HEIGHT
    blank_layout = prs.slide_layouts[6]

    # ==========================================================================
    # SLIDE 1: CAPA
    # ==========================================================================
    s1 = prs.slides.add_slide(blank_layout)
    # Fundo decorativo verde à esquerda/topo
    add_rounded_card(s1, 0.8, 0.8, 11.733, 5.9, bg_color=COLOR_PRIMARY, border_color=None)
    
    # Cartão interno claro com efeito de contraste
    add_rounded_card(s1, 1.2, 1.2, 10.933, 5.1, bg_color=COLOR_CARD_BG, border_color=COLOR_CARD_BORDER)

    # Tag de identificação do curso
    tag_shape = add_rounded_card(s1, 1.7, 1.6, 6.8, 0.4, bg_color=COLOR_PRIMARY_LIGHT, border_color=None)
    tf_tag = tag_shape.text_frame
    tf_tag.vertical_anchor = MSO_ANCHOR.MIDDLE
    p_tag = tf_tag.paragraphs[0]
    p_tag.alignment = PP_ALIGN.CENTER
    p_tag.text = "CURSO SUPERIOR DE TECNOLOGIA EM ANÁLISE E DESENVOLVIMENTO DE SISTEMAS"
    p_tag.font.name = FONT_HEADING
    p_tag.font.size = Pt(9.5)
    p_tag.font.bold = True
    p_tag.font.color.rgb = COLOR_PRIMARY

    # Título do App
    t_box = s1.shapes.add_textbox(Inches(1.7), Inches(2.2), Inches(9.8), Inches(1.1))
    tf_t = t_box.text_frame
    tf_t.word_wrap = True
    p_t = tf_t.paragraphs[0]
    p_t.text = "Crescer Bem"
    p_t.font.name = FONT_HEADING
    p_t.font.size = Pt(44)
    p_t.font.bold = True
    p_t.font.color.rgb = COLOR_PRIMARY

    # Subtítulo
    p_sub = tf_t.add_paragraph()
    p_sub.text = "Inovação, Acessibilidade e Impacto Social na Primeira Infância"
    p_sub.font.name = FONT_HEADING
    p_sub.font.size = Pt(17)
    p_sub.font.bold = True
    p_sub.font.color.rgb = COLOR_ACCENT
    p_sub.space_before = Pt(4)

    # Descrição do projeto
    d_box = s1.shapes.add_textbox(Inches(1.7), Inches(3.55), Inches(9.8), Inches(0.9))
    tf_d = d_box.text_frame
    tf_d.word_wrap = True
    p_d = tf_d.paragraphs[0]
    p_d.text = (
        "Um aplicativo móvel Android nativo, 100% offline e privativo, desenvolvido para apoiar pais e responsáveis "
        "no registro de marcos de crescimento, calendário vacinal, consultas pediátricas, sintomas e alimentação do bebê."
    )
    p_d.font.name = FONT_BODY
    p_d.font.size = Pt(13)
    p_d.font.color.rgb = TEXT_MAIN

    # Rodapé institucional do Slide 1 (Metadados em 3 colunas)
    meta_box = s1.shapes.add_textbox(Inches(1.7), Inches(4.7), Inches(9.8), Inches(1.2))
    tf_m = meta_box.text_frame
    tf_m.word_wrap = True
    p_m1 = tf_m.paragraphs[0]
    p_m1.font.size = Pt(11)
    
    add_run(p_m1, "Autor: ", bold=True, color=COLOR_PRIMARY)
    add_run(p_m1, "Felipe de Lima Souza   |   ")
    add_run(p_m1, "Disciplina: ", bold=True, color=COLOR_PRIMARY)
    add_run(p_m1, "Projetos (Projeto Integrado à Sociedade I)\n")
    
    add_run(p_m1, "Orientação: ", bold=True, color=COLOR_PRIMARY)
    add_run(p_m1, "Profª. Mônica Norris Ribeiro   |   ")
    add_run(p_m1, "Data: ", bold=True, color=COLOR_PRIMARY)
    add_run(p_m1, "Novembro de 2026")

    set_notes(
        s1,
        "Bom dia/boa tarde a todos. Sou o Felipe de Lima Souza, aluno do curso superior de Tecnologia em Análise e "
        "Desenvolvimento de Sistemas. Hoje apresento o projeto 'Crescer Bem: Inovação e Impacto Social', desenvolvido no âmbito "
        "da disciplina Projeto Integrado à Sociedade I, sob a valorosa orientação da Professora Mônica Norris Ribeiro.\n\n"
        "O Crescer Bem nasceu da união entre tecnologia móvel moderna, engenharia de software rigorosa e empatia humana profunda "
        "com a rotina das famílias na fase mais crítica da vida: a primeira infância.",
    )

    # ==========================================================================
    # SLIDE 2: SUMÁRIO EXECUTIVO / VISÃO GERAL
    # ==========================================================================
    s2 = prs.slides.add_slide(blank_layout)
    add_header(s2, "Slide 02 | Sumário Executivo", "Visão Geral: O que é o Crescer Bem e Qual Problema Resolvemos", 2)

    cards_s2 = [
        (
            "O QUE É O APLICATIVO",
            COLOR_PRIMARY,
            COLOR_PRIMARY_LIGHT,
            [
                ("Natureza do App: ", "Software Android nativo moderno desenvolvido em Kotlin e Jetpack Compose."),
                ("Operação 100% Offline: ", "Funciona sem internet, sem necessidade de login, conta ou servidores em nuvem."),
                ("Escopo Abrangente: ", "Reúne registros de crescimento físico, vacinas do PNI, consultas, sintomas e mamadas."),
                ("Exportação Local: ", "Gera relatórios consolidados em formato PDF para levar às consultas com o pediatra."),
            ],
        ),
        (
            "PROBLEMA QUE RESOLVEMOS",
            COLOR_ACCENT,
            COLOR_ACCENT_LIGHT,
            [
                ("Sobrecarga Parental: ", "Exaustão física e mental extrema dos pais nos primeiros meses de vida da criança."),
                ("Informações Dispersas: ", "Dados críticos perdidos entre anotações em papel, caderneta física, fotos e memória falha."),
                ("Consultas Prejudicadas: ", "Dificuldade em relatar com precisão histórico clínico, reações e episódios de febre ao médico."),
                ("Risco de Esquecimento: ", "Atrasos involuntários em doses de vacinação por falta de visualização clara de prazos."),
            ],
        ),
        (
            "IMPACTO ESPERADO",
            COLOR_PURPLE,
            COLOR_PURPLE_LIGHT,
            [
                ("Organização Prática: ", "Centralização do histórico de saúde em uma interface rápida, operável em até 3 toques."),
                ("Consultas Pediátricas Resolutivas: ", "Entrega de relatórios claros em PDF que facilitam a conduta do pediatra."),
                ("Privacidade Absoluta: ", "Arquitetura Zero-Leakage: dados e fotos não saem do aparelho do responsável."),
                ("Inclusão Tecnológica: ", "Disponível gratuitamente, sem anúncios e operando sem consumir pacote de dados."),
            ],
        ),
    ]

    left_base = 0.8
    card_w = 3.68
    for i, (title, color_h, color_bg_tag, bullets) in enumerate(cards_s2):
        pos_left = left_base + i * 4.02
        add_rounded_card(s2, pos_left, 1.8, card_w, 5.15)
        
        # Tag cabeçalho
        tag = add_rounded_card(s2, pos_left + 0.3, 2.05, card_w - 0.6, 0.42, bg_color=color_bg_tag, border_color=None)
        p_t = tag.text_frame.paragraphs[0]
        p_t.text = title
        p_t.alignment = PP_ALIGN.CENTER
        p_t.font.name = FONT_HEADING
        p_t.font.bold = True
        p_t.font.size = Pt(10.5)
        p_t.font.color.rgb = color_h

        # Bullet points
        tx_box = s2.shapes.add_textbox(Inches(pos_left + 0.25), Inches(2.7), Inches(card_w - 0.5), Inches(4.0))
        tf = tx_box.text_frame
        tf.word_wrap = True
        for b_idx, (b_title, b_desc) in enumerate(bullets):
            p = tf.paragraphs[0] if b_idx == 0 else tf.add_paragraph()
            p.space_after = Pt(12)
            p.font.name = FONT_BODY
            p.font.size = Pt(11)
            
            add_run(p, "• " + b_title, bold=True, color=color_h)
            add_run(p, b_desc, color=TEXT_MAIN)

    set_notes(
        s2,
        "Neste sumário executivo, apresentamos a síntese do nosso projeto em três pilares.\n"
        "Primeiro: o Crescer Bem é um aplicativo Android nativo construído com as tecnologias mais modernas do Google, focado em facilidade de uso e sem dependência de internet.\n"
        "Segundo: o problema que resolvemos é a exaustão dos cuidadores. Ter um bebê é maravilhoso, mas é também um período de privação de sono. As informações médicas acabam se perdendo em papéis soltos e mensagens de celular.\n"
        "Terceiro: nosso impacto é oferecer paz de espírito aos responsáveis, permitindo registrar tudo em poucos segundos e levar um relatório limpo em PDF para a consulta pediátrica, com total garantia de que nenhum dado privado será exposto.",
    )

    # ==========================================================================
    # SLIDE 3: O TEMA ESCOLHIDO PARA O APLICATIVO
    # ==========================================================================
    s3 = prs.slides.add_slide(blank_layout)
    add_header(s3, "Slide 03 | O Tema Escolhido", "Saúde Digital Infantil e Apoio ao Cuidado Parental na Primeira Infância", 3)

    # Cartão de Destaque Superior: Definição do Tema
    top_c = add_rounded_card(s3, 0.8, 1.8, 11.733, 1.25, bg_color=COLOR_PRIMARY_LIGHT, border_color=COLOR_PRIMARY)
    tf_top = top_c.text_frame
    tf_top.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf_top.margin_left = Inches(0.4)
    tf_top.margin_right = Inches(0.4)
    p_top_h = tf_top.paragraphs[0]
    p_top_h.text = "TEMA PRINCIPAL: SAÚDE DIGITAL INFANTIL & APOIO PARENTAL (mHealth)"
    p_top_h.font.bold = True
    p_top_h.font.size = Pt(12)
    p_top_h.font.color.rgb = COLOR_PRIMARY
    p_top_d = tf_top.add_paragraph()
    p_top_d.text = (
        "Aplicação de tecnologia móvel assistiva para suporte direto a mães, pais e cuidadores durante os primeiros mil dias de vida "
        "do bebê, integrando vigilância antropométrica, imunológica e nutricional à rotina familiar diária."
    )
    p_top_d.font.size = Pt(11.5)
    p_top_d.font.color.rgb = TEXT_MAIN
    p_top_d.space_before = Pt(3)

    # Três Colunas Detalhadas Abaixo
    cards_s3 = [
        (
            "Por que o tema é relevante?",
            COLOR_ACCENT,
            [
                ("Janela dos Primeiros 1.000 Dias: ", "Fase mais determinante para o desenvolvimento neurológico, imunológico e físico da criança."),
                ("Prevenção Primária: ", "Identificação ágil de anomalias de ganho ponderal, desnutrição ou infecções precoces."),
                ("Cuidado Centrado na Família: ", "Atenção ao esgotamento físico de quem cuida do bebê no puerpério e nos primeiros meses."),
            ],
        ),
        (
            "Aspectos Específicos do App",
            COLOR_PRIMARY,
            [
                ("Vigilância de Crescimento: ", "Série histórica de peso, altura e perímetro cefálico com cálculo de plausibilidade."),
                ("Calendário de Imunização: ", "Acompanhamento do Programa Nacional de Imunizações (PNI/SUS) com status claro."),
                ("Alimentação & Sintomas: ", "Registro de mamadas, fórmulas e introdução alimentar, além de sintomas clínicos com alertas educativos."),
            ],
        ),
        (
            "Público-Alvo e Ergonomia",
            COLOR_PURPLE,
            [
                ("Perfil dos Usuários: ", "Mães, pais, familiares e cuidadores legais de recém-nascidos e lactentes."),
                ("Cenário de Uso Real: ", "Operação com uma só mão à noite, no escuro, enquanto o bebê é segurado ou amamentado."),
                ("Diretrizes Ergonômicas: ", "Áreas táteis amplas (mínimo 48dp), paleta verde-sálvia suave e leitura sem esforço."),
            ],
        ),
    ]

    for i, (col_title, col_color, col_bullets) in enumerate(cards_s3):
        pos_left = 0.8 + i * 4.02
        add_rounded_card(s3, pos_left, 3.25, 3.68, 3.7)
        
        # Barra lateral colorida de identificação
        bar = add_rounded_card(s3, pos_left + 0.25, 3.45, 3.18, 0.35, bg_color=col_color, border_color=None)
        p_bar = bar.text_frame.paragraphs[0]
        p_bar.text = col_title
        p_bar.alignment = PP_ALIGN.CENTER
        p_bar.font.name = FONT_HEADING
        p_bar.font.bold = True
        p_bar.font.size = Pt(10.5)
        p_bar.font.color.rgb = TEXT_WHITE

        # Bullets
        tx = s3.shapes.add_textbox(Inches(pos_left + 0.25), Inches(4.0), Inches(3.18), Inches(2.8))
        tf = tx.text_frame
        tf.word_wrap = True
        for b_idx, (b_t, b_d) in enumerate(col_bullets):
            p = tf.paragraphs[0] if b_idx == 0 else tf.add_paragraph()
            p.space_after = Pt(10)
            p.font.name = FONT_BODY
            p.font.size = Pt(10.5)
            add_run(p, "• " + b_t, bold=True, color=col_color)
            add_run(p, b_d, color=TEXT_MAIN)

    set_notes(
        s3,
        "No Slide 3, aprofundamos o tema do nosso trabalho: Saúde Digital Infantil voltada ao suporte parental.\n"
        "A literatura científica em pediatria enfatiza que os primeiros mil dias de vida — da concepção até os dois anos — definem grande parte da trajetória biológica do ser humano.\n"
        "O Crescer Bem cobre os aspectos essenciais desse período: peso, altura, vacinas, consultas e introdução alimentar.\n"
        "Mas o diferencial chave da nossa engenharia de requisitos é a ergonomia: pensamos na mãe que está amamentando às 3 da manhã, cansada, e que precisa registrar a mamada usando apenas o polegar. Cada detalhe visual foi feito para reduzir a fricção e o esforço do usuário.",
    )

    # ==========================================================================
    # SLIDE 4: A IMPORTÂNCIA DO ASSUNTO
    # ==========================================================================
    s4 = prs.slides.add_slide(blank_layout)
    add_header(s4, "Slide 04 | A Importância do Assunto", "Relevância Social, Desafios do Mercado e Consequências Práticas", 4)

    cards_s4 = [
        (
            "RELEVÂNCIA SOCIAL & SAÚDE PÚBLICA",
            COLOR_PRIMARY,
            [
                ("Caderneta do SUS Sob Risco: ", "A caderneta física em papel é o documento oficial, porém é suscetível a extravio, desgaste físico ou esquecimento em casa no dia da consulta."),
                ("Prevenção de Óbitos Infantis: ", "Segundo a OMS e o Ministério da Saúde, a vacinação oportuna e a vigilância de peso/estatura são as principais barreiras contra a morbimortalidade na primeira infância."),
                ("Comunicação Médico-Família: ", "Consultas pediátricas são curtas; dados tabulados e organizados agilizam a tomada de decisão clínica do profissional."),
            ],
        ),
        (
            "LACUNAS DO MERCADO ATUAL",
            COLOR_ACCENT,
            [
                ("Monetização de Dados Sensíveis: ", "A maioria dos aplicativos populares exige cadastro e vende perfis de consumo para anunciantes, violando a privacidade da criança."),
                ("Dependência Excessiva de Internet: ", "Apps concorrentes armazenam dados em nuvem, tornando-se inacessíveis em unidades de saúde ou locais sem sinal de celular."),
                ("Interfaces Tóxicas e Alarmistas: ", "Muitas ferramentas tentam emitir 'diagnósticos automatizados' perigosos, gerando pânico infundado nos responsáveis."),
            ],
        ),
        (
            "CONSEQUÊNCIAS DA FALTA DE SOLUÇÃO",
            COLOR_PURPLE,
            [
                ("Atrasos no Calendário Vacinal: ", "Esquecimento de datas de reforço de vacinas essenciais (Penta, Meningo C, Pneumo), expondo o bebê a enfermidades evitáveis."),
                ("Consultas Pediátricas Imprecisas: ", "Omissão involuntária de sintomas, horários de febre ou reações alérgicas por falta de registros estruturados."),
                ("Exaustão e Ansiedade Parental: ", "Aumento do estresse e sobrecarga cognitiva em um momento em que a família mais necessita de acolhimento e clareza."),
            ],
        ),
    ]

    for i, (title, color_h, bullets) in enumerate(cards_s4):
        pos_left = 0.8 + i * 4.02
        add_rounded_card(s4, pos_left, 1.8, 3.68, 5.15)
        
        # Cabeçalho do Card
        tag = add_rounded_card(s4, pos_left + 0.25, 2.05, 3.18, 0.5, bg_color=color_h, border_color=None)
        p_t = tag.text_frame.paragraphs[0]
        p_t.text = title
        p_t.alignment = PP_ALIGN.CENTER
        p_t.font.name = FONT_HEADING
        p_t.font.bold = True
        p_t.font.size = Pt(10)
        p_t.font.color.rgb = TEXT_WHITE

        # Bullets
        tx = s4.shapes.add_textbox(Inches(pos_left + 0.25), Inches(2.75), Inches(3.18), Inches(4.0))
        tf = tx.text_frame
        tf.word_wrap = True
        for b_idx, (b_t, b_d) in enumerate(bullets):
            p = tf.paragraphs[0] if b_idx == 0 else tf.add_paragraph()
            p.space_after = Pt(14)
            p.font.name = FONT_BODY
            p.font.size = Pt(10.5)
            add_run(p, "• " + b_t, bold=True, color=color_h)
            add_run(p, b_d, color=TEXT_MAIN)

    set_notes(
        s4,
        "Por que este tema é tão crucial hoje?\n"
        "Primeiro, porque o acompanhamento de saúde da criança é um direito e uma necessidade de saúde pública. Quando um bebê deixa de tomar uma vacina ou quando a perda de peso não é notada a tempo, as consequências podem ser graves.\n"
        "Segundo, porque existe um vazio no mercado: os apps comerciais tratam os dados das crianças como mercadoria para anunciantes e enchem a tela de propagandas que confundem mães inexperientes.\n"
        "O Crescer Bem combate isso frontalmente: é uma ferramenta limpa, sem anúncios, sem rastreamento e que não tenta adivinhar doenças, mantendo sempre o pediatra e a caderneta física do SUS como referências soberanas.",
    )

    # ==========================================================================
    # SLIDE 5: OBJETIVOS DO PROJETO E DO APLICATIVO
    # ==========================================================================
    s5 = prs.slides.add_slide(blank_layout)
    add_header(s5, "Slide 05 | Objetivos do Projeto", "Propósito Geral e Objetivos Específicos do Crescer Bem", 5)

    # Card Grande do Objetivo Geral
    obj_g = add_rounded_card(s5, 0.8, 1.8, 11.733, 1.45, bg_color=COLOR_PRIMARY, border_color=None)
    tf_g = obj_g.text_frame
    tf_g.margin_left = Inches(0.4)
    tf_g.margin_right = Inches(0.4)
    tf_g.vertical_anchor = MSO_ANCHOR.MIDDLE
    p_gh = tf_g.paragraphs[0]
    p_gh.text = "OBJETIVO GERAL DO PROJETO"
    p_gh.font.bold = True
    p_gh.font.size = Pt(12)
    p_gh.font.color.rgb = COLOR_ACCENT_LIGHT
    p_gd = tf_g.add_paragraph()
    p_gd.text = (
        "Desenvolver um aplicativo Android nativo que capacite pais e responsáveis a registrar, consultar e organizar, "
        "com agilidade ergonômica, clareza visual e garantia irrestrita de privacidade, o histórico cotidiano de saúde do bebê."
    )
    p_gd.font.size = Pt(12.5)
    p_gd.font.bold = True
    p_gd.font.color.rgb = TEXT_WHITE
    p_gd.space_before = Pt(4)

    # Três Objetivos Específicos
    spec_cards = [
        (
            "OBJETIVO ESPECÍFICO 1",
            "Registro Ágil em até 3 Toques",
            COLOR_ACCENT,
            COLOR_ACCENT_LIGHT,
            "Permitir o cadastramento rápido e descomplicado de medidas antropométricas (peso/altura/perímetro), "
            "doses de vacinas do PNI, consultas agendadas, sintomas clínicos e mamadas diárias diretamente a partir da tela inicial.",
        ),
        (
            "OBJETIVO ESPECÍFICO 2",
            "Organização e Relatórios em PDF",
            COLOR_PRIMARY,
            COLOR_PRIMARY_LIGHT,
            "Estruturar a visualização temporal dos dados através de painel diário ('Hoje'), calendário mensal interativo "
            "com marcadores coloridos por categoria e geração local de relatórios em PDF para consultas pediátricas.",
        ),
        (
            "OBJETIVO ESPECÍFICO 3",
            "Privacidade por Design e Acessibilidade",
            COLOR_PURPLE,
            COLOR_PURPLE_LIGHT,
            "Assegurar funcionamento 100% offline sem conta ou internet, com dados e fotos restritos ao armazenamento privado "
            "do app (conforme LGPD Art. 14), interface em alto contraste (Material 3) e alertas com linguagem puramente educativa.",
        ),
    ]

    for i, (tag_txt, tit_txt, col_h, col_bg, desc_txt) in enumerate(spec_cards):
        pos_left = 0.8 + i * 4.02
        add_rounded_card(s5, pos_left, 3.45, 3.68, 3.5)
        
        # Badge
        badge = add_rounded_card(s5, pos_left + 0.25, 3.65, 3.18, 0.35, bg_color=col_bg, border_color=None)
        p_b = badge.text_frame.paragraphs[0]
        p_b.text = tag_txt
        p_b.alignment = PP_ALIGN.CENTER
        p_b.font.bold = True
        p_b.font.size = Pt(9.5)
        p_b.font.color.rgb = col_h

        # Título
        t_box = s5.shapes.add_textbox(Inches(pos_left + 0.25), Inches(4.1), Inches(3.18), Inches(0.65))
        tf_t = t_box.text_frame
        tf_t.word_wrap = True
        p_ti = tf_t.paragraphs[0]
        p_ti.text = tit_txt
        p_ti.font.bold = True
        p_ti.font.size = Pt(12.5)
        p_ti.font.color.rgb = TEXT_TITLE

        # Descrição
        d_box = s5.shapes.add_textbox(Inches(pos_left + 0.25), Inches(4.8), Inches(3.18), Inches(2.0))
        tf_d = d_box.text_frame
        tf_d.word_wrap = True
        p_de = tf_d.paragraphs[0]
        p_de.text = desc_txt
        p_de.font.size = Pt(11)
        p_de.font.color.rgb = TEXT_MAIN

    set_notes(
        s5,
        "Neste slide, delimitamos a meta central da nossa engenharia de software.\n"
        "O Objetivo Geral é claro: construir uma aplicação móvel que una alta performance técnica com impacto social e respeito absoluto aos dados da criança.\n"
        "Para atingir esse propósito, traçamos três objetivos específicos mensuráveis:\n"
        "1. Registro em até 3 toques, porque facilidade de uso em celular é requisito de sobrevivência para pais com recém-nascidos.\n"
        "2. Visualização integrada por calendário e geração de relatórios em PDF para apoiar as consultas pediátricas.\n"
        "3. Privacidade por Design e Acessibilidade: sem cadastros na internet e com respeito estrito às diretrizes da LGPD.",
    )

    # ==========================================================================
    # SLIDE 6: PESQUISA E REFERÊNCIAS: MATERIAIS DE ESTUDO
    # ==========================================================================
    s6 = prs.slides.add_slide(blank_layout)
    add_header(s6, "Slide 06 | Pesquisa e Referências", "Bases Científicas, Normativas e Técnicas que Fundamentaram o Projeto", 6)

    refs_data = [
        (
            "1. Caderneta da Criança: Passaporte da Cidadania",
            "Ministério da Saúde do Brasil (2022)",
            COLOR_PRIMARY,
            "Forneceu a base oficial para o calendário vacinal do PNI (doses ao nascer, 2, 3, 4, 5, 6, 9, 12 e 15 meses), "
            "parâmetros de marcos de desenvolvimento e modelo de integração com a atenção primária do SUS.",
        ),
        (
            "2. WHO Child Growth Standards (Padrões de Crescimento Infantil)",
            "World Health Organization - OMS (2006)",
            COLOR_PRIMARY,
            "Estabeleceu os limites antropométricos saudáveis para lactentes e norteou a implementação de regras de plausibilidade "
            "no app, prevenindo digitações acidentais errôneas de peso e estatura.",
        ),
        (
            "3. Guia de Aleitamento Materno e Alimentação Complementar",
            "Sociedade Brasileira de Pediatria - SBP (2021)",
            COLOR_ACCENT,
            "Fundamentou a modelagem de dados da alimentação (mama esquerda/direita, fórmula, introdução alimentar) e a diretriz "
            "de que mensagens do app devem ser educativas, orientando busca médica em sinais de alerta.",
        ),
        (
            "4. Material Design 3 & Android Accessibility Guidelines (WCAG 2.1)",
            "Google Android Developers (2024)",
            COLOR_PURPLE,
            "Definiu padrões de usabilidade moderna em Jetpack Compose: áreas táteis de 48dp, contraste de cores superior a 4,5:1, "
            "suporte à escala de fonte do sistema operacional e arquitetura com navegação por barra inferior.",
        ),
        (
            "5. Lei Geral de Proteção de Dados Pessoais (LGPD - Art. 14)",
            "Presidência da República do Brasil (Lei nº 13.709/2018)",
            COLOR_PRIMARY,
            "Determinou o princípio de coleta mínima de dados de menores de idade e embasou a decisão de arquitetura 100% offline, "
            "sem login em nuvem, sem analytics e sem permissão de acesso à internet no AndroidManifest.",
        ),
    ]

    for i, (title, author, col, desc) in enumerate(refs_data):
        pos_top = 1.8 + i * 1.02
        add_rounded_card(s6, 0.8, pos_top, 11.733, 0.92)
        
        # Barra de cor lateral
        add_rounded_card(s6, 0.95, pos_top + 0.12, 0.12, 0.68, bg_color=col, border_color=None)
        
        # Texto
        tx = s6.shapes.add_textbox(Inches(1.2), Inches(pos_top + 0.08), Inches(11.1), Inches(0.8))
        tf = tx.text_frame
        tf.word_wrap = True
        p1 = tf.paragraphs[0]
        add_run(p1, title + " — ", bold=True, size=11, color=TEXT_TITLE)
        add_run(p1, author, bold=True, size=10.5, color=col)
        
        p2 = tf.add_paragraph()
        p2.text = desc
        p2.font.size = Pt(10)
        p2.font.color.rgb = TEXT_MAIN
        p2.space_before = Pt(2)

    set_notes(
        s6,
        "No Slide 6, destacamos que o Crescer Bem não é uma ideia abstrata: sua concepção repousa sobre bases sólidas.\n"
        "No campo pediátrico, nos embasamos na Caderneta Oficial do Ministério da Saúde, nos padrões de crescimento da Organização Mundial da Saúde e nas diretrizes da Sociedade Brasileira de Pediatria.\n"
        "No campo da tecnologia e acessibilidade, adotamos as normas do Material Design 3 e WCAG 2.1 da Google.\n"
        "E no campo ético-legal, seguimos à risca o Artigo 14 da LGPD, que tutela os dados pessoais de crianças com o mais alto nível de exigência de proteção.",
    )

    # ==========================================================================
    # SLIDE 7: APRENDIZADOS CHAVE DA PESQUISA
    # ==========================================================================
    s7 = prs.slides.add_slide(blank_layout)
    add_header(s7, "Slide 07 | Aprendizados da Pesquisa", "Como as Referências Teóricas se Transformaram em Recursos do App", 7)

    learnings = [
        (
            "CALENDÁRIO VACINAL FLEXÍVEL",
            "Caderneta da Criança & PNI",
            COLOR_PRIMARY,
            COLOR_PRIMARY_LIGHT,
            "Aprendemos que o calendário vacinal do aplicativo deve ser estruturado em faixas etárias sugeridas "
            "(ao nascer, 2, 3, 4, 5, 6, 9, 12 e 15 meses), permitindo cadastrar doses da rede pública ou privada "
            "e reafirmando expressamente que o pediatra e a caderneta física são as autoridades oficiais de saúde.",
        ),
        (
            "ALERTAS EDUCATIVOS, NÃO DIAGNÓSTICOS",
            "Diretrizes da SBP & Padrões da OMS",
            COLOR_ACCENT,
            COLOR_ACCENT_LIGHT,
            "A literatura médica nos ensinou que ferramentas digitais não devem formular diagnósticos ou prescrever remédios. "
            "Implementamos cartões educativos que detectam sinais de alerta (como febre em bebês com menos de 3 meses) "
            "e orientam os responsáveis a buscar atendimento médico imediato.",
        ),
        (
            "ERGONOMIA PARA CUIDADOS NOTURNOS",
            "Material 3 & Acessibilidade WCAG",
            COLOR_PURPLE,
            COLOR_PURPLE_LIGHT,
            "A pesquisa ergonômica revelou que pais cansados operam o celular com apenas uma mão e no escuro. "
            "Definimos alvos de toque amplos (mínimo de 48dp), paleta verde-sálvia suave para não prejudicar a visão noturna "
            "e seções recolhíveis tipo accordion para evitar rolagem cansativa de tela.",
        ),
        (
            "PRIVACIDADE ABSOLUTA POR DESIGN",
            "LGPD (Lei 13.709/2018, Art. 14)",
            COLOR_PRIMARY,
            COLOR_PRIMARY_LIGHT,
            "O estudo da legislação consolidou a decisão arquitetural da 'Coleta Mínima' e isolamento local: "
            "o aplicativo não declara permissão de internet no AndroidManifest, bloqueia backup automático em nuvem "
            "e mantém fotos e PDFs exclusivamente no diretório privado do aplicativo.",
        ),
    ]

    for i, (l_tit, l_ref, l_col, l_bg, l_desc) in enumerate(learnings):
        row = i // 2
        col = i % 2
        pos_left = 0.8 + col * 5.95
        pos_top = 1.8 + row * 2.65
        
        add_rounded_card(s7, pos_left, pos_top, 5.75, 2.45)
        
        # Tag superior do aprendizado
        tag = add_rounded_card(s7, pos_left + 0.25, pos_top + 0.2, 5.25, 0.35, bg_color=l_bg, border_color=None)
        p_tag = tag.text_frame.paragraphs[0]
        p_tag.text = f"{l_tit}  ({l_ref})"
        p_tag.alignment = PP_ALIGN.CENTER
        p_tag.font.bold = True
        p_tag.font.size = Pt(9)
        p_tag.font.color.rgb = l_col

        # Descrição do aprendizado
        tx = s7.shapes.add_textbox(Inches(pos_left + 0.25), Inches(pos_top + 0.65), Inches(5.25), Inches(1.65))
        tf = tx.text_frame
        tf.word_wrap = True
        p_d = tf.paragraphs[0]
        p_d.text = l_desc
        p_d.font.size = Pt(11)
        p_d.font.color.rgb = TEXT_MAIN

    set_notes(
        s7,
        "No Slide 7, mostramos como cada material de estudo impactou diretamente as decisões técnicas do Crescer Bem.\n"
        "Com a Caderneta do SUS, aprendemos a desenhar o calendário vacinal por faixa etária recomendada, sem engessar as vacinas da rede particular.\n"
        "Com a Sociedade Brasileira de Pediatria, entendemos a fronteira ética: nosso app nunca diagnostica. Se um bebê de 2 meses tem febre, ele orienta com calma a procurar uma unidade de saúde.\n"
        "Com o Material Design 3, eliminamos botões pequenos e criamos seções expansíveis confortáveis.\n"
        "E com a LGPD, implementamos a arquitetura de vazamento zero: os dados morrem dentro do próprio aparelho.",
    )

    # ==========================================================================
    # SLIDE 8: METODOLOGIA DE DESENVOLVIMENTO
    # ==========================================================================
    s8 = prs.slides.add_slide(blank_layout)
    add_header(s8, "Slide 08 | Metodologia de Desenvolvimento", "Processo Iterativo de Engenharia de Software e Tecnologias Utilizadas", 8)

    # Faixa da Metodologia
    m_box = add_rounded_card(s8, 0.8, 1.8, 11.733, 0.65, bg_color=COLOR_PRIMARY_LIGHT, border_color=COLOR_PRIMARY)
    tf_m = m_box.text_frame
    tf_m.vertical_anchor = MSO_ANCHOR.MIDDLE
    p_m = tf_m.paragraphs[0]
    p_m.alignment = PP_ALIGN.CENTER
    add_run(p_m, "METODOLOGIA ADOTADA: ", bold=True, color=COLOR_PRIMARY)
    add_run(p_m, "Engenharia Ágil com princípios de Design Thinking (empatia com os pais) e Clean Architecture (MVVM).")
    p_m.font.size = Pt(11)

    # 6 Etapas em grade 3x2
    stages = [
        ("1. Requisitos e Escopo", "Mapeamento das dores parentais, escrita de User Stories e priorização rigorosa em P0 (MVP), P1 (evolução) e P2 (futuro).", COLOR_PRIMARY),
        ("2. Pesquisa e Parâmetros", "Levantamento das doses vacinais oficiais do PNI, validação de limites de peso/altura (OMS) e diretrizes de conduta médica.", COLOR_PRIMARY),
        ("3. Prototipagem e Wireframes", "Estruturação dos fluxos nas 5 telas principais (Hoje, Calendário, Bebê, Saúde e Alimentação) e botão universal de registro.", COLOR_ACCENT),
        ("4. Design System & UI/UX", "Implementação com Jetpack Compose e Material 3, tema verde-sálvia acolhedor, alto contraste e áreas táteis acessíveis.", COLOR_ACCENT),
        ("5. Implementação Técnica", "Desenvolvimento em Kotlin 2.2, Android SDK 36, banco Room (SQLite), Flow reativo e gerador nativo de PDFs com PdfDocument.", COLOR_PURPLE),
        ("6. Testes e Validação de QA", "Testes unitários em JUnit 4 para regras puras e testes de integração com Robolectric para persistência do Room em memória.", COLOR_PURPLE),
    ]

    for i, (st_t, st_d, st_col) in enumerate(stages):
        row = i // 3
        col = i % 3
        pos_left = 0.8 + col * 4.02
        pos_top = 2.6 + row * 1.95
        
        add_rounded_card(s8, pos_left, pos_top, 3.68, 1.8)
        
        # Tag da etapa
        tag = add_rounded_card(s8, pos_left + 0.2, pos_top + 0.15, 3.28, 0.32, bg_color=st_col, border_color=None)
        p_tg = tag.text_frame.paragraphs[0]
        p_tg.text = st_t
        p_tg.alignment = PP_ALIGN.CENTER
        p_tg.font.bold = True
        p_tg.font.size = Pt(10)
        p_tg.font.color.rgb = TEXT_WHITE

        # Descrição
        tx = s8.shapes.add_textbox(Inches(pos_left + 0.2), Inches(pos_top + 0.52), Inches(3.28), Inches(1.2))
        tf = tx.text_frame
        tf.word_wrap = True
        p_txt = tf.paragraphs[0]
        p_txt.text = st_d
        p_txt.font.size = Pt(10)
        p_txt.font.color.rgb = TEXT_MAIN

    # Faixa de Tecnologias no Rodapé
    tech_bar = add_rounded_card(s8, 0.8, 6.55, 11.733, 0.45, bg_color=COLOR_PRIMARY, border_color=None)
    p_tech = tech_bar.text_frame.paragraphs[0]
    p_tech.alignment = PP_ALIGN.CENTER
    p_tech.text = "STACK TECNOLÓGICA: Kotlin 2.2  •  Jetpack Compose  •  Material 3  •  Room SQLite  •  Coil 3  •  PdfDocument  •  Robolectric  •  Android SDK 36"
    p_tech.font.name = FONT_HEADING
    p_tech.font.bold = True
    p_tech.font.size = Pt(10)
    p_tech.font.color.rgb = TEXT_WHITE

    set_notes(
        s8,
        "No Slide 8, detalhamos o processo de desenvolvimento e nossa arquitetura técnica.\n"
        "Seguimos seis etapas ágeis estruturadas: partimos do levantamento de requisitos com histórias de usuário, passando pela pesquisa médica, prototipação das 5 telas essenciais e construção de um Design System robusto no Jetpack Compose.\n"
        "No desenvolvimento, empregamos Kotlin nativo com banco de dados Room (SQLite) com exclusões em cascata e renderização vetorial de PDF nativa do Android.\n"
        "E na qualidade, construímos uma suíte completa de testes automatizados com JUnit 4 e Robolectric, garantindo que as regras de vacinas, cálculo de idade e geração de relatórios funcionem sem falhas antes de gerar o APK.",
    )

    # ==========================================================================
    # SLIDE 9: CONTRIBUIÇÃO PARA A COMUNIDADE: ALINHAMENTO COM OS ODS
    # ==========================================================================
    s9 = prs.slides.add_slide(blank_layout)
    add_header(s9, "Slide 09 | Contribuição Social e ODS", "Alinhamento com a Agenda 2030 da Organização das Nações Unidas (ONU)", 9)

    ods_cards = [
        (
            "ODS 3: SAÚDE E BEM-ESTAR",
            "Meta 3.2: Acabar com mortes evitáveis de recém-nascidos e crianças menores de 5 anos",
            COLOR_PRIMARY,
            COLOR_PRIMARY_LIGHT,
            [
                ("Adesão ao Calendário Vacinal: ", "O aplicativo alerta e organiza todas as doses previstas pelo PNI, auxiliando a evitar esquecimentos que deixariam a criança vulnerável a doenças preveníveis."),
                ("Vigilância Contínua do Crescimento: ", "O histórico de peso e altura permite identificar precocemente desacelerações de ganho ponderal, desnutrição ou obesidade infantil."),
                ("Educação Preventiva e Alertas: ", "Identifica sinais de alerta clínicos (ex.: febre, recusa alimentar persistente) e instrui a família sobre o momento oportuno de buscar atendimento médico."),
                ("Subsídio às Consultas Médicas: ", "O relatório em PDF fornece ao pediatra dados fidedignos e organizados sobre alimentação, curvas e intercorrências passadas."),
            ],
        ),
        (
            "ODS 10: REDUÇÃO DAS DESIGUALDADES",
            "Meta 10.2: Promover a inclusão social e o acesso igualitário à tecnologia",
            COLOR_ACCENT,
            COLOR_ACCENT_LIGHT,
            [
                ("100% Gratuito e Sem Anúncios: ", "Não comercializa recursos, não possui assinaturas pagas e não obriga o usuário a assistir publicidade para cuidar do filho."),
                ("Zero Consumo de Dados Móveis: ", "Por funcionar inteiramente offline, famílias sem plano de internet móvel ou em áreas rurais usam o aplicativo com 100% dos recursos habilitados."),
                ("Compatibilidade Democrática: ", "Projetado para rodar perfeitamente a partir do Android 8.0 (minSdk 26), funcionando com alta fluidez mesmo em smartphones simples."),
                ("Acessibilidade Universal: ", "Tipografia escalável no sistema, contraste WCAG elevado e interface pensada para usuários com diferentes níveis de letramento digital."),
            ],
        ),
    ]

    for i, (ods_tit, ods_meta, ods_col, ods_bg, bullets) in enumerate(ods_cards):
        pos_left = 0.8 + i * 5.95
        add_rounded_card(s9, pos_left, 1.8, 5.78, 5.15)
        
        # Banner do ODS
        banner = add_rounded_card(s9, pos_left + 0.25, 2.05, 5.28, 0.45, bg_color=ods_col, border_color=None)
        p_b = banner.text_frame.paragraphs[0]
        p_b.text = ods_tit
        p_b.alignment = PP_ALIGN.CENTER
        p_b.font.name = FONT_HEADING
        p_b.font.bold = True
        p_b.font.size = Pt(11)
        p_b.font.color.rgb = TEXT_WHITE

        # Meta do ODS
        meta_box = s9.shapes.add_textbox(Inches(pos_left + 0.25), Inches(2.6), Inches(5.28), Inches(0.65))
        tf_m = meta_box.text_frame
        tf_m.word_wrap = True
        p_m = tf_m.paragraphs[0]
        p_m.text = ods_meta
        p_m.font.name = FONT_BODY
        p_m.font.italic = True
        p_m.font.bold = True
        p_m.font.size = Pt(10)
        p_m.font.color.rgb = ods_col

        # Bullets
        tx = s9.shapes.add_textbox(Inches(pos_left + 0.25), Inches(3.3), Inches(5.28), Inches(3.45))
        tf = tx.text_frame
        tf.word_wrap = True
        for b_idx, (b_t, b_d) in enumerate(bullets):
            p = tf.paragraphs[0] if b_idx == 0 else tf.add_paragraph()
            p.space_after = Pt(11)
            p.font.name = FONT_BODY
            p.font.size = Pt(10.5)
            add_run(p, "• " + b_t, bold=True, color=ods_col)
            add_run(p, b_d, color=TEXT_MAIN)

    set_notes(
        s9,
        "No Slide 9, conectamos o Crescer Bem com a Agenda 2030 das Nações Unidas, validando seu caráter de projeto de impacto social.\n"
        "No ODS 3 — Saúde e Bem-Estar —, atuamos diretamente na prevenção da mortalidade infantil ao combater o atraso vacinal e fornecer histórico fidedigno de peso e sintomas para os pediatras.\n"
        "No ODS 10 — Redução das Desigualdades —, garantimos que a tecnologia de ponta não seja privilégio de quem tem dinheiro para pagar assinaturas caras ou planos de internet pós-pagos. Nosso app é gratuito, funciona 100% offline e roda com leveza em aparelhos Android básicos de baixo custo.",
    )

    # ==========================================================================
    # SLIDE 10: PRÓXIMOS PASSOS E CONCLUSÃO
    # ==========================================================================
    s10 = prs.slides.add_slide(blank_layout)
    add_header(s10, "Slide 10 | Próximos Passos e Conclusão", "Evolução do Produto, Visão de Futuro e Agradecimentos", 10)

    # Card Esquerdo: Roadmap de Evolução
    add_rounded_card(s10, 0.8, 1.8, 5.78, 5.15)
    tag_r = add_rounded_card(s10, 1.05, 2.05, 5.28, 0.42, bg_color=COLOR_PRIMARY, border_color=None)
    p_tr = tag_r.text_frame.paragraphs[0]
    p_tr.text = "ROADMAP DE EVOLUÇÃO (PRÓXIMOS PASSOS)"
    p_tr.alignment = PP_ALIGN.CENTER
    p_tr.font.bold = True
    p_tr.font.size = Pt(10.5)
    p_tr.font.color.rgb = TEXT_WHITE

    tx_r = s10.shapes.add_textbox(Inches(1.05), Inches(2.65), Inches(5.28), Inches(4.1))
    tf_r = tx_r.text_frame
    tf_r.word_wrap = True

    roadmap_items = [
        ("Curto Prazo (Versão P1):", COLOR_PRIMARY, [
            "Implementação de gráficos visuais de curvas de crescimento com percentis da OMS.",
            "Lembretes e notificações locais para consultas e vacinas agendadas.",
            "Exportação e restauração de backup local criptografado em arquivo privado.",
        ]),
        ("Médio Prazo (Versão P2):", COLOR_ACCENT, [
            "Módulo de monitoramento de sono, banhos e trocas de fralda do bebê.",
            "Bloqueio de acesso ao aplicativo por biometria do celular para maior segurança.",
        ]),
        ("Validação em Campo:", COLOR_PURPLE, [
            "Aplicação de testes de usabilidade com grupos de mães lactantes e profissionais da atenção primária (UBS).",
        ]),
    ]

    first_par = True
    for sec_title, sec_col, sec_bullets in roadmap_items:
        p_h = tf_r.paragraphs[0] if first_par else tf_r.add_paragraph()
        first_par = False
        p_h.text = sec_title
        p_h.font.bold = True
        p_h.font.size = Pt(11)
        p_h.font.color.rgb = sec_col
        p_h.space_before = Pt(8) if not first_par else Pt(0)
        
        for b in sec_bullets:
            p_b = tf_r.add_paragraph()
            p_b.text = "• " + b
            p_b.font.size = Pt(10)
            p_b.font.color.rgb = TEXT_MAIN
            p_b.space_after = Pt(2)

    # Card Direito: Visão de Futuro, Agradecimentos e Contato
    add_rounded_card(s10, 6.75, 1.8, 5.78, 5.15)
    tag_v = add_rounded_card(s10, 7.0, 2.05, 5.28, 0.42, bg_color=COLOR_ACCENT, border_color=None)
    p_tv = tag_v.text_frame.paragraphs[0]
    p_tv.text = "VISÃO DE FUTURO & CONTATO"
    p_tv.alignment = PP_ALIGN.CENTER
    p_tv.font.bold = True
    p_tv.font.size = Pt(10.5)
    p_tv.font.color.rgb = TEXT_WHITE

    tx_v = s10.shapes.add_textbox(Inches(7.0), Inches(2.65), Inches(5.28), Inches(4.1))
    tf_v = tx_v.text_frame
    tf_v.word_wrap = True

    p_vf_h = tf_v.paragraphs[0]
    p_vf_h.text = "Visão de Longo Prazo do Crescer Bem:"
    p_vf_h.font.bold = True
    p_vf_h.font.size = Pt(11)
    p_vf_h.font.color.rgb = COLOR_PRIMARY
    
    p_vf_d = tf_v.add_paragraph()
    p_vf_d.text = (
        "Consolidar o Crescer Bem como uma referência de tecnologia assistiva comunitária em saúde da família, "
        "comprovando que inovação móvel de ponta, acessibilidade e respeito irrestrito à privacidade podem caminhar juntas."
    )
    p_vf_d.font.size = Pt(10.5)
    p_vf_d.font.color.rgb = TEXT_MAIN
    p_vf_d.space_after = Pt(12)

    p_frase = tf_v.add_paragraph()
    p_frase.text = "“Cuidar de quem cuida: a tecnologia como aliada acolhedora da primeira infância.”"
    p_frase.font.italic = True
    p_frase.font.bold = True
    p_frase.font.size = Pt(11)
    p_frase.font.color.rgb = COLOR_ACCENT
    p_frase.space_after = Pt(14)

    p_ag_h = tf_v.add_paragraph()
    p_ag_h.text = "Agradecimentos:"
    p_ag_h.font.bold = True
    p_ag_h.font.size = Pt(11)
    p_ag_h.font.color.rgb = COLOR_PRIMARY
    
    p_ag_d = tf_v.add_paragraph()
    p_ag_d.text = "À Profª. Mônica Norris Ribeiro pela dedicação e valorosa orientação acadêmica, e ao corpo docente do UniFOA."
    p_ag_d.font.size = Pt(10)
    p_ag_d.font.color.rgb = TEXT_MAIN
    p_ag_d.space_after = Pt(12)

    p_ct_h = tf_v.add_paragraph()
    p_ct_h.text = "Contato do Apresentador:"
    p_ct_h.font.bold = True
    p_ct_h.font.size = Pt(11)
    p_ct_h.font.color.rgb = COLOR_PRIMARY
    
    p_ct_d = tf_v.add_paragraph()
    p_ct_d.text = "Felipe de Lima Souza  •  CST em Análise e Desenvolvimento de Sistemas\nEmail: felipe.lima@aluno.unifoa.edu.br"
    p_ct_d.font.size = Pt(10)
    p_ct_d.font.color.rgb = TEXT_MAIN

    set_notes(
        s10,
        "Para encerrar a apresentação, no Slide 10 mostramos que o projeto tem um horizonte claro de evolução.\n"
        "Já mapeamos as próximas versões P1 e P2: introduziremos os gráficos visuais de percentis da OMS, notificações locais de vacinas e biometria.\n"
        "E o nosso objetivo final é levar o aplicativo para testes de usabilidade em Unidades Básicas de Saúde, ouvindo mães reais para aprimorar ainda mais o produto.\n"
        "Gostaria de agradecer sinceramente à professora orientadora Mônica Norris Ribeiro por todo o apoio técnico e pedagógico neste semestre.\n"
        "Muito obrigado a todos pela atenção, e coloco-me à inteira disposição da banca para responder perguntas e demonstrar o funcionamento do aplicativo!",
    )

    return prs


def main() -> None:
    prs = build_presentation()
    out_root = Path("Crescer Bem - Apresentacao de Projeto.pptx")
    out_foa = Path("Foa/Crescer Bem - Apresentacao de Projeto.pptx")

    prs.save(out_root)
    prs.save(out_foa)

    downloads_d = Path("D:/Downloads D")
    if downloads_d.exists():
        shutil.copy(out_root, downloads_d / out_root.name)

    print(f"Apresentação PPTX gerada com sucesso em: {out_root.resolve()}")
    print(f"Apresentação PPTX gerada com sucesso em: {out_foa.resolve()}")


if __name__ == "__main__":
    main()
