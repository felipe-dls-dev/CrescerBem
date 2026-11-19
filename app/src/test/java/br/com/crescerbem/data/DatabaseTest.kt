package br.com.crescerbem.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.Intensity
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.Photo
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Sex
import br.com.crescerbem.data.model.Symptom
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.model.VaccineStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Dados fictícios apenas ("Bebê Teste"); nenhum dado real. */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private val day = LocalDate.of(2026, 11, 17)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newBaby(name: String = "Bebê Teste"): Long =
        db.babyDao().insert(Baby(name = name, birthDate = LocalDate.of(2026, 9, 1)))

    @Test
    fun `bebe cria le edita e exclui`() = runTest {
        val id = db.babyDao().insert(
            Baby(name = "Bebê Teste", birthDate = LocalDate.of(2026, 9, 1), birthTime = LocalTime.of(7, 45), sex = Sex.FEMALE),
        )
        val saved = db.babyDao().getById(id)!!
        assertEquals(LocalTime.of(7, 45), saved.birthTime)
        assertEquals(Sex.FEMALE, saved.sex)

        db.babyDao().update(saved.copy(name = "Bebê Teste 2", bloodType = "O+"))
        assertEquals("Bebê Teste 2", db.babyDao().getById(id)!!.name)
        assertEquals(1, db.babyDao().observeAll().first().size)

        db.babyDao().delete(db.babyDao().getById(id)!!)
        assertNull(db.babyDao().getById(id))
    }

    @Test
    fun `cada tipo de registro cria le edita e exclui`() = runTest {
        val babyId = newBaby()
        val at = day.atTime(10, 0)

        val growthId = db.growthDao().insert(GrowthMeasurement(babyId = babyId, date = day, type = MeasurementType.WEIGHT, value = 4.5))
        db.growthDao().update(db.growthDao().getById(growthId)!!.copy(value = 4.6))
        assertEquals(4.6, db.growthDao().getById(growthId)!!.value, 0.0001)
        db.growthDao().deleteById(growthId)
        assertNull(db.growthDao().getById(growthId))

        val vaccineId = db.vaccineDao().insert(Vaccine(babyId = babyId, name = "BCG", scheduledDate = day))
        db.vaccineDao().update(db.vaccineDao().getById(vaccineId)!!.copy(status = VaccineStatus.APPLIED, appliedDate = day))
        assertEquals(VaccineStatus.APPLIED, db.vaccineDao().getById(vaccineId)!!.status)
        db.vaccineDao().deleteById(vaccineId)
        assertNull(db.vaccineDao().getById(vaccineId))

        val appointmentId = db.appointmentDao().insert(Appointment(babyId = babyId, title = "Pediatra", dateTime = at))
        db.appointmentDao().update(db.appointmentDao().getById(appointmentId)!!.copy(status = AppointmentStatus.DONE))
        assertEquals(AppointmentStatus.DONE, db.appointmentDao().getById(appointmentId)!!.status)
        db.appointmentDao().deleteById(appointmentId)
        assertNull(db.appointmentDao().getById(appointmentId))

        val eventId = db.healthEventDao().insert(
            HealthEvent(babyId = babyId, dateTime = at, symptoms = listOf(Symptom.FEVER, Symptom.COUGH), temperatureCelsius = 38.1),
        )
        val event = db.healthEventDao().getById(eventId)!!
        assertEquals(listOf(Symptom.FEVER, Symptom.COUGH), event.symptoms)
        db.healthEventDao().update(event.copy(intensity = Intensity.STRONG))
        assertEquals(Intensity.STRONG, db.healthEventDao().getById(eventId)!!.intensity)
        db.healthEventDao().deleteById(eventId)
        assertNull(db.healthEventDao().getById(eventId))

        val feedingId = db.feedingDao().insert(Feeding(babyId = babyId, dateTime = at, type = FeedingType.FORMULA, amountMl = 90))
        db.feedingDao().update(db.feedingDao().getById(feedingId)!!.copy(amountMl = 120))
        assertEquals(120, db.feedingDao().getById(feedingId)!!.amountMl)
        db.feedingDao().deleteById(feedingId)
        assertNull(db.feedingDao().getById(feedingId))

        val momentId = db.momentDao().insert(Moment(babyId = babyId, dateTime = at, title = "Primeiro sorriso"))
        db.momentDao().update(db.momentDao().getById(momentId)!!.copy(description = "Na hora do banho"))
        assertEquals("Na hora do banho", db.momentDao().getById(momentId)!!.description)
        db.momentDao().deleteById(momentId)
        assertNull(db.momentDao().getById(momentId))
    }

    @Test
    fun `excluir bebe apaga em cascata todos os registros e fotos`() = runTest {
        val babyId = newBaby()
        val otherId = newBaby("Outro Bebê Teste")
        val at = day.atTime(8, 0)
        db.growthDao().insert(GrowthMeasurement(babyId = babyId, date = day, type = MeasurementType.HEIGHT, value = 55.0))
        db.vaccineDao().insert(Vaccine(babyId = babyId, name = "BCG", scheduledDate = day))
        db.appointmentDao().insert(Appointment(babyId = babyId, title = "Pediatra", dateTime = at))
        db.healthEventDao().insert(HealthEvent(babyId = babyId, dateTime = at, symptoms = listOf(Symptom.COLIC)))
        db.feedingDao().insert(Feeding(babyId = babyId, dateTime = at, type = FeedingType.WATER, amountMl = 20))
        val momentId = db.momentDao().insert(Moment(babyId = babyId, dateTime = at, title = "Banho"))
        db.photoDao().insert(Photo(babyId = babyId, ownerType = RecordType.MOMENT, ownerId = momentId, fileName = "a.jpg", addedAt = at))
        db.feedingDao().insert(Feeding(babyId = otherId, dateTime = at, type = FeedingType.WATER, amountMl = 10))

        db.babyDao().delete(db.babyDao().getById(babyId)!!)

        val range = day.atStartOfDay() to day.plusDays(1).atStartOfDay()
        assertTrue(db.growthDao().getBetween(babyId, day, day).isEmpty())
        assertTrue(db.vaccineDao().getAll(babyId).isEmpty())
        assertTrue(db.appointmentDao().getBetween(babyId, range.first, range.second).isEmpty())
        assertTrue(db.healthEventDao().getBetween(babyId, range.first, range.second).isEmpty())
        assertTrue(db.feedingDao().getBetween(babyId, range.first, range.second).isEmpty())
        assertTrue(db.momentDao().getBetween(babyId, range.first, range.second).isEmpty())
        assertTrue(db.photoDao().getForBaby(babyId).isEmpty())
        // O outro bebê não é afetado.
        assertEquals(1, db.feedingDao().getBetween(otherId, range.first, range.second).size)
    }

    @Test
    fun `intervalo de data e hora inclui o dia inteiro e exclui o seguinte`() = runTest {
        val babyId = newBaby()
        listOf(
            day.minusDays(1).atTime(23, 59),
            day.atStartOfDay(),
            day.atTime(23, 59),
            day.plusDays(1).atStartOfDay(),
        ).forEach { db.feedingDao().insert(Feeding(babyId = babyId, dateTime = it, type = FeedingType.BREAST_MILK)) }

        val result = db.feedingDao().getBetween(babyId, day.atStartOfDay(), day.plusDays(1).atStartOfDay())
        assertEquals(listOf(day.atStartOfDay(), day.atTime(23, 59)), result.map { it.dateTime })
    }

    @Test
    fun `vacina entra no intervalo pela data de aplicacao ou prevista`() = runTest {
        val babyId = newBaby()
        db.vaccineDao().insert(Vaccine(babyId = babyId, name = "Prevista no dia", scheduledDate = day))
        db.vaccineDao().insert(
            Vaccine(
                babyId = babyId,
                name = "Prevista antes, aplicada no dia",
                scheduledDate = day.minusDays(20),
                appliedDate = day,
                status = VaccineStatus.APPLIED,
            ),
        )
        db.vaccineDao().insert(Vaccine(babyId = babyId, name = "Fora", scheduledDate = day.plusDays(3)))
        db.vaccineDao().insert(Vaccine(babyId = babyId, name = "Sem data", status = VaccineStatus.NOT_APPLICABLE))

        val names = db.vaccineDao().getBetween(babyId, day, day).map { it.name }.toSet()
        assertEquals(setOf("Prevista no dia", "Prevista antes, aplicada no dia"), names)
    }

    @Test
    fun `proximas consultas so traz agendadas a partir da data`() = runTest {
        val babyId = newBaby()
        db.appointmentDao().insert(Appointment(babyId = babyId, title = "Passada", dateTime = day.minusDays(1).atTime(9, 0)))
        db.appointmentDao().insert(Appointment(babyId = babyId, title = "Futura", dateTime = day.plusDays(2).atTime(9, 0)))
        db.appointmentDao().insert(
            Appointment(babyId = babyId, title = "Cancelada", dateTime = day.plusDays(1).atTime(9, 0), status = AppointmentStatus.CANCELED),
        )
        val upcoming = db.appointmentDao().observeUpcoming(babyId, day.atStartOfDay(), 5).first()
        assertEquals(listOf("Futura"), upcoming.map { it.title })
    }

    @Test
    fun `alimentos experimentados agrupados sem diferenciar maiusculas`() = runTest {
        val babyId = newBaby()
        db.feedingDao().insert(Feeding(babyId = babyId, dateTime = LocalDateTime.of(2026, 11, 1, 12, 0), type = FeedingType.FRUIT, food = "Banana"))
        db.feedingDao().insert(Feeding(babyId = babyId, dateTime = LocalDateTime.of(2026, 11, 3, 12, 0), type = FeedingType.FRUIT, food = " banana "))
        db.feedingDao().insert(Feeding(babyId = babyId, dateTime = LocalDateTime.of(2026, 11, 2, 12, 0), type = FeedingType.SOLID_MEAL, food = "Abóbora"))
        db.feedingDao().insert(Feeding(babyId = babyId, dateTime = LocalDateTime.of(2026, 11, 2, 15, 0), type = FeedingType.FORMULA, amountMl = 90))

        val foods = db.feedingDao().observeFoodsTried(babyId).first()
        assertEquals(2, foods.size)
        val banana = foods.first { it.food.equals("banana", ignoreCase = true) }
        assertEquals(LocalDateTime.of(2026, 11, 1, 12, 0), banana.firstDateTime)
    }
}
