package br.com.crescerbem.domain

import br.com.crescerbem.data.model.RecordType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Grade mensal do calendário, começando no domingo (padrão brasileiro). */
object CalendarGrid {
    /** Células da grade: nulos preenchem o começo e o fim para completar semanas de 7 dias. */
    fun cells(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val withLeading: List<LocalDate?> = List(leading) { null } + days
        val trailing = (7 - withLeading.size % 7) % 7
        return withLeading + List(trailing) { null }
    }

    fun weekdayInitials(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): List<String> {
        val initials = mapOf(
            DayOfWeek.SUNDAY to "D",
            DayOfWeek.MONDAY to "S",
            DayOfWeek.TUESDAY to "T",
            DayOfWeek.WEDNESDAY to "Q",
            DayOfWeek.THURSDAY to "Q",
            DayOfWeek.FRIDAY to "S",
            DayOfWeek.SATURDAY to "S",
        )
        return (0 until 7).map { initials.getValue(firstDayOfWeek.plus(it.toLong())) }
    }

    fun weekdayNames(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): List<String> {
        val names = mapOf(
            DayOfWeek.SUNDAY to "domingo",
            DayOfWeek.MONDAY to "segunda-feira",
            DayOfWeek.TUESDAY to "terça-feira",
            DayOfWeek.WEDNESDAY to "quarta-feira",
            DayOfWeek.THURSDAY to "quinta-feira",
            DayOfWeek.FRIDAY to "sexta-feira",
            DayOfWeek.SATURDAY to "sábado",
        )
        return (0 until 7).map { names.getValue(firstDayOfWeek.plus(it.toLong())) }
    }

    /** Tipos de registro presentes em cada dia, para desenhar os marcadores. */
    fun markersByDay(items: List<TimelineItem>): Map<LocalDate, Set<RecordType>> =
        items.groupBy { it.date }.mapValues { (_, list) -> list.map { it.type }.toSortedSet() }
}
