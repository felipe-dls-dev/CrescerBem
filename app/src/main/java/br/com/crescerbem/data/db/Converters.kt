package br.com.crescerbem.data.db

import androidx.room.TypeConverter
import br.com.crescerbem.data.model.Symptom
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Datas são gravadas como números para permitir consultas por intervalo:
 * - LocalDate: dia epoch;
 * - LocalDateTime: segundos epoch tratando a hora local como UTC (sem conversão de fuso,
 *   o que evita registros "mudando de dia" quando o aparelho troca de fuso);
 * - LocalTime: segundos do dia.
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? = value?.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): Int? = value?.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(value: Int?): LocalTime? = value?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter
    fun fromSymptoms(value: List<Symptom>?): String? = value?.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun toSymptoms(value: String?): List<Symptom>? = value
        ?.split(SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.mapNotNull { name -> Symptom.entries.firstOrNull { it.name == name } }

    private companion object {
        const val SEPARATOR = ","
    }
}
