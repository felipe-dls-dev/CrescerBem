package br.com.crescerbem.domain

import br.com.crescerbem.data.model.MeasurementType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação no padrão brasileiro (datas dd/MM/aaaa, vírgula decimal). */
object Formats {
    val locale: Locale = Locale.forLanguageTag("pt-BR")

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
    private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val fileStampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)

    fun date(value: LocalDate): String = value.format(dateFormatter)

    fun shortDate(value: LocalDate): String = value.format(shortDateFormatter)

    /** Ex.: "Terça-feira, 17 de novembro". */
    fun longDate(value: LocalDate): String = value.format(longDateFormatter).capitalizeFirst()

    /** Ex.: "Novembro de 2026". */
    fun monthYear(value: YearMonth): String = value.atDay(1).format(monthYearFormatter).capitalizeFirst()

    fun time(value: LocalTime): String = value.format(timeFormatter)

    fun dateTime(value: LocalDateTime): String = "${date(value.toLocalDate())} ${time(value.toLocalTime())}"

    fun fileStamp(value: LocalDate): String = value.format(fileStampFormatter)

    fun decimal(value: Double, decimals: Int): String {
        val pattern = if (decimals <= 0) "0" else "0." + "0".repeat(decimals)
        return DecimalFormat(pattern, DecimalFormatSymbols(locale)).format(value)
    }

    /** Ex.: "3,450 kg", "52,0 cm". */
    fun measurement(type: MeasurementType, value: Double): String =
        "${decimal(value, type.decimals)} ${type.unit}"

    fun temperature(value: Double): String = "${decimal(value, 1)} °C"

    /** Aceita "3,45" e "3.45". Retorna nulo para texto vazio ou inválido. */
    fun parseDecimal(text: String): Double? =
        text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    fun parseInt(text: String): Int? = text.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    private fun String.capitalizeFirst(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun plural(count: Number, singular: String, plural: String): String =
    "$count ${if (count.toLong() == 1L) singular else plural}"
