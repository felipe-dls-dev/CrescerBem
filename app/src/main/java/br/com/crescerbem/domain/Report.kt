package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import java.time.LocalDate
import java.time.LocalDateTime

/** Pedido do responsável na tela de relatórios. */
data class ReportRequest(
    val baby: Baby,
    val start: LocalDate,
    val end: LocalDate,
    val types: Set<RecordType>,
    val includePhotos: Boolean = false,
) {
    init {
        require(!end.isBefore(start)) { "O fim do período não pode ser antes do início." }
    }
}

data class RecordKey(val type: RecordType, val id: Long)

/** Registros carregados do banco para montar o relatório. */
data class ReportData(
    val measurements: List<GrowthMeasurement> = emptyList(),
    val vaccines: List<Vaccine> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val healthEvents: List<HealthEvent> = emptyList(),
    val feedings: List<Feeding> = emptyList(),
    val moments: List<Moment> = emptyList(),
    val medications: List<MedicationDose> = emptyList(),
    /** Nomes dos arquivos de foto por registro. */
    val photos: Map<RecordKey, List<String>> = emptyMap(),
)

/** Documento independente de formato; o PDF é só uma forma de desenhá-lo. */
data class ReportDocument(
    val title: String,
    val headerLines: List<String>,
    val sections: List<ReportSection>,
    val footer: String,
)

data class ReportSection(val title: String, val blocks: List<ReportBlock>)

sealed interface ReportBlock {
    data class Paragraph(val text: String) : ReportBlock

    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        /** Peso relativo da largura de cada coluna. */
        val weights: List<Float>,
    ) : ReportBlock

    data class Entry(
        val heading: String,
        val lines: List<String>,
        val photos: List<String> = emptyList(),
    ) : ReportBlock
}

/** Quantidade de registros de cada tipo, usada na pré-visualização e no resumo. */
fun ReportData.countFor(type: RecordType): Int = when (type) {
    RecordType.GROWTH -> measurements.size
    RecordType.VACCINE -> vaccines.size
    RecordType.APPOINTMENT -> appointments.size
    RecordType.HEALTH_EVENT -> healthEvents.size
    RecordType.FEEDING -> feedings.size
    RecordType.MOMENT -> moments.size
    RecordType.MEDICATION -> medications.size
    RecordType.BABY -> 0
}

object ReportBuilder {
    /** Ordem das seções no relatório. */
    val reportableTypes: List<RecordType> = listOf(
        RecordType.GROWTH,
        RecordType.VACCINE,
        RecordType.APPOINTMENT,
        RecordType.HEALTH_EVENT,
        RecordType.MEDICATION,
        RecordType.FEEDING,
        RecordType.MOMENT,
    )

    /**
     * Monta o documento. Filtra de novo por bebê e período, para garantir que nada
     * fora do pedido entre no PDF mesmo que a consulta tenha trazido a mais.
     */
    fun build(request: ReportRequest, data: ReportData, generatedAt: LocalDateTime): ReportDocument {
        val filtered = filter(request, data)
        val baby = request.baby
        val header = listOf(
            "Bebê: ${baby.name}",
            "Nascimento: ${Formats.date(baby.birthDate)}",
            "Idade no fim do período: ${BabyAge.describe(baby.birthDate, request.end)}",
            "Período: ${Formats.date(request.start)} a ${Formats.date(request.end)}",
            "Gerado em: ${Formats.dateTime(generatedAt)}",
        )
        val selected = reportableTypes.filter { it in request.types }
        val sections = buildList {
            add(summarySection(selected, filtered))
            selected.forEach { type ->
                add(
                    when (type) {
                        RecordType.GROWTH -> growthSection(filtered, request.includePhotos)
                        RecordType.VACCINE -> vaccineSection(filtered, request.end, request.includePhotos)
                        RecordType.APPOINTMENT -> appointmentSection(filtered, request.includePhotos)
                        RecordType.HEALTH_EVENT -> healthSection(filtered, request.includePhotos)
                        RecordType.FEEDING -> feedingSection(filtered, request.includePhotos)
                        RecordType.MOMENT -> momentSection(filtered, request.includePhotos)
                        RecordType.MEDICATION -> medicationSection(filtered, generatedAt)
                        RecordType.BABY -> error("Tipo não reportável")
                    },
                )
            }
        }
        return ReportDocument(
            title = "Relatório de acompanhamento",
            headerLines = header,
            sections = sections,
            footer = Texts.REPORT_FOOTER,
        )
    }

    /** Mantém apenas registros do bebê, do período e dos tipos pedidos. */
    fun filter(request: ReportRequest, data: ReportData): ReportData {
        val babyId = request.baby.id
        fun inPeriod(date: LocalDate) = !date.isBefore(request.start) && !date.isAfter(request.end)
        fun wants(type: RecordType) = type in request.types
        return ReportData(
            measurements = if (wants(RecordType.GROWTH)) {
                data.measurements.filter { it.babyId == babyId && inPeriod(it.date) }.sortedBy { it.date }
            } else {
                emptyList()
            },
            vaccines = if (wants(RecordType.VACCINE)) {
                data.vaccines
                    .filter { v -> v.babyId == babyId && v.referenceDate?.let(::inPeriod) == true }
                    .sortedBy { it.referenceDate }
            } else {
                emptyList()
            },
            appointments = if (wants(RecordType.APPOINTMENT)) {
                data.appointments.filter { it.babyId == babyId && inPeriod(it.dateTime.toLocalDate()) }
                    .sortedBy { it.dateTime }
            } else {
                emptyList()
            },
            healthEvents = if (wants(RecordType.HEALTH_EVENT)) {
                data.healthEvents.filter { it.babyId == babyId && inPeriod(it.dateTime.toLocalDate()) }
                    .sortedBy { it.dateTime }
            } else {
                emptyList()
            },
            feedings = if (wants(RecordType.FEEDING)) {
                data.feedings.filter { it.babyId == babyId && inPeriod(it.dateTime.toLocalDate()) }
                    .sortedBy { it.dateTime }
            } else {
                emptyList()
            },
            moments = if (wants(RecordType.MOMENT)) {
                data.moments.filter { it.babyId == babyId && inPeriod(it.dateTime.toLocalDate()) }
                    .sortedBy { it.dateTime }
            } else {
                emptyList()
            },
            medications = if (wants(RecordType.MEDICATION)) {
                data.medications.filter { it.babyId == babyId && inPeriod(it.referenceTime.toLocalDate()) }
                    .sortedBy { it.referenceTime }
            } else {
                emptyList()
            },
            photos = if (request.includePhotos) data.photos else emptyMap(),
        )
    }

    private fun medicationSection(data: ReportData, generatedAt: LocalDateTime): ReportSection {
        val items = data.medications
        if (items.isEmpty()) return emptySection(RecordType.MEDICATION)
        return ReportSection(
            RecordType.MEDICATION.pluralLabel,
            items.map { m ->
                ReportBlock.Entry(
                    heading = "${Formats.dateTime(m.referenceTime)} · ${m.name}",
                    lines = listOfNotNull(
                        "Situação: ${m.statusLabel(generatedAt)}",
                        m.dosage.line("Dose"),
                        if (m.status == MedicationStatus.GIVEN && m.givenAt != null && m.givenAt != m.scheduledAt) {
                            "Horário previsto: ${Formats.dateTime(m.scheduledAt)}"
                        } else {
                            null
                        },
                        m.notes.line("Observações"),
                    ),
                )
            },
        )
    }

    private fun summarySection(types: List<RecordType>, data: ReportData) = ReportSection(
        title = "Resumo do período",
        blocks = listOf(
            ReportBlock.Table(
                headers = listOf("Tipo de registro", "Quantidade"),
                rows = types.map { listOf(it.pluralLabel, data.countFor(it).toString()) },
                weights = listOf(3f, 1f),
            ),
        ),
    )

    private fun photosFor(data: ReportData, type: RecordType, id: Long, include: Boolean): List<String> =
        if (include) data.photos[RecordKey(type, id)].orEmpty() else emptyList()

    private fun photoEntries(
        data: ReportData,
        type: RecordType,
        include: Boolean,
        items: List<Pair<Long, String>>,
    ): List<ReportBlock> = if (!include) {
        emptyList()
    } else {
        items.mapNotNull { (id, heading) ->
            val photos = photosFor(data, type, id, true)
            if (photos.isEmpty()) null else ReportBlock.Entry("Fotos: $heading", emptyList(), photos)
        }
    }

    private fun growthSection(data: ReportData, includePhotos: Boolean): ReportSection {
        val items = data.measurements
        if (items.isEmpty()) return emptySection(RecordType.GROWTH)
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Table(
            headers = listOf("Data", "Medida", "Valor", "Local / observação"),
            rows = items.map { m ->
                listOf(
                    Formats.date(m.date),
                    m.type.label,
                    Formats.measurement(m.type, m.value),
                    listOfNotNull(m.place, m.notes).filter { it.isNotBlank() }.joinToString(" · "),
                )
            },
            weights = listOf(1.2f, 1.5f, 1.2f, 2.6f),
        )
        MeasurementType.entries.forEach { type ->
            items.filter { it.type == type }.maxByOrNull { it.date }?.let { last ->
                blocks += ReportBlock.Paragraph(
                    "Último registro de ${type.label.lowercase()} no período: " +
                        "${Formats.measurement(type, last.value)} em ${Formats.date(last.date)}.",
                )
            }
        }
        blocks += photoEntries(
            data,
            RecordType.GROWTH,
            includePhotos,
            items.map { it.id to "${it.type.label} em ${Formats.date(it.date)}" },
        )
        return ReportSection(RecordType.GROWTH.pluralLabel, blocks)
    }

    private fun vaccineSection(data: ReportData, end: LocalDate, includePhotos: Boolean): ReportSection {
        val items = data.vaccines
        if (items.isEmpty()) return emptySection(RecordType.VACCINE)
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Table(
            headers = listOf("Vacina", "Dose", "Situação", "Data", "Lote / local / reação"),
            rows = items.map { v ->
                listOf(
                    v.name,
                    v.dose.orEmpty(),
                    v.displayStatus(end).label,
                    v.referenceDate?.let(Formats::date).orEmpty(),
                    listOfNotNull(
                        v.lot?.takeIf { it.isNotBlank() }?.let { "Lote $it" },
                        v.place,
                        v.reaction?.takeIf { it.isNotBlank() }?.let { "Reação: $it" },
                        v.notes,
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                )
            },
            weights = listOf(2.4f, 1.1f, 1.1f, 1.1f, 2.4f),
        )
        blocks += ReportBlock.Paragraph(Texts.VACCINE_NOTE)
        blocks += photoEntries(
            data,
            RecordType.VACCINE,
            includePhotos,
            items.map { it.id to it.title() },
        )
        return ReportSection(RecordType.VACCINE.pluralLabel, blocks)
    }

    private fun appointmentSection(data: ReportData, includePhotos: Boolean): ReportSection {
        val items = data.appointments
        if (items.isEmpty()) return emptySection(RecordType.APPOINTMENT)
        return ReportSection(
            RecordType.APPOINTMENT.pluralLabel,
            items.map { a ->
                ReportBlock.Entry(
                    heading = "${Formats.dateTime(a.dateTime)} · ${a.title}",
                    lines = listOfNotNull(
                        "Situação: ${a.status.label}",
                        a.professional.line("Profissional"),
                        a.place.line("Local"),
                        a.reason.line("Motivo"),
                        a.guidance.line("Orientações recebidas"),
                        a.notes.line("Observações"),
                    ),
                    photos = photosFor(data, RecordType.APPOINTMENT, a.id, includePhotos),
                )
            },
        )
    }

    private fun healthSection(data: ReportData, includePhotos: Boolean): ReportSection {
        val items = data.healthEvents
        if (items.isEmpty()) return emptySection(RecordType.HEALTH_EVENT)
        return ReportSection(
            RecordType.HEALTH_EVENT.pluralLabel,
            items.map { e ->
                ReportBlock.Entry(
                    heading = "${Formats.dateTime(e.dateTime)} · ${e.symptomsText()}",
                    lines = listOfNotNull(
                        "Intensidade: ${e.intensity.label}",
                        e.temperatureCelsius?.let { "Temperatura: ${Formats.temperature(it)}" },
                        e.duration.line("Duração"),
                        e.actionsTaken.line("O que foi feito"),
                        "Procurou atendimento: ${e.soughtCare.yesNo()}",
                        e.notes.line("Observações"),
                    ),
                    photos = photosFor(data, RecordType.HEALTH_EVENT, e.id, includePhotos),
                )
            },
        )
    }

    private fun feedingSection(data: ReportData, includePhotos: Boolean): ReportSection {
        val items = data.feedings
        if (items.isEmpty()) return emptySection(RecordType.FEEDING)
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Table(
            headers = listOf("Dia", "Mamadas (min)", "Fórmula (ml)", "Água (ml)", "Refeições", "Alimentos novos"),
            rows = FeedingSummaries.byDay(items).map { (day, s) ->
                listOf(
                    Formats.date(day),
                    if (s.breastfeedings == 0) "-" else "${s.breastfeedings} (${s.breastMinutes})",
                    if (s.formulaCount == 0) "-" else s.formulaMl.toString(),
                    if (s.waterMl == 0) "-" else s.waterMl.toString(),
                    if (s.solidMeals == 0) "-" else s.solidMeals.toString(),
                    s.newFoods.joinToString(", ").ifBlank { "-" },
                )
            },
            weights = listOf(1.2f, 1.3f, 1.1f, 1f, 1f, 2f),
        )
        val solids = items.filter { it.type.isSolid }
        if (solids.isNotEmpty()) {
            blocks += ReportBlock.Paragraph("Introdução alimentar")
            blocks += ReportBlock.Table(
                headers = listOf("Data", "Alimento", "Forma", "Aceitação", "Reação / observação"),
                rows = solids.map { f ->
                    listOf(
                        Formats.dateTime(f.dateTime),
                        (f.food?.trim().orEmpty().ifBlank { f.type.label }) + if (f.firstTime) " (1ª vez)" else "",
                        f.texture?.label.orEmpty(),
                        f.acceptance?.label.orEmpty(),
                        listOfNotNull(f.reaction, f.notes).filter { it.isNotBlank() }.joinToString(" · "),
                    )
                },
                weights = listOf(1.5f, 1.6f, 1.1f, 1.1f, 2f),
            )
        }
        blocks += photoEntries(
            data,
            RecordType.FEEDING,
            includePhotos,
            items.map { it.id to "${it.type.label} em ${Formats.dateTime(it.dateTime)}" },
        )
        return ReportSection(RecordType.FEEDING.pluralLabel, blocks)
    }

    private fun momentSection(data: ReportData, includePhotos: Boolean): ReportSection {
        val items = data.moments
        if (items.isEmpty()) return emptySection(RecordType.MOMENT)
        return ReportSection(
            RecordType.MOMENT.pluralLabel,
            items.map { m ->
                ReportBlock.Entry(
                    heading = "${Formats.dateTime(m.dateTime)} · ${m.title}",
                    lines = listOfNotNull(m.description?.takeIf { it.isNotBlank() }),
                    photos = photosFor(data, RecordType.MOMENT, m.id, includePhotos),
                )
            },
        )
    }

    private fun emptySection(type: RecordType) =
        ReportSection(type.pluralLabel, listOf(ReportBlock.Paragraph(Texts.EMPTY_PERIOD)))

    private fun String?.line(label: String): String? = this?.takeIf { it.isNotBlank() }?.let { "$label: ${it.trim()}" }
}
