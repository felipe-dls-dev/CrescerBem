package br.com.crescerbem.domain

import br.com.crescerbem.data.model.MeasurementType

/**
 * Avisos para valores pouco prováveis (ex.: peso digitado em gramas).
 * Nunca bloqueiam o registro: o app pede confirmação e respeita a decisão do responsável.
 */
object Plausibility {
    val weightRangeKg = 0.5..30.0
    val heightRangeCm = 30.0..130.0
    val headRangeCm = 25.0..60.0
    val temperatureRangeCelsius = 34.0..43.0

    fun measurementWarning(type: MeasurementType, value: Double): String? = when (type) {
        MeasurementType.WEIGHT -> if (value !in weightRangeKg) {
            "Peso fora do comum para bebês (entre 0,5 e 30 kg). Confira se o valor está em quilos, por exemplo 3,450."
        } else {
            null
        }
        MeasurementType.HEIGHT -> if (value !in heightRangeCm) {
            "Altura fora do comum para bebês (entre 30 e 130 cm). Confira se o valor está em centímetros."
        } else {
            null
        }
        MeasurementType.HEAD_CIRCUMFERENCE -> if (value !in headRangeCm) {
            "Perímetro cefálico fora do comum (entre 25 e 60 cm). Confira se o valor está em centímetros."
        } else {
            null
        }
    }

    fun temperatureWarning(value: Double): String? =
        if (value !in temperatureRangeCelsius) "Temperatura incomum. Confira o valor digitado." else null
}
