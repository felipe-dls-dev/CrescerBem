package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReportBuilderTest {
    private val baby = Baby(id = 1, name = "Bebê Teste", birthDate = LocalDate.of(2026, 9, 1))
    private val start = LocalDate.of(2026, 11, 1)
    private val end = LocalDate.of(2026, 11, 30)
    private val now = LocalDateTime.of(2026, 12, 1, 10, 0)

    private val data = ReportData(
        measurements = listOf(
            GrowthMeasurement(id = 1, babyId = 1, date = LocalDate.of(2026, 11, 5), type = MeasurementType.WEIGHT, value = 4.2),
            GrowthMeasurement(id = 2, babyId = 1, date = LocalDate.of(2026, 11, 20), type = MeasurementType.WEIGHT, value = 4.6),
            // Fora do período e de outro bebê: não podem aparecer.
            GrowthMeasurement(id = 3, babyId = 1, date = LocalDate.of(2026, 10, 31), type = MeasurementType.WEIGHT, value = 4.0),
            GrowthMeasurement(id = 4, babyId = 2, date = LocalDate.of(2026, 11, 10), type = MeasurementType.WEIGHT, value = 9.9),
        ),
        vaccines = listOf(
            Vaccine(
                id = 10,
                babyId = 1,
                name = "Rotavírus",
                dose = "1ª dose",
                scheduledDate = LocalDate.of(2026, 11, 1),
                appliedDate = LocalDate.of(2026, 11, 3),
                status = VaccineStatus.APPLIED,
                lot = "L123",
            ),
            Vaccine(id = 11, babyId = 1, name = "Meningocócica C", scheduledDate = LocalDate.of(2026, 12, 1)),
        ),
        appointments = listOf(
            Appointment(id = 20, babyId = 1, title = "Pediatra", dateTime = LocalDateTime.of(2026, 11, 30, 23, 30), guidance = "Manter aleitamento"),
            Appointment(id = 21, babyId = 1, title = "Retorno", dateTime = LocalDateTime.of(2026, 12, 1, 0, 0)),
        ),
        healthEvents = listOf(
            HealthEvent(id = 30, babyId = 1, dateTime = LocalDateTime.of(2026, 11, 12, 3, 0), symptoms = listOf(Symptom.COLIC)),
        ),
        feedings = listOf(
            Feeding(id = 40, babyId = 1, dateTime = LocalDateTime.of(2026, 11, 12, 6, 0), type = FeedingType.BREAST_MILK, durationMinutes = 15),
            Feeding(id = 41, babyId = 1, dateTime = LocalDateTime.of(2026, 11, 12, 9, 0), type = FeedingType.FORMULA, amountMl = 90),
        ),
        moments = listOf(
            Moment(id = 50, babyId = 1, dateTime = LocalDateTime.of(2026, 11, 2, 15, 0), title = "Primeiro sorriso"),
        ),
        photos = mapOf(RecordKey(RecordType.MOMENT, 50) to listOf("foto-teste.jpg")),
    )

    private fun request(types: Set<RecordType> = ReportBuilder.reportableTypes.toSet(), photos: Boolean = false) =
        ReportRequest(baby, start, end, types, photos)

    private fun ReportDocument.allText(): String = buildString {
        appendLine(title)
        headerLines.forEach(::appendLine)
        sections.forEach { section ->
            appendLine(section.title)
            section.blocks.forEach { block ->
                when (block) {
                    is ReportBlock.Paragraph -> appendLine(block.text)
                    is ReportBlock.Table -> (listOf(block.headers) + block.rows).forEach { appendLine(it.joinToString(" | ")) }
                    is ReportBlock.Entry -> {
                        appendLine(block.heading)
                        block.lines.forEach(::appendLine)
                    }
                }
            }
        }
    }

    @Test
    fun `filtra por bebe e periodo inclusive nas bordas`() {
        val filtered = ReportBuilder.filter(request(), data)
        assertEquals(listOf(1L, 2L), filtered.measurements.map { it.id })
        assertEquals(listOf(10L), filtered.vaccines.map { it.id })
        // 30/11 às 23:30 entra; 01/12 à meia-noite fica de fora.
        assertEquals(listOf(20L), filtered.appointments.map { it.id })
        assertEquals(1, filtered.healthEvents.size)
        assertEquals(2, filtered.feedings.size)
        assertEquals(1, filtered.moments.size)
    }

    @Test
    fun `documento contem cabecalho, resumo e rodape de aviso`() {
        val doc = ReportBuilder.build(request(), data, now)
        assertEquals(Texts.REPORT_FOOTER, doc.footer)
        assertTrue(doc.headerLines.contains("Período: 01/11/2026 a 30/11/2026"))
        assertTrue(doc.headerLines.contains("Bebê: Bebê Teste"))
        assertEquals("Resumo do período", doc.sections.first().title)
        val summary = doc.sections.first().blocks.single() as ReportBlock.Table
        assertTrue(summary.rows.contains(listOf("Crescimento", "2")))
        assertTrue(summary.rows.contains(listOf("Vacinas", "1")))
    }

    @Test
    fun `nada fora do periodo aparece no texto`() {
        val text = ReportBuilder.build(request(), data, now).allText()
        assertTrue(text.contains("4,200 kg"))
        assertTrue(text.contains("4,600 kg"))
        assertFalse(text.contains("4,000 kg"))
        assertFalse(text.contains("9,900 kg"))
        assertFalse(text.contains("Retorno"))
        assertFalse(text.contains("Meningocócica"))
        assertTrue(text.contains("Lote L123"))
        assertTrue(text.contains("Orientações recebidas: Manter aleitamento"))
        assertTrue(text.contains("Último registro de peso no período: 4,600 kg em 20/11/2026."))
    }

    @Test
    fun `so inclui as secoes escolhidas`() {
        val doc = ReportBuilder.build(request(setOf(RecordType.VACCINE, RecordType.MOMENT)), data, now)
        assertEquals(listOf("Resumo do período", "Vacinas", "Momentos"), doc.sections.map { it.title })
    }

    @Test
    fun `secao vazia explica que nao ha registros`() {
        val empty = ReportBuilder.build(request(setOf(RecordType.HEALTH_EVENT)), ReportData(), now)
        val section = empty.sections.last()
        assertEquals((section.blocks.single() as ReportBlock.Paragraph).text, Texts.EMPTY_PERIOD)
    }

    @Test
    fun `fotos so entram quando pedidas`() {
        fun momentPhotos(doc: ReportDocument) = doc.sections.first { it.title == "Momentos" }
            .blocks.filterIsInstance<ReportBlock.Entry>().flatMap { it.photos }
        assertTrue(momentPhotos(ReportBuilder.build(request(photos = false), data, now)).isEmpty())
        assertEquals(listOf("foto-teste.jpg"), momentPhotos(ReportBuilder.build(request(photos = true), data, now)))
    }

    @Test
    fun `alimentacao resumida por dia`() {
        val doc = ReportBuilder.build(request(setOf(RecordType.FEEDING)), data, now)
        val table = doc.sections.last().blocks.first() as ReportBlock.Table
        assertEquals(listOf("12/11/2026", "1 (15)", "90", "-", "-", "-"), table.rows.single())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `periodo invertido e rejeitado`() {
        ReportRequest(baby, end, start, setOf(RecordType.GROWTH))
    }
}
