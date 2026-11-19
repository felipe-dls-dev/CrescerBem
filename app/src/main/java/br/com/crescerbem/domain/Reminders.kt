package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderKind { APPOINTMENT_DAY_BEFORE, APPOINTMENT_SOON, VACCINE_TODAY }

/** Um lembrete a disparar. [key] é estável: o mesmo registro e tipo sempre geram a mesma chave. */
data class Reminder(val key: String, val kind: ReminderKind, val triggerAt: LocalDateTime)

/**
 * Decide quais lembretes existem hoje. Lógica pura, sem Android, para ser testada.
 * Consultas: 1 dia antes e 2 horas antes. Vacinas previstas: às 8h do dia marcado.
 */
object ReminderPlanner {
    val VACCINE_HOUR: LocalTime = LocalTime.of(8, 0)

    fun plan(appointments: List<Appointment>, vaccines: List<Vaccine>, now: LocalDateTime): List<Reminder> {
        val result = mutableListOf<Reminder>()
        appointments.filter { it.status == AppointmentStatus.SCHEDULED }.forEach {
            result += Reminder("a${it.id}-1d", ReminderKind.APPOINTMENT_DAY_BEFORE, it.dateTime.minusDays(1))
            result += Reminder("a${it.id}-2h", ReminderKind.APPOINTMENT_SOON, it.dateTime.minus(Duration.ofHours(2)))
        }
        vaccines.filter { it.status == VaccineStatus.SCHEDULED && it.appliedDate == null }.forEach { v ->
            v.scheduledDate?.let {
                result += Reminder("v${v.id}-day", ReminderKind.VACCINE_TODAY, it.atTime(VACCINE_HOUR))
            }
        }
        return result.filter { it.triggerAt.isAfter(now) }.sortedBy { it.triggerAt }
    }

    /**
     * Texto da notificação. Privacidade: não cita nome do bebê, vacina, médico nem motivo,
     * pois a notificação pode aparecer na tela de bloqueio.
     */
    fun text(kind: ReminderKind): String = when (kind) {
        ReminderKind.APPOINTMENT_DAY_BEFORE -> "Você tem uma consulta amanhã."
        ReminderKind.APPOINTMENT_SOON -> "Você tem uma consulta daqui a pouco."
        ReminderKind.VACCINE_TODAY -> "Há uma vacina prevista para hoje."
    }
}
