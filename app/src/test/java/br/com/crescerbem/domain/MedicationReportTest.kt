package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationReportTest {
    private val baby = Baby(id = 1, name = "Bebê Teste", birthDate = LocalDate.of(2026, 9, 1))
    private val now = LocalDateTime.of(2026, 11, 18, 12, 0)

    private fun dose(
        id: Long,
        hour: Int,
        status: MedicationStatus = MedicationStatus.SCHEDULED,
        babyId: Long = 1,
        givenAt: LocalDateTime? = null,
    ) = MedicationDose(
        id = id,
        babyId = babyId,
        name = "Remédio $id",
        dosage = "5 ml",
        scheduledAt = LocalDateTime.of(2026, 11, 18, hour, 0),
        status = status,
        givenAt = givenAt,
    )

    @Test
    fun `status exibido distingue realizado, nao realizado, nao registrado e agendado`() {
        assertEquals("Realizado", dose(1, 8, MedicationStatus.GIVEN).statusLabel(now))
        assertEquals("Não realizado", dose(2, 9, MedicationStatus.SKIPPED).statusLabel(now))
        assertEquals("Não registrado", dose(3, 10).statusLabel(now))
        assertEquals("Agendado", dose(4, 20).statusLabel(now))
    }

    @Test
    fun `relatorio lista medicamentos do bebe com a situacao de cada dose`() {
        val data = ReportData(
            medications = listOf(
                dose(1, 8, MedicationStatus.GIVEN, givenAt = LocalDateTime.of(2026, 11, 18, 8, 20)),
                dose(2, 10, MedicationStatus.SKIPPED),
                dose(3, 11),
                dose(4, 20),
                dose(5, 9, babyId = 2),
            ),
        )
        val doc = ReportBuilder.build(
            ReportRequest(baby, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), setOf(RecordType.MEDICATION)),
            data,
            now,
        )
        val section = doc.sections.single { it.title == RecordType.MEDICATION.pluralLabel }
        val entries = section.blocks.filterIsInstance<ReportBlock.Entry>()
        assertEquals(4, entries.size)
        val situations = entries.map { e -> e.lines.first { it.startsWith("Situação") } }
        assertEquals(
            listOf(
                "Situação: Realizado",
                "Situação: Não realizado",
                "Situação: Não registrado",
                "Situação: Agendado",
            ),
            situations,
        )
        assertTrue(entries.none { it.heading.contains("Remédio 5") })
        assertTrue(entries.first().lines.any { it.startsWith("Horário previsto") })
    }
}
