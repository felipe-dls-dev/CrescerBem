package br.com.crescerbem.domain

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Idade do bebê descrita do jeito que pais e pediatras costumam falar. */
object BabyAge {
    fun days(birth: LocalDate, on: LocalDate): Long = ChronoUnit.DAYS.between(birth, on)

    fun months(birth: LocalDate, on: LocalDate): Int =
        if (on.isBefore(birth)) 0 else Period.between(birth, on).toTotalMonths().toInt()

    /**
     * - até 13 dias: "5 dias";
     * - até 59 dias: semanas e dias ("3 semanas e 2 dias");
     * - até 1 ano: meses e dias ("4 meses e 10 dias");
     * - depois: anos e meses ("1 ano e 2 meses").
     */
    fun describe(birth: LocalDate, on: LocalDate): String {
        if (on.isBefore(birth)) return "Nascimento previsto para ${Formats.date(birth)}"
        val totalDays = days(birth, on)
        if (totalDays == 0L) return "Nasceu hoje"
        if (totalDays < 14) return plural(totalDays, "dia", "dias")
        if (totalDays < 60) {
            val weeks = totalDays / 7
            val rest = totalDays % 7
            return join(plural(weeks, "semana", "semanas"), rest.takeIf { it > 0 }?.let { plural(it, "dia", "dias") })
        }
        val period = Period.between(birth, on)
        if (period.years == 0) {
            return join(
                plural(period.months, "mês", "meses"),
                period.days.takeIf { it > 0 }?.let { plural(it, "dia", "dias") },
            )
        }
        return join(
            plural(period.years, "ano", "anos"),
            period.months.takeIf { it > 0 }?.let { plural(it, "mês", "meses") },
        )
    }

    /** Frase completa para o cabeçalho de Hoje: "Ana tem 3 semanas", "Ana nasceu hoje". */
    fun sentence(name: String, birth: LocalDate, on: LocalDate): String = when {
        on.isBefore(birth) -> describe(birth, on)
        on == birth -> "$name nasceu hoje"
        else -> "$name tem ${describe(birth, on)}"
    }

    private fun join(first: String, second: String?): String = if (second == null) first else "$first e $second"
}
