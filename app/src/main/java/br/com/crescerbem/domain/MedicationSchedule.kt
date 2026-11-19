package br.com.crescerbem.domain

import java.time.LocalDateTime

/** Regras do agendamento repetido de medicamentos: de tantas em tantas horas, por N dias. */
object MedicationSchedule {
    const val MAX_DAYS = 30
    const val MAX_INTERVAL_HOURS = 72
    const val MAX_DOSES = 240

    /**
     * Horários das doses: a primeira em [start] e as seguintes a cada [intervalHours],
     * enquanto couberem nos [days] dias a partir de [start] (o fim do período não entra).
     * Ex.: 8 em 8 horas por 7 dias = 21 doses.
     */
    fun times(start: LocalDateTime, intervalHours: Int, days: Int): List<LocalDateTime> {
        require(intervalHours >= 1 && days >= 1) { "Intervalo e dias precisam ser positivos." }
        val end = start.plusDays(days.toLong())
        return generateSequence(start) { it.plusHours(intervalHours.toLong()) }
            .takeWhile { it.isBefore(end) }
            .toList()
    }

    /** Mensagem de erro para os valores digitados, ou nulo se estiverem válidos. */
    fun validate(intervalHours: Int?, days: Int?): String? = when {
        intervalHours == null || intervalHours !in 1..MAX_INTERVAL_HOURS ->
            "Informe o intervalo em horas (de 1 a $MAX_INTERVAL_HOURS)."
        days == null || days !in 1..MAX_DAYS ->
            "Informe por quantos dias repetir (de 1 a $MAX_DAYS)."
        (days * 24 + intervalHours - 1) / intervalHours > MAX_DOSES ->
            "São doses demais para agendar de uma vez (máximo $MAX_DOSES). Aumente o intervalo ou reduza os dias."
        else -> null
    }
}
