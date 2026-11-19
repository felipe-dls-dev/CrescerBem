package br.com.crescerbem.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class MedicationScheduleTest {
    private val start = LocalDateTime.of(2026, 11, 18, 8, 0)

    @Test
    fun `8 em 8 horas por 7 dias gera 21 doses e a ultima nao passa do periodo`() {
        val times = MedicationSchedule.times(start, 8, 7)
        assertEquals(21, times.size)
        assertEquals(start, times.first())
        assertEquals(LocalDateTime.of(2026, 11, 25, 0, 0), times.last())
    }

    @Test
    fun `intervalo que nao divide o dia deixa de fora a dose que passaria do limite`() {
        // 5 em 5 horas por 1 dia: 08h, 13h, 18h, 23h e 04h do dia seguinte; 09h já passaria do limite (08h + 24h).
        val times = MedicationSchedule.times(start, 5, 1)
        assertEquals(5, times.size)
        assertEquals(LocalDateTime.of(2026, 11, 19, 4, 0), times.last())
    }

    @Test
    fun `1 dia com intervalo de 24 horas gera uma dose`() {
        assertEquals(listOf(start), MedicationSchedule.times(start, 24, 1))
    }

    @Test
    fun `validacao recusa valores fora dos limites`() {
        assertNull(MedicationSchedule.validate(8, 7))
        assertNotNull(MedicationSchedule.validate(null, 7))
        assertNotNull(MedicationSchedule.validate(0, 7))
        assertNotNull(MedicationSchedule.validate(73, 7))
        assertNotNull(MedicationSchedule.validate(8, 0))
        assertNotNull(MedicationSchedule.validate(8, MedicationSchedule.MAX_DAYS + 1))
        // 1 hora por 30 dias = 720 doses: acima do máximo
        assertNotNull(MedicationSchedule.validate(1, 30))
        // 1 hora por 10 dias = 240 doses: no limite
        assertNull(MedicationSchedule.validate(1, 10))
    }
}
