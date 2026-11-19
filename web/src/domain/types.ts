// Modelo de dados do Crescer Bem (espelha data/model/Entities.kt e Enums.kt do app Android).
//
// Datas e horas são guardadas como texto ISO sem fuso:
//   LocalDate     "2026-11-19"
//   LocalTime     "14:30"
//   LocalDateTime "2026-11-19T14:30"
// O formato ordena corretamente como texto e não depende do fuso do aparelho.

export type LocalDate = string;
export type LocalTime = string;
export type LocalDateTime = string;

/** Tipos de registro do diário. Também identifica o "dono" de uma foto. A ordem importa (ordinal). */
export const RECORD_TYPES = [
  'BABY',
  'MOMENT',
  'VACCINE',
  'APPOINTMENT',
  'HEALTH_EVENT',
  'GROWTH',
  'FEEDING',
  'MEDICATION',
] as const;
export type RecordType = (typeof RECORD_TYPES)[number];

export const MEDICATION_STATUSES = ['SCHEDULED', 'GIVEN', 'SKIPPED'] as const;
export type MedicationStatus = (typeof MEDICATION_STATUSES)[number];

export const SEXES = ['FEMALE', 'MALE'] as const;
export type Sex = (typeof SEXES)[number];

export const MEASUREMENT_TYPES = ['WEIGHT', 'HEIGHT', 'HEAD_CIRCUMFERENCE'] as const;
export type MeasurementType = (typeof MEASUREMENT_TYPES)[number];

/** Status gravado. "Pendente" e "atrasada" são derivados da data (ver vaccineRules). */
export const VACCINE_STATUSES = ['SCHEDULED', 'APPLIED', 'NOT_APPLICABLE'] as const;
export type VaccineStatus = (typeof VACCINE_STATUSES)[number];

export const APPOINTMENT_STATUSES = ['SCHEDULED', 'DONE', 'CANCELED'] as const;
export type AppointmentStatus = (typeof APPOINTMENT_STATUSES)[number];

export const INTENSITIES = ['MILD', 'MODERATE', 'STRONG'] as const;
export type Intensity = (typeof INTENSITIES)[number];

export const SYMPTOMS = [
  'FEVER',
  'COUGH',
  'RUNNY_NOSE',
  'VOMITING',
  'DIARRHEA',
  'COLIC',
  'IRRITABILITY',
  'SLEEP_CHANGES',
  'SKIN_RASH',
  'FEEDING_REFUSAL',
  'BREATHING_DIFFICULTY',
  'OTHER',
] as const;
export type Symptom = (typeof SYMPTOMS)[number];

export const FEEDING_TYPES = ['BREAST_MILK', 'FORMULA', 'FRUIT', 'SOLID_MEAL', 'WATER', 'OTHER'] as const;
export type FeedingType = (typeof FEEDING_TYPES)[number];

export const BREAST_SIDES = ['LEFT', 'RIGHT', 'BOTH'] as const;
export type BreastSide = (typeof BREAST_SIDES)[number];

export const FOOD_TEXTURES = ['PUREE', 'MASHED', 'PIECES'] as const;
export type FoodTexture = (typeof FOOD_TEXTURES)[number];

export const ACCEPTANCES = ['GOOD', 'LITTLE', 'REFUSED'] as const;
export type Acceptance = (typeof ACCEPTANCES)[number];

/** Posição do tipo na lista (equivale ao ordinal do enum no Kotlin). */
export const recordTypeOrdinal = (type: RecordType): number => RECORD_TYPES.indexOf(type);

// Todas as tabelas de registro apontam para o bebê; apagar o bebê apaga tudo que pertence a ele.
// `id` ausente = registro novo (o IndexedDB atribui o número ao salvar).

export interface Baby {
  id?: number;
  name: string;
  birthDate: LocalDate;
  birthTime?: LocalTime | null;
  sex?: Sex | null;
  bloodType?: string | null;
  notes?: string | null;
  /** Índice na paleta BabyPalette (0 = verde-menta padrão). */
  colorIndex: number;
}

export interface GrowthMeasurement {
  id?: number;
  babyId: number;
  date: LocalDate;
  type: MeasurementType;
  /** Peso em kg; altura e perímetro cefálico em cm. */
  value: number;
  place?: string | null;
  notes?: string | null;
}

export interface Vaccine {
  id?: number;
  babyId: number;
  name: string;
  dose?: string | null;
  /** Idade recomendada em meses, usada para agrupar. Nulo para vacinas avulsas. */
  recommendedAgeMonths?: number | null;
  scheduledDate?: LocalDate | null;
  appliedDate?: LocalDate | null;
  status: VaccineStatus;
  lot?: string | null;
  place?: string | null;
  reaction?: string | null;
  notes?: string | null;
}

export interface Appointment {
  id?: number;
  babyId: number;
  title: string;
  dateTime: LocalDateTime;
  status: AppointmentStatus;
  professional?: string | null;
  place?: string | null;
  reason?: string | null;
  guidance?: string | null;
  notes?: string | null;
}

export interface HealthEvent {
  id?: number;
  babyId: number;
  dateTime: LocalDateTime;
  symptoms: Symptom[];
  otherSymptom?: string | null;
  intensity: Intensity;
  temperatureCelsius?: number | null;
  duration?: string | null;
  actionsTaken?: string | null;
  soughtCare: boolean;
  notes?: string | null;
}

export interface Feeding {
  id?: number;
  babyId: number;
  dateTime: LocalDateTime;
  type: FeedingType;
  breastSide?: BreastSide | null;
  durationMinutes?: number | null;
  amountMl?: number | null;
  food?: string | null;
  texture?: FoodTexture | null;
  acceptance?: Acceptance | null;
  /** Primeira vez que o bebê experimenta este alimento. */
  firstTime: boolean;
  reaction?: string | null;
  notes?: string | null;
}

/** Uma dose de medicamento (cada horário agendado ou administrado é uma linha). */
export interface MedicationDose {
  id?: number;
  babyId: number;
  name: string;
  /** Quantidade em texto livre, como na receita (ex.: "5 ml", "10 gotas"). */
  dosage?: string | null;
  scheduledAt: LocalDateTime;
  status: MedicationStatus;
  /** Hora em que foi realmente administrada; só faz sentido com status GIVEN. */
  givenAt?: LocalDateTime | null;
  notes?: string | null;
  /** Doses criadas juntas por uma repetição compartilham este valor. */
  seriesId?: number | null;
}

export interface Moment {
  id?: number;
  babyId: number;
  dateTime: LocalDateTime;
  title: string;
  description?: string | null;
}

/**
 * Foto anexada a qualquer registro. No navegador a imagem fica guardada na própria linha (Blob),
 * dentro do IndexedDB do aparelho. O vínculo é polimórfico (ownerType + ownerId).
 */
export interface Photo {
  id?: number;
  babyId: number;
  ownerType: RecordType;
  ownerId: number;
  blob: Blob;
  addedAt: LocalDateTime;
  caption?: string | null;
}
