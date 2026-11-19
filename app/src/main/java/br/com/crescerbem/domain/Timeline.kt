package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.data.model.Vaccine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Item da linha do tempo: forma comum para listar qualquer registro no calendário e em Hoje. */
data class TimelineItem(
    val type: RecordType,
    val id: Long,
    val date: LocalDate,
    val time: LocalTime?,
    val title: String,
    val detail: String? = null,
) {
    val sortKey: LocalDateTime get() = date.atTime(time ?: LocalTime.MIN)
}

fun List<TimelineItem>.sortedChronologically(): List<TimelineItem> =
    sortedWith(compareBy<TimelineItem> { it.sortKey }.thenBy { it.type.ordinal }.thenBy { it.id })

fun GrowthMeasurement.toTimelineItem() = TimelineItem(
    type = RecordType.GROWTH,
    id = id,
    date = date,
    time = null,
    title = "${type.label}: ${Formats.measurement(type, value)}",
    detail = listOfNotNull(place?.takeIf { it.isNotBlank() }, notes?.takeIf { it.isNotBlank() })
        .joinToString(" · ").ifBlank { null },
)

fun Vaccine.toTimelineItem(today: LocalDate): TimelineItem? {
    val date = referenceDate ?: return null
    return TimelineItem(
        type = RecordType.VACCINE,
        id = id,
        date = date,
        time = null,
        title = "Vacina: ${title()}",
        detail = displayStatus(today).label,
    )
}

fun Appointment.toTimelineItem() = TimelineItem(
    type = RecordType.APPOINTMENT,
    id = id,
    date = dateTime.toLocalDate(),
    time = dateTime.toLocalTime(),
    title = title,
    detail = listOfNotNull(
        status.label.takeIf { status != AppointmentStatus.SCHEDULED },
        professional?.takeIf { it.isNotBlank() },
        place?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { null },
)

/** Horário que vale para a dose: o da administração, se já foi dada; senão o agendado. */
val MedicationDose.referenceTime: LocalDateTime
    get() = if (status == MedicationStatus.GIVEN) givenAt ?: scheduledAt else scheduledAt

/** Dose agendada cujo horário já passou sem ser confirmada. */
fun MedicationDose.isOverdue(now: LocalDateTime): Boolean =
    status == MedicationStatus.SCHEDULED && scheduledAt.isBefore(now)

/** Rótulo exibido: dose agendada que já passou sem marcação vira "Não registrado". */
fun MedicationDose.statusLabel(now: LocalDateTime): String =
    if (isOverdue(now)) "Não registrado" else status.label

fun MedicationDose.toTimelineItem(now: LocalDateTime = LocalDateTime.now()): TimelineItem {
    val at = referenceTime
    return TimelineItem(
        type = RecordType.MEDICATION,
        id = id,
        date = at.toLocalDate(),
        time = at.toLocalTime(),
        title = listOfNotNull(name, dosage?.takeIf { it.isNotBlank() }).joinToString(" · "),
        detail = statusLabel(now),
    )
}

fun HealthEvent.symptomsText(): String {
    val names = symptoms.filter { it != Symptom.OTHER }.map { it.label }.toMutableList()
    if (Symptom.OTHER in symptoms || !otherSymptom.isNullOrBlank()) {
        names += otherSymptom?.takeIf { it.isNotBlank() } ?: Symptom.OTHER.label
    }
    return names.joinToString(", ").ifBlank { "Mal-estar" }
}

fun HealthEvent.toTimelineItem() = TimelineItem(
    type = RecordType.HEALTH_EVENT,
    id = id,
    date = dateTime.toLocalDate(),
    time = dateTime.toLocalTime(),
    title = symptomsText(),
    detail = listOfNotNull(
        "Intensidade ${intensity.label.lowercase()}",
        temperatureCelsius?.let { Formats.temperature(it) },
    ).joinToString(" · "),
)

fun Feeding.describe(): String {
    val parts = mutableListOf<String>()
    when (type) {
        FeedingType.BREAST_MILK -> {
            breastSide?.let { parts += "lado ${it.label.lowercase()}" }
            durationMinutes?.let { parts += "$it min" }
            amountMl?.let { parts += "$it ml" }
        }
        FeedingType.FORMULA, FeedingType.WATER -> amountMl?.let { parts += "$it ml" }
        FeedingType.FRUIT, FeedingType.SOLID_MEAL, FeedingType.OTHER -> {
            food?.takeIf { it.isNotBlank() }?.let { parts += it.trim() }
            amountMl?.let { parts += "$it ml" }
            acceptance?.let { parts += it.label.lowercase() }
        }
    }
    if (firstTime) parts += "primeira vez"
    return parts.joinToString(" · ")
}

fun Feeding.toTimelineItem() = TimelineItem(
    type = RecordType.FEEDING,
    id = id,
    date = dateTime.toLocalDate(),
    time = dateTime.toLocalTime(),
    title = type.label,
    detail = describe().ifBlank { null },
)

fun Moment.toTimelineItem() = TimelineItem(
    type = RecordType.MOMENT,
    id = id,
    date = dateTime.toLocalDate(),
    time = dateTime.toLocalTime(),
    title = title,
    detail = description?.takeIf { it.isNotBlank() },
)
