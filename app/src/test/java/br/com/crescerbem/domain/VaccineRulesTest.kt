package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VaccineRulesTest {
    private val baby = Baby(id = 7, name = "Bebê Teste", birthDate = LocalDate.of(2026, 1, 15))
    private val today = LocalDate.of(2026, 6, 1)

    @Test
    fun `calendario sugerido usa a data de nascimento`() {
        val suggested = VaccineSchedule.suggestedFor(baby)
        assertEquals(VaccineSchedule.suggestions.size, suggested.size)
        assertTrue(suggested.all { it.babyId == 7L && it.status == VaccineStatus.SCHEDULED })
        val bcg = suggested.first { it.name == "BCG" }
        assertEquals(LocalDate.of(2026, 1, 15), bcg.scheduledDate)
        val penta2 = suggested.first { it.name.startsWith("Penta") && it.dose == "2ª dose" }
        assertEquals(LocalDate.of(2026, 5, 15), penta2.scheduledDate)
        assertEquals(4, penta2.recommendedAgeMonths)
    }

    @Test
    fun `sugestoes nao duplicam vacinas existentes`() {
        val existing = listOf(
            Vaccine(babyId = 7, name = "bcg", dose = "dose única"),
            Vaccine(babyId = 7, name = "Hepatite B", dose = "Dose ao nascer"),
        )
        val missing = VaccineSchedule.missingFor(baby, existing)
        assertEquals(VaccineSchedule.suggestions.size - 2, missing.size)
        assertTrue(missing.none { it.name == "BCG" })
    }

    @Test
    fun `status calculado pela data`() {
        fun scheduled(date: LocalDate?) = Vaccine(babyId = 7, name = "X", scheduledDate = date)
        assertEquals(VaccineDisplayStatus.PLANNED, scheduled(today.plusDays(1)).displayStatus(today))
        assertEquals(VaccineDisplayStatus.PLANNED, scheduled(null).displayStatus(today))
        assertEquals(VaccineDisplayStatus.PENDING, scheduled(today).displayStatus(today))
        assertEquals(VaccineDisplayStatus.PENDING, scheduled(today.minusDays(30)).displayStatus(today))
        assertEquals(VaccineDisplayStatus.OVERDUE, scheduled(today.minusDays(31)).displayStatus(today))
    }

    @Test
    fun `aplicada e nao se aplica ignoram a data`() {
        val old = today.minusYears(1)
        assertEquals(
            VaccineDisplayStatus.APPLIED,
            Vaccine(babyId = 7, name = "X", scheduledDate = old, status = VaccineStatus.APPLIED).displayStatus(today),
        )
        assertEquals(
            VaccineDisplayStatus.NOT_APPLICABLE,
            Vaccine(babyId = 7, name = "X", scheduledDate = old, status = VaccineStatus.NOT_APPLICABLE).displayStatus(today),
        )
    }

    @Test
    fun `data de referencia prefere a aplicacao`() {
        val applied = LocalDate.of(2026, 3, 20)
        val v = Vaccine(babyId = 7, name = "X", scheduledDate = LocalDate.of(2026, 3, 15), appliedDate = applied)
        assertEquals(applied, v.referenceDate)
        assertNull(Vaccine(babyId = 7, name = "X").referenceDate)
    }

    @Test
    fun `rotulos dos grupos de idade`() {
        assertEquals("Ao nascer", VaccineSchedule.ageGroupLabel(0))
        assertEquals("1 mês", VaccineSchedule.ageGroupLabel(1))
        assertEquals("12 meses", VaccineSchedule.ageGroupLabel(12))
        assertEquals("Outras vacinas", VaccineSchedule.ageGroupLabel(null))
    }
}
