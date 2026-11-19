package br.com.crescerbem.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.crescerbem.data.model.Appointment
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.model.Feeding
import br.com.crescerbem.data.model.GrowthMeasurement
import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.MedicationDose
import br.com.crescerbem.data.model.Moment
import br.com.crescerbem.data.model.Photo
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Vaccine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

// Convenção dos intervalos:
// - colunas de data (LocalDate): BETWEEN :start AND :end (inclusivo);
// - colunas de data e hora (LocalDateTime): >= :start AND < :endExclusive.

@Dao
interface BabyDao {
    @Query("SELECT * FROM babies ORDER BY birthDate DESC, id ASC")
    fun observeAll(): Flow<List<Baby>>

    @Query("SELECT * FROM babies ORDER BY birthDate DESC, id ASC")
    suspend fun getAll(): List<Baby>

    @Query("SELECT * FROM babies WHERE id = :id")
    suspend fun getById(id: Long): Baby?

    @Insert
    suspend fun insert(baby: Baby): Long

    @Update
    suspend fun update(baby: Baby)

    @Delete
    suspend fun delete(baby: Baby)
}

@Dao
interface GrowthDao {
    @Query("SELECT * FROM growth_measurements WHERE babyId = :babyId ORDER BY date DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<GrowthMeasurement>>

    @Query(
        "SELECT * FROM growth_measurements WHERE babyId = :babyId AND date BETWEEN :start AND :end " +
            "ORDER BY date ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDate, end: LocalDate): Flow<List<GrowthMeasurement>>

    @Query(
        "SELECT * FROM growth_measurements WHERE babyId = :babyId AND date BETWEEN :start AND :end " +
            "ORDER BY date ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDate, end: LocalDate): List<GrowthMeasurement>

    @Query("SELECT * FROM growth_measurements WHERE id = :id")
    suspend fun getById(id: Long): GrowthMeasurement?

    @Insert
    suspend fun insert(item: GrowthMeasurement): Long

    @Update
    suspend fun update(item: GrowthMeasurement)

    @Query("DELETE FROM growth_measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface VaccineDao {
    @Query(
        "SELECT * FROM vaccines WHERE babyId = :babyId " +
            "ORDER BY recommendedAgeMonths IS NULL, recommendedAgeMonths ASC, " +
            "COALESCE(appliedDate, scheduledDate) ASC, id ASC",
    )
    fun observeAll(babyId: Long): Flow<List<Vaccine>>

    @Query(
        "SELECT * FROM vaccines WHERE babyId = :babyId " +
            "AND COALESCE(appliedDate, scheduledDate) BETWEEN :start AND :end " +
            "ORDER BY COALESCE(appliedDate, scheduledDate) ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDate, end: LocalDate): Flow<List<Vaccine>>

    @Query(
        "SELECT * FROM vaccines WHERE babyId = :babyId " +
            "AND COALESCE(appliedDate, scheduledDate) BETWEEN :start AND :end " +
            "ORDER BY COALESCE(appliedDate, scheduledDate) ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDate, end: LocalDate): List<Vaccine>

    @Query("SELECT * FROM vaccines WHERE babyId = :babyId")
    suspend fun getAll(babyId: Long): List<Vaccine>

    /** Vacinas ainda não aplicadas e com data prevista, de todos os bebês (para lembretes). */
    @Query("SELECT * FROM vaccines WHERE status = 'SCHEDULED' AND appliedDate IS NULL AND scheduledDate IS NOT NULL")
    suspend fun getAllScheduled(): List<Vaccine>

    @Query("SELECT * FROM vaccines WHERE id = :id")
    suspend fun getById(id: Long): Vaccine?

    @Insert
    suspend fun insert(item: Vaccine): Long

    @Insert
    suspend fun insertAll(items: List<Vaccine>): List<Long>

    @Update
    suspend fun update(item: Vaccine)

    @Query("DELETE FROM vaccines WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE babyId = :babyId ORDER BY dateTime DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<Appointment>>

    @Query(
        "SELECT * FROM appointments WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): Flow<List<Appointment>>

    @Query(
        "SELECT * FROM appointments WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): List<Appointment>

    @Query(
        "SELECT * FROM appointments WHERE babyId = :babyId AND status = 'SCHEDULED' AND dateTime >= :from " +
            "ORDER BY dateTime ASC LIMIT :limit",
    )
    fun observeUpcoming(babyId: Long, from: LocalDateTime, limit: Int): Flow<List<Appointment>>

    /** Consultas ainda agendadas de todos os bebês (para lembretes). */
    @Query("SELECT * FROM appointments WHERE status = 'SCHEDULED'")
    suspend fun getAllScheduled(): List<Appointment>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: Long): Appointment?

    @Insert
    suspend fun insert(item: Appointment): Long

    @Update
    suspend fun update(item: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HealthEventDao {
    @Query("SELECT * FROM health_events WHERE babyId = :babyId ORDER BY dateTime DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<HealthEvent>>

    @Query(
        "SELECT * FROM health_events WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): Flow<List<HealthEvent>>

    @Query(
        "SELECT * FROM health_events WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): List<HealthEvent>

    @Query("SELECT * FROM health_events WHERE id = :id")
    suspend fun getById(id: Long): HealthEvent?

    @Insert
    suspend fun insert(item: HealthEvent): Long

    @Update
    suspend fun update(item: HealthEvent)

    @Query("DELETE FROM health_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_doses WHERE babyId = :babyId ORDER BY scheduledAt DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<MedicationDose>>

    // Doses administradas contam no horário em que foram dadas; as demais, no horário agendado.
    @Query(
        "SELECT * FROM medication_doses WHERE babyId = :babyId " +
            "AND COALESCE(givenAt, scheduledAt) >= :start AND COALESCE(givenAt, scheduledAt) < :endExclusive " +
            "ORDER BY COALESCE(givenAt, scheduledAt) ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): Flow<List<MedicationDose>>

    @Query(
        "SELECT * FROM medication_doses WHERE babyId = :babyId " +
            "AND COALESCE(givenAt, scheduledAt) >= :start AND COALESCE(givenAt, scheduledAt) < :endExclusive " +
            "ORDER BY COALESCE(givenAt, scheduledAt) ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): List<MedicationDose>

    @Query("SELECT * FROM medication_doses WHERE id = :id")
    suspend fun getById(id: Long): MedicationDose?

    /** Doses da mesma série ainda agendadas e marcadas para depois de [after], sem a de id [excludeId]. */
    @Query(
        "SELECT * FROM medication_doses WHERE seriesId = :seriesId AND id != :excludeId " +
            "AND status = 'SCHEDULED' AND scheduledAt > :after ORDER BY scheduledAt ASC, id ASC",
    )
    suspend fun getFollowing(seriesId: Long, excludeId: Long, after: LocalDateTime): List<MedicationDose>

    @Update
    suspend fun updateAll(items: List<MedicationDose>)

    @Query("DELETE FROM medication_doses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Insert
    suspend fun insert(item: MedicationDose): Long

    @Insert
    suspend fun insertAll(items: List<MedicationDose>): List<Long>

    @Update
    suspend fun update(item: MedicationDose)

    @Query("DELETE FROM medication_doses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** Alimento já oferecido ao bebê e a data da primeira vez. */
data class FoodTried(
    val food: String,
    val firstDateTime: LocalDateTime,
)

@Dao
interface FeedingDao {
    @Query(
        "SELECT * FROM feedings WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): Flow<List<Feeding>>

    @Query(
        "SELECT * FROM feedings WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): List<Feeding>

    @Query(
        "SELECT TRIM(food) AS food, MIN(dateTime) AS firstDateTime FROM feedings " +
            "WHERE babyId = :babyId AND food IS NOT NULL AND TRIM(food) != '' " +
            "GROUP BY LOWER(TRIM(food)) ORDER BY firstDateTime DESC",
    )
    fun observeFoodsTried(babyId: Long): Flow<List<FoodTried>>

    @Query("SELECT * FROM feedings WHERE id = :id")
    suspend fun getById(id: Long): Feeding?

    @Insert
    suspend fun insert(item: Feeding): Long

    @Update
    suspend fun update(item: Feeding)

    @Query("DELETE FROM feedings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments WHERE babyId = :babyId ORDER BY dateTime DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<Moment>>

    @Query(
        "SELECT * FROM moments WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    fun observeBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): Flow<List<Moment>>

    @Query(
        "SELECT * FROM moments WHERE babyId = :babyId AND dateTime >= :start AND dateTime < :endExclusive " +
            "ORDER BY dateTime ASC, id ASC",
    )
    suspend fun getBetween(babyId: Long, start: LocalDateTime, endExclusive: LocalDateTime): List<Moment>

    @Query("SELECT * FROM moments WHERE id = :id")
    suspend fun getById(id: Long): Moment?

    @Insert
    suspend fun insert(item: Moment): Long

    @Update
    suspend fun update(item: Moment)

    @Query("DELETE FROM moments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY addedAt ASC, id ASC")
    fun observeForOwner(ownerType: RecordType, ownerId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY addedAt ASC, id ASC")
    suspend fun getForOwner(ownerType: RecordType, ownerId: Long): List<Photo>

    @Query("SELECT * FROM photos WHERE babyId = :babyId ORDER BY addedAt ASC, id ASC")
    suspend fun getForBaby(babyId: Long): List<Photo>

    @Insert
    suspend fun insert(photo: Photo): Long

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerType: RecordType, ownerId: Long)
}
