package br.com.crescerbem.domain

import br.com.crescerbem.data.model.HealthEvent
import br.com.crescerbem.data.model.Intensity
import br.com.crescerbem.data.model.Symptom
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Sinais de atenção exibidos depois de registrar um sintoma.
 *
 * Isto NÃO é triagem nem diagnóstico: só destaca combinações em que vale procurar
 * um profissional, sempre acompanhadas de [Texts.ATTENTION_ADVICE].
 */
object AttentionSigns {
    /** Temperatura axilar a partir da qual se considera febre. */
    const val FEVER_CELSIUS = 37.8
    const val HIGH_TEMPERATURE_CELSIUS = 38.0
    const val LOW_TEMPERATURE_CELSIUS = 35.5
    private const val YOUNG_INFANT_DAYS = 90L

    fun evaluate(event: HealthEvent, birthDate: LocalDate): List<String> {
        val signs = mutableListOf<String>()
        val ageDays = ChronoUnit.DAYS.between(birthDate, event.dateTime.toLocalDate())
        val youngInfant = ageDays in 0 until YOUNG_INFANT_DAYS
        val temperature = event.temperatureCelsius
        val symptoms = event.symptoms.toSet()
        val hasFever = Symptom.FEVER in symptoms || (temperature != null && temperature >= FEVER_CELSIUS)

        if (hasFever && youngInfant) {
            signs += "Febre em bebê com menos de 3 meses."
        }
        if (temperature != null && temperature >= HIGH_TEMPERATURE_CELSIUS) {
            signs += "Temperatura de 38 °C ou mais."
        }
        if (temperature != null && temperature < LOW_TEMPERATURE_CELSIUS) {
            signs += "Temperatura abaixo de 35,5 °C."
        }
        if (Symptom.BREATHING_DIFFICULTY in symptoms) {
            signs += "Dificuldade para respirar."
        }
        val vomiting = Symptom.VOMITING in symptoms
        val diarrhea = Symptom.DIARRHEA in symptoms
        if (vomiting && diarrhea) {
            signs += "Vômito e diarreia juntos podem levar à desidratação."
        } else if ((vomiting || diarrhea) && event.intensity == Intensity.STRONG) {
            signs += "Vômito ou diarreia intensos podem levar à desidratação."
        }
        if (Symptom.FEEDING_REFUSAL in symptoms && youngInfant) {
            signs += "Recusa para mamar em bebê com menos de 3 meses."
        }
        if (event.intensity == Intensity.STRONG && signs.isEmpty()) {
            signs += "Mal-estar com intensidade forte."
        }
        return signs
    }
}
