package br.com.crescerbem.domain

import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.Intensity
import br.com.crescerbem.data.model.Symptom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AttentionSignsTest {
    private val birth = LocalDate.of(2026, 1, 1)

    private fun event(
        daysOld: Long,
        symptoms: List<Symptom> = emptyList(),
        temperature: Double? = null,
        intensity: Intensity = Intensity.MILD,
    ) = HealthEvent(
        babyId = 1,
        dateTime = LocalDateTime.of(birth.plusDays(daysOld), java.time.LocalTime.NOON),
        symptoms = symptoms,
        temperatureCelsius = temperature,
        intensity = intensity,
    )

    @Test
    fun `sem sinais para sintoma leve em bebe maior`() {
        assertTrue(AttentionSigns.evaluate(event(120, listOf(Symptom.RUNNY_NOSE)), birth).isEmpty())
    }

    @Test
    fun `febre antes dos 3 meses e sinal de atencao`() {
        val signs = AttentionSigns.evaluate(event(30, listOf(Symptom.FEVER)), birth)
        assertTrue(signs.any { it.contains("menos de 3 meses") })
    }

    @Test
    fun `temperatura de febre conta mesmo sem marcar o chip`() {
        val signs = AttentionSigns.evaluate(event(30, temperature = 37.9), birth)
        assertTrue(signs.any { it.contains("Febre") })
    }

    @Test
    fun `temperatura alta em qualquer idade`() {
        val signs = AttentionSigns.evaluate(event(200, temperature = 38.5), birth)
        assertEquals(listOf("Temperatura de 38 °C ou mais."), signs)
    }

    @Test
    fun `temperatura baixa`() {
        val signs = AttentionSigns.evaluate(event(10, temperature = 35.0), birth)
        assertTrue(signs.any { it.contains("abaixo de 35,5") })
    }

    @Test
    fun `dificuldade para respirar`() {
        val signs = AttentionSigns.evaluate(event(300, listOf(Symptom.BREATHING_DIFFICULTY)), birth)
        assertEquals(listOf("Dificuldade para respirar."), signs)
    }

    @Test
    fun `vomito e diarreia juntos`() {
        val signs = AttentionSigns.evaluate(event(300, listOf(Symptom.VOMITING, Symptom.DIARRHEA)), birth)
        assertTrue(signs.single().contains("desidratação"))
    }

    @Test
    fun `vomito forte sozinho`() {
        val signs = AttentionSigns.evaluate(event(300, listOf(Symptom.VOMITING), intensity = Intensity.STRONG), birth)
        assertTrue(signs.single().contains("intensos"))
    }

    @Test
    fun `intensidade forte sem outro sinal`() {
        val signs = AttentionSigns.evaluate(event(300, listOf(Symptom.COLIC), intensity = Intensity.STRONG), birth)
        assertEquals(listOf("Mal-estar com intensidade forte."), signs)
    }

    @Test
    fun `recusa alimentar em recem nascido`() {
        val signs = AttentionSigns.evaluate(event(20, listOf(Symptom.FEEDING_REFUSAL)), birth)
        assertTrue(signs.any { it.contains("Recusa") })
        assertTrue(AttentionSigns.evaluate(event(200, listOf(Symptom.FEEDING_REFUSAL)), birth).isEmpty())
    }

    @Test
    fun `mensagens nunca usam linguagem de diagnostico`() {
        val all = listOf(
            event(10, Symptom.entries, temperature = 39.0, intensity = Intensity.STRONG),
            event(10, temperature = 35.0),
        ).flatMap { AttentionSigns.evaluate(it, birth) } + Texts.ATTENTION_ADVICE
        all.forEach { text ->
            listOf("diagnóstico", "grave", "anormal").forEach { word ->
                assertTrue("'$word' em: $text", !text.lowercase().contains(word))
            }
        }
    }
}
