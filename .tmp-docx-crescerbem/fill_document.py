from __future__ import annotations

from copy import copy
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from lxml import etree


SOURCE = Path(".tmp-docx-crescerbem/template.docx")
OUTPUT = Path("Crescer Bem - Projeto Integrado preenchido.docx")
NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}


REPLACEMENTS = {
    4: "Projeto Crescer Bem Aplicativo de Impacto Social",
    7: "Equipe responsável pelo planejamento, desenvolvimento, interface, acessibilidade e qualidade do aplicativo.",
    8: "Integrantes: nomes a serem informados pela equipe responsável.",
    9: "Papéis e responsabilidades: produto e requisitos, desenvolvimento Android, interface e acessibilidade, testes e qualidade.",
    11: "O Crescer Bem foi concebido para apoiar a organização de registros de saúde infantil no dia a dia, sem substituir o acompanhamento profissional.",
    12: "Problema identificado: responsáveis por recém-nascidos precisam reunir, de forma simples, informações de saúde que costumam ficar dispersas entre memória, caderneta, anotações e fotos. Essa organização é especialmente difícil na rotina cansativa dos primeiros meses.",
    13: "Público-alvo: pais, mães e outros responsáveis por recém-nascidos e bebês, em especial quem precisa registrar informações pelo celular com rapidez e em poucos passos.",
    14: "Impacto social esperado: facilitar o acompanhamento cotidiano e a preparação para conversas com profissionais de saúde, com registros organizados de crescimento, vacinas, consultas, sintomas, alimentação e momentos, sem substituir o cuidado pediátrico.",
    16: "O projeto contribui para objetivos que promovem saúde, bem-estar e acesso mais inclusivo a ferramentas de apoio ao cuidado.",
    17: "ODS principal: ODS 3 - Saúde e Bem-Estar, ao apoiar famílias na organização de informações relevantes para o acompanhamento infantil.",
    18: "ODS secundário: ODS 10 - Redução das Desigualdades, por oferecer uma ferramenta acessível, offline e sem necessidade de conta para apoiar a rotina de cuidado.",
    20: "Objetivo geral: desenvolver um aplicativo Android que permita aos responsáveis registrar e consultar, com clareza e privacidade, o acompanhamento cotidiano da saúde de bebês.",
    22: "Registrar dados de crescimento, vacinas, consultas, sintomas, alimentação, fotos e momentos em poucos toques.",
    23: "Organizar os registros por bebê, calendário e histórico, com relatórios em PDF para o período escolhido.",
    24: "Manter os dados somente no aparelho, com linguagem educativa, acessibilidade e alertas que orientam procurar um profissional quando necessário.",
    27: "[x] Cadastro de um ou mais bebês e registro de dados básicos, medidas de crescimento, fotos e momentos.",
    28: "[x] Calendário de registros, vacinas, consultas, sintomas e alimentação, com criação, edição e exclusão.",
    29: "[x] Geração local de relatório em PDF por período e compartilhamento somente por ação explícita do responsável.",
    31: "Segurança: coleta mínima; dados, fotos e PDFs ficam na área privada do aplicativo, sem conta, servidor ou permissão de internet.",
    32: "Interface: Jetpack Compose e Material 3, com botões e áreas de toque amplas, contraste, rótulos visíveis e suporte à escala de fonte.",
    33: "Performance: funcionamento offline, persistência local com Room e operações essenciais sem depender de serviços externos.",
    35: "Todo registro pertence ao bebê selecionado; a exclusão de um bebê remove seus registros e fotos após confirmação.",
    36: "Alertas são educativos, nunca diagnosticam; o calendário vacinal é uma sugestão editável e a caderneta e o pediatra permanecem como referência oficial.",
    38: "Histórias escritas a partir da perspectiva de quem cuida do bebê e precisa consultar ou registrar informações rapidamente.",
    39: "Responsável: Como responsável, quero registrar peso, vacina, alimentação ou um sintoma em poucos toques para manter o histórico do bebê organizado.",
    40: "Responsável: Como responsável, quero consultar o calendário e os registros por dia para lembrar consultas, acompanhar vacinas e revisar o que aconteceu.",
    41: "Responsável: Como responsável, quero gerar um relatório em PDF de um período para compartilhar as informações organizadas durante uma consulta, quando eu escolher fazê-lo.",
    43: "Stack tecnológica: Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room com SQLite local, Coil para imagens locais e PdfDocument para relatórios offline.",
    44: "Ferramentas de design: Android Studio, componentes do Jetpack Compose e sistema visual documentado para cores, tipografia, estados e acessibilidade.",
    45: "Padrões visuais: interface calma e acolhedora, com cartões, seções recolhíveis, cores de alto contraste, ícones com rótulos e ações principais sempre evidentes.",
    47: "Fases previstas para transformar o escopo do MVP em uma versão testável do aplicativo.",
    48: "Planejamento: levantamento do problema, definição do escopo do MVP, fluxos de uso, privacidade e protótipo das telas principais.",
    49: "Desenvolvimento da base: modelagem local dos dados, banco Room, regras de domínio, armazenamento privado de fotos e geração de PDF.",
    50: "Desenvolvimento da interface: telas de Hoje, Calendário, Bebê, Saúde e Alimentação, com formulários de registro e navegação acessível.",
    51: "Testes e ajustes: testes de regras e persistência, revisão de linguagem, acessibilidade, privacidade e validação dos fluxos principais.",
    52: "Entrega do MVP: geração do APK de teste, acompanhamento de defeitos e evolução planejada para lembretes, gráficos e backup local.",
    54: "O MVP é considerado bem-sucedido quando os fluxos essenciais funcionam de forma clara, privada e verificável.",
    55: "Critério 1: o responsável deve conseguir cadastrar um bebê e criar, editar e excluir registros que apareçam no calendário na data correta.",
    56: "Critério 2: os dados devem permanecer no aparelho e o relatório em PDF deve incluir somente o bebê, o período e os tipos de registro escolhidos.",
    57: "Cenário de teste: cadastrar um bebê de teste -> registrar uma medida de peso e uma vacina -> abrir o calendário e gerar o PDF do período -> os dois registros aparecem corretamente, sem envio automático de dados.",
}


def replace_text_in_paragraph(paragraph: etree._Element, text: str) -> None:
    text_nodes = paragraph.xpath(".//w:t", namespaces=NS)
    if not text_nodes:
        raise ValueError("Slot sem nó de texto")
    text_nodes[0].text = text
    for node in text_nodes[1:]:
        node.text = ""


def main() -> None:
    with ZipFile(SOURCE, "r") as source_zip:
        document = etree.fromstring(source_zip.read("word/document.xml"))
        paragraphs = document.xpath("//w:body/w:p", namespaces=NS)
        if len(paragraphs) != 59:
            raise ValueError(f"Estrutura inesperada: {len(paragraphs)} parágrafos")

        for index, text in REPLACEMENTS.items():
            replace_text_in_paragraph(paragraphs[index], text)

        altered_document = etree.tostring(
            document,
            xml_declaration=True,
            encoding="UTF-8",
            standalone=True,
        )

        with ZipFile(OUTPUT, "w", compression=ZIP_DEFLATED) as output_zip:
            for original_info in source_zip.infolist():
                info = copy(original_info)
                data = altered_document if original_info.filename == "word/document.xml" else source_zip.read(original_info.filename)
                output_zip.writestr(info, data)


if __name__ == "__main__":
    main()
