package br.com.crescerbem.data.repo

import br.com.crescerbem.data.db.AppDatabase
import br.com.crescerbem.data.db.FoodTried
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import br.com.crescerbem.domain.VaccineSchedule
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Criar, ler, editar e excluir os registros do diário. Fotos são tratadas junto. */
class RecordRepository(
    private val db: AppDatabase,
    private val photos: PhotoRepository,
    /** Chamado após mudar consultas ou vacinas, para reagendar os lembretes. */
    private val onRemindersChanged: suspend () -> Unit = {},
) {
    // Crescimento
    fun observeGrowth(babyId: Long): Flow<List<GrowthMeasurement>> = db.growthDao().observeAll(babyId)
    suspend fun getGrowth(id: Long): GrowthMeasurement? = db.growthDao().getById(id)
    suspend fun saveGrowth(item: GrowthMeasurement): Long =
        if (item.id == 0L) db.growthDao().insert(item) else item.id.also { db.growthDao().update(item) }

    // Vacinas
    fun observeVaccines(babyId: Long): Flow<List<Vaccine>> = db.vaccineDao().observeAll(babyId)
    suspend fun getVaccine(id: Long): Vaccine? = db.vaccineDao().getById(id)
    suspend fun saveVaccine(item: Vaccine): Long =
        (if (item.id == 0L) db.vaccineDao().insert(item) else item.id.also { db.vaccineDao().update(item) })
            .also { onRemindersChanged() }

    /** Adiciona as vacinas do calendário sugerido que ainda não existem. Retorna quantas entraram. */
    suspend fun addSuggestedVaccines(baby: Baby): Int {
        val missing = VaccineSchedule.missingFor(baby, db.vaccineDao().getAll(baby.id))
        if (missing.isNotEmpty()) {
            db.vaccineDao().insertAll(missing)
            onRemindersChanged()
        }
        return missing.size
    }

    // Consultas
    fun observeAppointments(babyId: Long): Flow<List<Appointment>> = db.appointmentDao().observeAll(babyId)
    fun observeUpcomingAppointments(babyId: Long, from: LocalDateTime, limit: Int = 5): Flow<List<Appointment>> =
        db.appointmentDao().observeUpcoming(babyId, from, limit)
    suspend fun getAppointment(id: Long): Appointment? = db.appointmentDao().getById(id)
    suspend fun saveAppointment(item: Appointment): Long =
        (if (item.id == 0L) db.appointmentDao().insert(item) else item.id.also { db.appointmentDao().update(item) })
            .also { onRemindersChanged() }

    // Medicamentos
    fun observeMedications(babyId: Long): Flow<List<MedicationDose>> = db.medicationDao().observeAll(babyId)
    suspend fun getMedication(id: Long): MedicationDose? = db.medicationDao().getById(id)
    suspend fun saveMedication(item: MedicationDose): Long =
        if (item.id == 0L) db.medicationDao().insert(item) else item.id.also { db.medicationDao().update(item) }

    /** Agenda várias doses de uma vez (ex.: de 8 em 8 horas). Retorna o id da primeira. */
    suspend fun saveMedicationDoses(items: List<MedicationDose>): List<Long> = db.medicationDao().insertAll(items)

    /** Doses da mesma série, ainda agendadas, marcadas para depois de [dose]. */
    suspend fun followingInSeries(dose: MedicationDose): List<MedicationDose> =
        dose.seriesId?.let { db.medicationDao().getFollowing(it, dose.id, dose.scheduledAt) }.orEmpty()

    /**
     * Repete em [following] a edição feita em [original] → [updated]: nome, dose, observações e o
     * deslocamento de horário (se a dose mudou 30 min, as próximas também mudam 30 min).
     */
    suspend fun applyEditToFollowing(original: MedicationDose, updated: MedicationDose): Int {
        val following = followingInSeries(original)
        val shift = Duration.between(original.scheduledAt, updated.scheduledAt)
        db.medicationDao().updateAll(
            following.map {
                it.copy(
                    name = updated.name,
                    dosage = updated.dosage,
                    notes = updated.notes,
                    scheduledAt = it.scheduledAt.plus(shift),
                )
            },
        )
        return following.size
    }

    suspend fun deleteFollowingInSeries(dose: MedicationDose): Int {
        val following = followingInSeries(dose)
        if (following.isNotEmpty()) db.medicationDao().deleteByIds(following.map { it.id })
        return following.size
    }

    // Sintomas e mal-estar
    fun observeHealthEvents(babyId: Long): Flow<List<HealthEvent>> = db.healthEventDao().observeAll(babyId)
    suspend fun getHealthEvent(id: Long): HealthEvent? = db.healthEventDao().getById(id)
    suspend fun saveHealthEvent(item: HealthEvent): Long =
        if (item.id == 0L) db.healthEventDao().insert(item) else item.id.also { db.healthEventDao().update(item) }

    // Alimentação
    fun observeFeedingsOn(babyId: Long, day: LocalDate): Flow<List<Feeding>> =
        db.feedingDao().observeBetween(babyId, day.atStartOfDay(), day.plusDays(1).atStartOfDay())
    fun observeFoodsTried(babyId: Long): Flow<List<FoodTried>> = db.feedingDao().observeFoodsTried(babyId)
    suspend fun getFeeding(id: Long): Feeding? = db.feedingDao().getById(id)
    suspend fun saveFeeding(item: Feeding): Long =
        if (item.id == 0L) db.feedingDao().insert(item) else item.id.also { db.feedingDao().update(item) }

    // Momentos
    fun observeMoments(babyId: Long): Flow<List<Moment>> = db.momentDao().observeAll(babyId)
    suspend fun getMoment(id: Long): Moment? = db.momentDao().getById(id)
    suspend fun saveMoment(item: Moment): Long =
        if (item.id == 0L) db.momentDao().insert(item) else item.id.also { db.momentDao().update(item) }

    /** Exclui o registro e as fotos vinculadas a ele (linhas e arquivos). */
    suspend fun delete(type: RecordType, id: Long) {
        photos.deleteFor(type, id)
        when (type) {
            RecordType.GROWTH -> db.growthDao().deleteById(id)
            RecordType.VACCINE -> db.vaccineDao().deleteById(id)
            RecordType.APPOINTMENT -> db.appointmentDao().deleteById(id)
            RecordType.HEALTH_EVENT -> db.healthEventDao().deleteById(id)
            RecordType.FEEDING -> db.feedingDao().deleteById(id)
            RecordType.MOMENT -> db.momentDao().deleteById(id)
            RecordType.MEDICATION -> db.medicationDao().deleteById(id)
            RecordType.BABY -> error("Use BabyRepository.delete para excluir um bebê.")
        }
        if (type == RecordType.VACCINE || type == RecordType.APPOINTMENT) onRemindersChanged()
    }
}
