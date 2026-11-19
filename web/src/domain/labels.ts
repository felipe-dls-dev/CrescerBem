// Rótulos em português usados pela interface e pelo relatório em PDF.

import type {
  Acceptance,
  AppointmentStatus,
  BreastSide,
  FeedingType,
  FoodTexture,
  Intensity,
  MeasurementType,
  MedicationStatus,
  RecordType,
  Sex,
  Symptom,
} from './types';

export const recordTypeLabel: Record<RecordType, string> = {
  BABY: 'Bebê',
  MOMENT: 'Momento',
  VACCINE: 'Vacina',
  APPOINTMENT: 'Consulta',
  HEALTH_EVENT: 'Sintoma',
  GROWTH: 'Medida',
  FEEDING: 'Alimentação',
  MEDICATION: 'Medicamento',
};

export const recordTypePluralLabel: Record<RecordType, string> = {
  BABY: 'Bebês',
  MOMENT: 'Momentos',
  VACCINE: 'Vacinas',
  APPOINTMENT: 'Consultas',
  HEALTH_EVENT: 'Sintomas e mal-estar',
  GROWTH: 'Crescimento',
  FEEDING: 'Alimentação',
  MEDICATION: 'Medicamentos',
};

export const medicationStatusLabel: Record<MedicationStatus, string> = {
  SCHEDULED: 'Agendado',
  GIVEN: 'Realizado',
  SKIPPED: 'Não realizado',
};

export const measurementLabel: Record<MeasurementType, string> = {
  WEIGHT: 'Peso',
  HEIGHT: 'Altura',
  HEAD_CIRCUMFERENCE: 'Perímetro cefálico',
};

export const measurementUnit: Record<MeasurementType, string> = {
  WEIGHT: 'kg',
  HEIGHT: 'cm',
  HEAD_CIRCUMFERENCE: 'cm',
};

/** Casas decimais exibidas: peso com 3 (gramas), comprimentos com 1. */
export const measurementDecimals: Record<MeasurementType, number> = {
  WEIGHT: 3,
  HEIGHT: 1,
  HEAD_CIRCUMFERENCE: 1,
};

export const sexLabel: Record<Sex, string> = {
  FEMALE: 'Feminino',
  MALE: 'Masculino',
};

export const appointmentStatusLabel: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Agendada',
  DONE: 'Realizada',
  CANCELED: 'Cancelada',
};

export const intensityLabel: Record<Intensity, string> = {
  MILD: 'Leve',
  MODERATE: 'Moderada',
  STRONG: 'Forte',
};

export const symptomLabel: Record<Symptom, string> = {
  FEVER: 'Febre',
  COUGH: 'Tosse',
  RUNNY_NOSE: 'Coriza',
  VOMITING: 'Vômito',
  DIARRHEA: 'Diarreia',
  COLIC: 'Cólica',
  IRRITABILITY: 'Irritabilidade',
  SLEEP_CHANGES: 'Sono alterado',
  SKIN_RASH: 'Pele (assadura, manchas)',
  FEEDING_REFUSAL: 'Recusa para mamar ou comer',
  BREATHING_DIFFICULTY: 'Dificuldade para respirar',
  OTHER: 'Outro',
};

export const feedingTypeLabel: Record<FeedingType, string> = {
  BREAST_MILK: 'Aleitamento materno',
  FORMULA: 'Fórmula',
  FRUIT: 'Fruta',
  SOLID_MEAL: 'Papa salgada / refeição',
  WATER: 'Água',
  OTHER: 'Outro',
};

export const isSolid = (type: FeedingType): boolean => type === 'FRUIT' || type === 'SOLID_MEAL';

export const breastSideLabel: Record<BreastSide, string> = {
  LEFT: 'Esquerdo',
  RIGHT: 'Direito',
  BOTH: 'Os dois',
};

export const foodTextureLabel: Record<FoodTexture, string> = {
  PUREE: 'Papa / purê',
  MASHED: 'Amassado',
  PIECES: 'Em pedaços',
};

export const acceptanceLabel: Record<Acceptance, string> = {
  GOOD: 'Aceitou bem',
  LITTLE: 'Aceitou pouco',
  REFUSED: 'Recusou',
};

export const yesNo = (value: boolean): string => (value ? 'Sim' : 'Não');
