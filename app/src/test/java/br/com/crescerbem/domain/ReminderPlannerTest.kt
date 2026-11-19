package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlannerTest {
    private val now = LocalDateTime.of(2026, 3, 10, 9, 0)

    private fun appointment(id: Long, at: LocalDateTime, status: AppointmentStatus = AppointmentStatus.SCHEDULED) =
        Appointment(id = id, babyId = 1, title = "Consulta de teste", dateTime = at, status = status)

    private fun vaccine(id: Long, date: LocalDate?, status: VaccineStatus = VaccineStatus.SCHEDULED, applied: LocalDate? = null) =
        Vaccine(id = id, babyId = 1, name = "Vacina de teste", scheduledDate = date, status = status, appliedDate = applied)

    @Test
    fun appointmentGetsDayBeforeAndTwoHoursBefore() {
        val plan = ReminderPlanner.plan(listOf(appointment(1, LocalDateTime.of(2026, 3, 15, 14, 0))), emptyList(), now)
        assertEquals(listOf("a1-1d", "a1-2h"), plan.map { it.key })
        assertEquals(LocalDateTime.of(2026, 3, 14, 14, 0), plan[0].triggerAt)
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0), plan[1].triggerAt)
    }

    @Test
    fun pastReminderIsSkippedButFutureOneStays() {
        // Consulta amanhã às 8h: o aviso de 1 dia antes (hoje 8h) já passou, o de 2h (amanhã 6h) ainda não.
        val plan = ReminderPlanner.plan(listOf(appointment(2, LocalDateTime.of(2026, 3, 11, 8, 0))), emptyList(), now)
        assertEquals(listOf("a2-2h"), plan.map { it.key })
    }

    @Test
    fun doneOrCanceledAppointmentsHaveNoReminders() {
        val at = LocalDateTime.of(2026, 3, 20, 10, 0)
        val plan = ReminderPlanner.plan(
            listOf(appointment(3, at, AppointmentStatus.DONE), appointment(4, at, AppointmentStatus.CANCELED)),
            emptyList(),
            now,
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun scheduledVaccineRemindsAt8OnTheDay() {
        val plan = ReminderPlanner.plan(emptyList(), listOf(vaccine(5, LocalDate.of(2026, 3, 12))), now)
        assertEquals(1, plan.size)
        assertEquals("v5-day", plan[0].key)
        assertEquals(LocalDateTime.of(2026, 3, 12, 8, 0), plan[0].triggerAt)
    }

    @Test
    fun appliedUndatedOrNotApplicableVaccinesHaveNoReminders() {
        val day = LocalDate.of(2026, 3, 12)
        val plan = ReminderPlanner.plan(
            emptyList(),
            listOf(
                vaccine(6, day, VaccineStatus.APPLIED, applied = day),
                vaccine(7, null),
                vaccine(8, day, VaccineStatus.NOT_APPLICABLE),
            ),
            now,
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun resultIsSortedByTime() {
        val plan = ReminderPlanner.plan(
            listOf(appointment(9, LocalDateTime.of(2026, 3, 20, 10, 0))),
            listOf(vaccine(10, LocalDate.of(2026, 3, 12))),
            now,
        )
        assertEquals(listOf("v10-day", "a9-1d", "a9-2h"), plan.map { it.key })
    }

    @Test
    fun notificationTextNeverExposesHealthDetails() {
        ReminderKind.entries.forEach {
            val text = ReminderPlanner.text(it).lowercase()
            assertFalse(text.contains("teste"))
            assertTrue(text.isNotBlank())
        }
    }
}
