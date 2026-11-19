package br.com.crescerbem.domain

import br.com.crescerbem.data.model.Acceptance
import br.com.crescerbem.data.model.AppointmentStatus
import br.com.crescerbem.data.model.BreastSide
import br.com.crescerbem.data.model.FeedingType
import br.com.crescerbem.data.model.FoodTexture
import br.com.crescerbem.data.model.Intensity
import br.com.crescerbem.data.model.MeasurementType
import br.com.crescerbem.data.model.MedicationStatus
import br.com.crescerbem.data.model.RecordType
import br.com.crescerbem.data.model.Sex
import br.com.crescerbem.data.model.Symptom

// Rótulos em português usados pela interface e pelo relatório em PDF.

val RecordType.label: String
    get() = when (this) {
        RecordType.BABY -> "Bebê"
        RecordType.MOMENT -> "Momento"
        RecordType.VACCINE -> "Vacina"
        RecordType.APPOINTMENT -> "Consulta"
        RecordType.HEALTH_EVENT -> "Sintoma"
        RecordType.GROWTH -> "Medida"
        RecordType.FEEDING -> "Alimentação"
        RecordType.MEDICATION -> "Medicamento"
    }

val MedicationStatus.label: String
    get() = when (this) {
        MedicationStatus.SCHEDULED -> "Agendado"
        MedicationStatus.GIVEN -> "Realizado"
        MedicationStatus.SKIPPED -> "Não realizado"
    }

val RecordType.pluralLabel: String
    get() = when (this) {
        RecordType.BABY -> "Bebês"
        RecordType.MOMENT -> "Momentos"
        RecordType.VACCINE -> "Vacinas"
        RecordType.APPOINTMENT -> "Consultas"
        RecordType.HEALTH_EVENT -> "Sintomas e mal-estar"
        RecordType.GROWTH -> "Crescimento"
        RecordType.FEEDING -> "Alimentação"
        RecordType.MEDICATION -> "Medicamentos"
    }

val MeasurementType.label: String
    get() = when (this) {
        MeasurementType.WEIGHT -> "Peso"
        MeasurementType.HEIGHT -> "Altura"
        MeasurementType.HEAD_CIRCUMFERENCE -> "Perímetro cefálico"
    }

val MeasurementType.unit: String
    get() = when (this) {
        MeasurementType.WEIGHT -> "kg"
        MeasurementType.HEIGHT, MeasurementType.HEAD_CIRCUMFERENCE -> "cm"
    }

/** Casas decimais exibidas: peso com 3 (gramas), comprimentos com 1. */
val MeasurementType.decimals: Int
    get() = when (this) {
        MeasurementType.WEIGHT -> 3
        MeasurementType.HEIGHT, MeasurementType.HEAD_CIRCUMFERENCE -> 1
    }

val Sex.label: String
    get() = when (this) {
        Sex.FEMALE -> "Feminino"
        Sex.MALE -> "Masculino"
    }

val AppointmentStatus.label: String
    get() = when (this) {
        AppointmentStatus.SCHEDULED -> "Agendada"
        AppointmentStatus.DONE -> "Realizada"
        AppointmentStatus.CANCELED -> "Cancelada"
    }

val Intensity.label: String
    get() = when (this) {
        Intensity.MILD -> "Leve"
        Intensity.MODERATE -> "Moderada"
        Intensity.STRONG -> "Forte"
    }

val Symptom.label: String
    get() = when (this) {
        Symptom.FEVER -> "Febre"
        Symptom.COUGH -> "Tosse"
        Symptom.RUNNY_NOSE -> "Coriza"
        Symptom.VOMITING -> "Vômito"
        Symptom.DIARRHEA -> "Diarreia"
        Symptom.COLIC -> "Cólica"
        Symptom.IRRITABILITY -> "Irritabilidade"
        Symptom.SLEEP_CHANGES -> "Sono alterado"
        Symptom.SKIN_RASH -> "Pele (assadura, manchas)"
        Symptom.FEEDING_REFUSAL -> "Recusa para mamar ou comer"
        Symptom.BREATHING_DIFFICULTY -> "Dificuldade para respirar"
        Symptom.OTHER -> "Outro"
    }

val FeedingType.label: String
    get() = when (this) {
        FeedingType.BREAST_MILK -> "Aleitamento materno"
        FeedingType.FORMULA -> "Fórmula"
        FeedingType.FRUIT -> "Fruta"
        FeedingType.SOLID_MEAL -> "Papa salgada / refeição"
        FeedingType.WATER -> "Água"
        FeedingType.OTHER -> "Outro"
    }

val FeedingType.isSolid: Boolean
    get() = this == FeedingType.FRUIT || this == FeedingType.SOLID_MEAL

val BreastSide.label: String
    get() = when (this) {
        BreastSide.LEFT -> "Esquerdo"
        BreastSide.RIGHT -> "Direito"
        BreastSide.BOTH -> "Os dois"
    }

val FoodTexture.label: String
    get() = when (this) {
        FoodTexture.PUREE -> "Papa / purê"
        FoodTexture.MASHED -> "Amassado"
        FoodTexture.PIECES -> "Em pedaços"
    }

val Acceptance.label: String
    get() = when (this) {
        Acceptance.GOOD -> "Aceitou bem"
        Acceptance.LITTLE -> "Aceitou pouco"
        Acceptance.REFUSED -> "Recusou"
    }

fun Boolean.yesNo(): String = if (this) "Sim" else "Não"
