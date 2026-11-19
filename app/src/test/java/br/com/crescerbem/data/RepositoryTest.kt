package br.com.crescerbem.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.data.repo.BabyRepository
import br.com.crescerbem.data.repo.DataEraser
import br.com.crescerbem.data.repo.InMemoryActiveBabyStore
import br.com.crescerbem.data.repo.PhotoFiles
import br.com.crescerbem.data.repo.PhotoRepository
import br.com.crescerbem.data.repo.RecordRepository
import br.com.crescerbem.data.repo.TimelineRepository
import br.com.crescerbem.domain.RecordKey
import br.com.crescerbem.domain.VaccineSchedule
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

/** Registra quais arquivos foram apagados, no lugar do disco. */
private class FakePhotoFiles : PhotoFiles {
    val deleted = mutableListOf<String>()
    var deletedAll = false
    override fun delete(fileName: String) {
        deleted += fileName
    }

    override fun deleteAll() {
        deletedAll = true
    }
}

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var files: FakePhotoFiles
    private lateinit var photos: PhotoRepository
    private lateinit var active: InMemoryActiveBabyStore
    private lateinit var babies: BabyRepository
    private lateinit var records: RecordRepository
    private lateinit var timeline: TimelineRepository
    private val day = LocalDate.of(2026, 11, 17)
    private val fixedNow = LocalDateTime.of(2026, 11, 17, 12, 0)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        files = FakePhotoFiles()
        photos = PhotoRepository(db.photoDao(), files) { fixedNow }
        active = InMemoryActiveBabyStore()
        babies = BabyRepository(db, photos, active)
        records = RecordRepository(db, photos)
        timeline = TimelineRepository(db, photos) { day }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun baby(): Baby {
        val id = babies.save(Baby(name = "Bebê Teste", birthDate = LocalDate.of(2026, 9, 1)))
        return babies.get(id)!!
    }

    @Test
    fun `bebe novo vira o ativo e o primeiro e usado quando nada foi escolhido`() = runTest {
        assertNull(babies.activeBaby.first())
        val first = baby()
        assertEquals(first.id, active.activeBabyId.value)
        val second = babies.save(Baby(name = "Gêmeo Teste", birthDate = first.birthDate))
        assertEquals(second, babies.activeBaby.first()!!.id)
        active.setActive(null)
        assertEquals(first.birthDate, babies.activeBaby.first()!!.birthDate)
    }

    @Test
    fun `cor de bebe novo e a primeira livre e a escolha explicita e respeitada`() = runTest {
        assertEquals(0, babies.nextColorIndex(8))
        val first = baby()
        assertEquals(1, babies.nextColorIndex(8))
        val second = babies.save(Baby(name = "Segundo Teste", birthDate = first.birthDate, colorIndex = 3))
        assertEquals(3, babies.get(second)!!.colorIndex)
        assertEquals(1, babies.nextColorIndex(8))
        val third = babies.save(Baby(name = "Terceiro Teste", birthDate = first.birthDate, colorIndex = 0))
        assertEquals(0, babies.get(third)!!.colorIndex)
    }

    @Test
    fun `doses de medicamento aparecem no calendario no horario dado ou agendado`() = runTest {
        val b = baby()
        val agendada = MedicationDose(babyId = b.id, name = "Remédio A", dosage = "5 ml", scheduledAt = day.atTime(20, 0))
        val dada = MedicationDose(
            babyId = b.id,
            name = "Remédio B",
            scheduledAt = day.atTime(8, 0),
            status = MedicationStatus.GIVEN,
            givenAt = day.plusDays(1).atTime(0, 10),
        )
        records.saveMedicationDoses(listOf(agendada, dada))

        val hoje = timeline.observeRange(b.id, day, day).first()
        assertEquals(listOf("Remédio A · 5 ml"), hoje.map { it.title })
        assertEquals(RecordType.MEDICATION, hoje.single().type)

        val amanha = timeline.observeRange(b.id, day.plusDays(1), day.plusDays(1)).first()
        assertEquals(listOf("Remédio B"), amanha.map { it.title })

        val id = records.observeMedications(b.id).first().first().id
        records.delete(RecordType.MEDICATION, id)
        assertEquals(1, records.observeMedications(b.id).first().size)
    }

    private suspend fun seedSeries(babyId: Long): List<MedicationDose> {
        val doses = (0 until 4).map { i ->
            MedicationDose(
                babyId = babyId,
                name = "Série",
                dosage = "5 ml",
                scheduledAt = day.atTime(8, 0).plusHours(8L * i),
                seriesId = 99,
            )
        }
        records.saveMedicationDoses(doses)
        return records.observeMedications(babyId).first().sortedBy { it.scheduledAt }
    }

    @Test
    fun `editar horario da dose aplica o mesmo deslocamento so as proximas agendadas`() = runTest {
        val b = baby()
        val all = seedSeries(b.id)
        // A 3ª dose já foi realizada: não deve ser alterada.
        records.saveMedication(all[2].copy(status = MedicationStatus.GIVEN, givenAt = all[2].scheduledAt))

        val edited = all[1].copy(name = "Novo nome", scheduledAt = all[1].scheduledAt.plusMinutes(30))
        val changed = records.applyEditToFollowing(all[1], edited)
        records.saveMedication(edited)

        assertEquals(1, changed) // só a 4ª (a 3ª está realizada)
        val after = records.observeMedications(b.id).first().sortedBy { it.id }
        assertEquals(all[0].scheduledAt, after[0].scheduledAt) // anterior intacta
        assertEquals("Série", after[0].name)
        assertEquals(all[2].scheduledAt, after[2].scheduledAt) // realizada intacta
        assertEquals("Série", after[2].name)
        assertEquals(all[3].scheduledAt.plusMinutes(30), after[3].scheduledAt)
        assertEquals("Novo nome", after[3].name)
    }

    @Test
    fun `excluir a serie remove so as proximas agendadas`() = runTest {
        val b = baby()
        val all = seedSeries(b.id)
        val removed = records.deleteFollowingInSeries(all[1])
        assertEquals(2, removed)
        assertEquals(listOf(all[0].id, all[1].id), records.observeMedications(b.id).first().map { it.id }.sorted())
    }

    @Test
    fun `sincronizar fotos insere novas e apaga removidas`() = runTest {
        val b = baby()
        val momentId = records.saveMoment(Moment(babyId = b.id, dateTime = fixedNow, title = "Banho"))
        photos.sync(b.id, RecordType.MOMENT, momentId, listOf("a.jpg", "b.jpg"))
        assertEquals(listOf("a.jpg", "b.jpg"), photos.fileNamesFor(RecordType.MOMENT, momentId))

        photos.sync(b.id, RecordType.MOMENT, momentId, listOf("b.jpg", "c.jpg"))
        assertEquals(listOf("b.jpg", "c.jpg"), photos.fileNamesFor(RecordType.MOMENT, momentId).sorted())
        assertEquals(listOf("a.jpg"), files.deleted)
    }

    @Test
    fun `excluir registro apaga as fotos dele e mantem as de outros`() = runTest {
        val b = baby()
        val m1 = records.saveMoment(Moment(babyId = b.id, dateTime = fixedNow, title = "Um"))
        val m2 = records.saveMoment(Moment(babyId = b.id, dateTime = fixedNow, title = "Dois"))
        photos.sync(b.id, RecordType.MOMENT, m1, listOf("um.jpg"))
        photos.sync(b.id, RecordType.MOMENT, m2, listOf("dois.jpg"))

        records.delete(RecordType.MOMENT, m1)

        assertNull(records.getMoment(m1))
        assertTrue(photos.fileNamesFor(RecordType.MOMENT, m1).isEmpty())
        assertEquals(listOf("um.jpg"), files.deleted)
        assertEquals(listOf("dois.jpg"), photos.fileNamesFor(RecordType.MOMENT, m2))
    }

    @Test
    fun `excluir bebe apaga arquivos e limpa o ativo`() = runTest {
        val b = baby()
        val m = records.saveMoment(Moment(babyId = b.id, dateTime = fixedNow, title = "Um"))
        photos.sync(b.id, RecordType.MOMENT, m, listOf("um.jpg"))
        photos.sync(b.id, RecordType.BABY, b.id, listOf("perfil.jpg"))

        babies.delete(b)

        assertEquals(setOf("um.jpg", "perfil.jpg"), files.deleted.toSet())
        assertNull(active.activeBabyId.value)
        assertNull(babies.get(b.id))
        assertNull(records.getMoment(m))
    }

    @Test
    fun `calendario sugerido nao duplica ao adicionar de novo`() = runTest {
        val b = baby()
        assertEquals(VaccineSchedule.suggestions.size, records.addSuggestedVaccines(b))
        assertEquals(0, records.addSuggestedVaccines(b))
        assertEquals(VaccineSchedule.suggestions.size, records.observeVaccines(b.id).first().size)
    }

    @Test
    fun `linha do tempo junta tipos em ordem cronologica`() = runTest {
        val b = baby()
        records.saveGrowth(GrowthMeasurement(babyId = b.id, date = day, type = MeasurementType.WEIGHT, value = 4.4))
        records.saveFeeding(Feeding(babyId = b.id, dateTime = day.atTime(9, 0), type = FeedingType.FORMULA, amountMl = 90))
        records.saveMoment(Moment(babyId = b.id, dateTime = day.atTime(8, 0), title = "Sorriso"))
        records.saveVaccine(Vaccine(babyId = b.id, name = "BCG", scheduledDate = day))
        records.saveMoment(Moment(babyId = b.id, dateTime = day.plusDays(1).atTime(8, 0), title = "Amanhã"))

        val items = timeline.observeRange(b.id, day, day).first()
        assertEquals(4, items.size)
        assertEquals(RecordType.MOMENT, items[2].type)
        assertEquals(RecordType.FEEDING, items[3].type)
        assertTrue(items.none { it.title == "Amanhã" })
    }

    @Test
    fun `dados do relatorio trazem fotos so quando pedidas`() = runTest {
        val b = baby()
        val m = records.saveMoment(Moment(babyId = b.id, dateTime = day.atTime(8, 0), title = "Sorriso"))
        photos.sync(b.id, RecordType.MOMENT, m, listOf("sorriso.jpg"))
        photos.sync(b.id, RecordType.BABY, b.id, listOf("perfil.jpg"))

        assertTrue(timeline.loadReportData(b.id, day, day, includePhotos = false).photos.isEmpty())
        val withPhotos = timeline.loadReportData(b.id, day, day, includePhotos = true)
        assertEquals(listOf("sorriso.jpg"), withPhotos.photos[RecordKey(RecordType.MOMENT, m)])
        // Foto de perfil não entra no relatório.
        assertTrue(withPhotos.photos.keys.none { it.type == RecordType.BABY })
    }

    @Test
    fun `apagar tudo limpa banco arquivos e ativo`() = runTest {
        val b = baby()
        records.saveMoment(Moment(babyId = b.id, dateTime = fixedNow, title = "Um"))
        var reportsDeleted = false
        DataEraser(db, photos, active) { reportsDeleted = true }.eraseEverything()

        assertTrue(babies.babies.first().isEmpty())
        assertTrue(files.deletedAll)
        assertTrue(reportsDeleted)
        assertNull(active.activeBabyId.value)
    }
}
