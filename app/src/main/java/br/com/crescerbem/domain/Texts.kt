package br.com.crescerbem.domain

/**
 * Textos educativos e avisos fixos do produto, num só lugar para revisão de linguagem
 * (Fable) e QA (Antigravity). Ver docs/escopo-mvp.md, seção 7.
 */
object Texts {
    const val DISCLAIMER =
        "O Crescer Bem ajuda a organizar registros. Ele não substitui a avaliação de um profissional de saúde."

    const val WELCOME =
        "O Crescer Bem organiza o dia a dia do seu bebê: momentos, vacinas, consultas, medidas e alimentação. " +
            "Ele não substitui o pediatra."

    const val ATTENTION_TITLE = "Sinais que merecem atenção"

    const val ATTENTION_ADVICE =
        "Esses sinais merecem avaliação de um profissional de saúde o quanto antes. " +
            "Em caso de emergência, ligue 192 (SAMU)."

    const val VACCINE_NOTE =
        "Sugestão baseada no calendário nacional de vacinação. A caderneta e o pediatra são a referência oficial."

    const val MEDICATION_NOTE =
        "Use as doses e os horários indicados pelo pediatra. O app só ajuda a organizar e lembrar o que foi dado."

    const val REPORT_FOOTER =
        "Registro feito pela família no Crescer Bem. Não substitui documentos médicos."

    const val PRIVACY_SUMMARY =
        "Seus registros e fotos ficam só neste aparelho. O app não usa internet, não tem conta " +
            "e não envia dados. O relatório em PDF só sai do celular quando você toca em Compartilhar."

    const val DATA_LOSS_WARNING =
        "Como tudo fica só neste aparelho, desinstalar o app ou perder o celular apaga os registros. " +
            "Gere relatórios em PDF de tempos em tempos para guardar uma cópia."

    const val EMPTY_PERIOD = "Nenhum registro no período."
}
