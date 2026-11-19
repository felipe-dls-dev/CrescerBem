package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import java.time.LocalDate

data class SuggestedVaccine(val name: String, val dose: String, val ageMonths: Int)

/**
 * Calendário sugerido (referência educativa, editável pelo responsável).
 * Baseado no calendário nacional para os primeiros 15 meses. A caderneta e o pediatra
 * são a referência oficial; ver [Texts.VACCINE_NOTE].
 */
object VaccineSchedule {
    val suggestions: List<SuggestedVaccine> = listOf(
        SuggestedVaccine("BCG", "Dose única", 0),
        SuggestedVaccine("Hepatite B", "Dose ao nascer", 0),
        SuggestedVaccine("Penta (DTP/Hib/Hepatite B)", "1ª dose", 2),
        SuggestedVaccine("VIP (poliomielite inativada)", "1ª dose", 2),
        SuggestedVaccine("Pneumocócica 10-valente", "1ª dose", 2),
        SuggestedVaccine("Rotavírus", "1ª dose", 2),
        SuggestedVaccine("Meningocócica C", "1ª dose", 3),
        SuggestedVaccine("Penta (DTP/Hib/Hepatite B)", "2ª dose", 4),
        SuggestedVaccine("VIP (poliomielite inativada)", "2ª dose", 4),
        SuggestedVaccine("Pneumocócica 10-valente", "2ª dose", 4),
        SuggestedVaccine("Rotavírus", "2ª dose", 4),
        SuggestedVaccine("Meningocócica C", "2ª dose", 5),
        SuggestedVaccine("Penta (DTP/Hib/Hepatite B)", "3ª dose", 6),
        SuggestedVaccine("VIP (poliomielite inativada)", "3ª dose", 6),
        SuggestedVaccine("Febre amarela", "1 dose", 9),
        SuggestedVaccine("Tríplice viral (sarampo, caxumba, rubéola)", "1ª dose", 12),
        SuggestedVaccine("Pneumocócica 10-valente", "Reforço", 12),
        SuggestedVaccine("Meningocócica C", "Reforço", 12),
        SuggestedVaccine("DTP (tríplice bacteriana)", "1º reforço", 15),
        SuggestedVaccine("Hepatite A", "Dose única", 15),
        SuggestedVaccine("Tetra viral (sarampo, caxumba, rubéola, varicela)", "Dose única", 15),
    )

    /** Vacinas previstas para o bebê, com data calculada a partir do nascimento. */
    fun suggestedFor(baby: Baby): List<Vaccine> = suggestions.map {
        Vaccine(
            babyId = baby.id,
            name = it.name,
            dose = it.dose,
            recommendedAgeMonths = it.ageMonths,
            scheduledDate = baby.birthDate.plusMonths(it.ageMonths.toLong()),
        )
    }

    /** Remove sugestões que já existem (mesmo nome e dose), para não duplicar. */
    fun missingFor(baby: Baby, existing: List<Vaccine>): List<Vaccine> {
        val keys = existing.map { key(it.name, it.dose) }.toSet()
        return suggestedFor(baby).filter { key(it.name, it.dose) !in keys }
    }

    fun ageGroupLabel(months: Int?): String = when (months) {
        null -> "Outras vacinas"
        0 -> "Ao nascer"
        1 -> "1 mês"
        else -> "$months meses"
    }

    private fun key(name: String, dose: String?) = name.trim().lowercase() + "|" + dose.orEmpty().trim().lowercase()
}

enum class VaccineDisplayStatus(val label: String) {
    APPLIED("Aplicada"),
    PLANNED("Prevista"),
    PENDING("Pendente"),
    OVERDUE("Atrasada"),
    NOT_APPLICABLE("Não se aplica"),
}

/** Dias depois da data prevista a partir dos quais a vacina aparece como atrasada. */
const val VACCINE_OVERDUE_AFTER_DAYS = 30L

/** Data usada no calendário: a da aplicação ou, se ainda não aplicada, a prevista. */
val Vaccine.referenceDate: LocalDate?
    get() = appliedDate ?: scheduledDate

fun Vaccine.displayStatus(today: LocalDate): VaccineDisplayStatus = when (status) {
    VaccineStatus.APPLIED -> VaccineDisplayStatus.APPLIED
    VaccineStatus.NOT_APPLICABLE -> VaccineDisplayStatus.NOT_APPLICABLE
    VaccineStatus.SCHEDULED -> {
        val date = scheduledDate
        when {
            date == null || date.isAfter(today) -> VaccineDisplayStatus.PLANNED
            date.isBefore(today.minusDays(VACCINE_OVERDUE_AFTER_DAYS)) -> VaccineDisplayStatus.OVERDUE
            else -> VaccineDisplayStatus.PENDING
        }
    }
}

fun Vaccine.title(): String = if (dose.isNullOrBlank()) name else "$name · $dose"
