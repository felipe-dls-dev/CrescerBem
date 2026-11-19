package br.com.crescerbem.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Todas as tabelas de registro apontam para o bebê com exclusão em cascata:
// apagar o bebê apaga tudo que pertence a ele (inclusive as linhas de foto).

@Entity(tableName = "babies")
data class Baby(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthDate: LocalDate,
    val birthTime: LocalTime? = null,
    val sex: Sex? = null,
    val bloodType: String? = null,
    val notes: String? = null,
    /** Índice na paleta BabyPalette (0 = verde-menta padrão). */
    val colorIndex: Int = 0,
)

@Entity(
    tableName = "growth_measurements",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "date"])],
)
data class GrowthMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val date: LocalDate,
    val type: MeasurementType,
    /** Peso em kg; altura e perímetro cefálico em cm. */
    val value: Double,
    val place: String? = null,
    val notes: String? = null,
)

@Entity(
    tableName = "vaccines",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId"])],
)
data class Vaccine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val name: String,
    val dose: String? = null,
    /** Idade recomendada em meses, usada para agrupar. Nulo para vacinas avulsas. */
    val recommendedAgeMonths: Int? = null,
    val scheduledDate: LocalDate? = null,
    val appliedDate: LocalDate? = null,
    val status: VaccineStatus = VaccineStatus.SCHEDULED,
    val lot: String? = null,
    val place: String? = null,
    val reaction: String? = null,
    val notes: String? = null,
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "dateTime"])],
)
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val professional: String? = null,
    val place: String? = null,
    val reason: String? = null,
    val guidance: String? = null,
    val notes: String? = null,
)

@Entity(
    tableName = "health_events",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "dateTime"])],
)
data class HealthEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val dateTime: LocalDateTime,
    val symptoms: List<Symptom>,
    val otherSymptom: String? = null,
    val intensity: Intensity = Intensity.MILD,
    val temperatureCelsius: Double? = null,
    val duration: String? = null,
    val actionsTaken: String? = null,
    val soughtCare: Boolean = false,
    val notes: String? = null,
)

@Entity(
    tableName = "feedings",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "dateTime"])],
)
data class Feeding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val dateTime: LocalDateTime,
    val type: FeedingType,
    val breastSide: BreastSide? = null,
    val durationMinutes: Int? = null,
    val amountMl: Int? = null,
    val food: String? = null,
    val texture: FoodTexture? = null,
    val acceptance: Acceptance? = null,
    /** Primeira vez que o bebê experimenta este alimento. */
    val firstTime: Boolean = false,
    val reaction: String? = null,
    val notes: String? = null,
)

/** Uma dose de medicamento (cada horário agendado ou administrado é uma linha). */
@Entity(
    tableName = "medication_doses",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "scheduledAt"])],
)
data class MedicationDose(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val name: String,
    /** Quantidade em texto livre, como na receita (ex.: "5 ml", "10 gotas"). */
    val dosage: String? = null,
    val scheduledAt: LocalDateTime,
    val status: MedicationStatus = MedicationStatus.SCHEDULED,
    /** Hora em que foi realmente administrada; só faz sentido com status GIVEN. */
    val givenAt: LocalDateTime? = null,
    val notes: String? = null,
    /** Doses criadas juntas por uma repetição ("de 8 em 8 horas por 7 dias") compartilham este valor. */
    val seriesId: Long? = null,
)

@Entity(
    tableName = "moments",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId", "dateTime"])],
)
data class Moment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val dateTime: LocalDateTime,
    val title: String,
    val description: String? = null,
)

/**
 * Foto anexada a qualquer registro. O arquivo fica em files/photos/[fileName].
 * O vínculo é polimórfico ([ownerType] + [ownerId]); a limpeza é feita pelo repositório.
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Baby::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["babyId"]), Index(value = ["ownerType", "ownerId"])],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val ownerType: RecordType,
    val ownerId: Long,
    val fileName: String,
    val addedAt: LocalDateTime,
    val caption: String? = null,
)
