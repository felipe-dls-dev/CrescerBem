package br.com.crescerbem.data.model

/** Tipos de registro do diário. Também identifica o "dono" de uma foto. */
enum class RecordType { BABY, MOMENT, VACCINE, APPOINTMENT, HEALTH_EVENT, GROWTH, FEEDING, MEDICATION }

/** Dose de medicamento: agendada para depois, já administrada ou deixada de dar. */
enum class MedicationStatus { SCHEDULED, GIVEN, SKIPPED }

enum class Sex { FEMALE, MALE }

enum class MeasurementType { WEIGHT, HEIGHT, HEAD_CIRCUMFERENCE }

/** Status gravado. "Pendente" e "atrasada" são derivados da data (ver domain/VaccineRules). */
enum class VaccineStatus { SCHEDULED, APPLIED, NOT_APPLICABLE }

enum class AppointmentStatus { SCHEDULED, DONE, CANCELED }

enum class Intensity { MILD, MODERATE, STRONG }

enum class Symptom {
    FEVER,
    COUGH,
    RUNNY_NOSE,
    VOMITING,
    DIARRHEA,
    COLIC,
    IRRITABILITY,
    SLEEP_CHANGES,
    SKIN_RASH,
    FEEDING_REFUSAL,
    BREATHING_DIFFICULTY,
    OTHER,
}

enum class FeedingType { BREAST_MILK, FORMULA, FRUIT, SOLID_MEAL, WATER, OTHER }

enum class BreastSide { LEFT, RIGHT, BOTH }

enum class FoodTexture { PUREE, MASHED, PIECES }

enum class Acceptance { GOOD, LITTLE, REFUSED }
