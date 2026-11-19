// Avisos para valores pouco prováveis (ex.: peso digitado em gramas).
// Nunca bloqueiam o registro: o app pede confirmação e respeita a decisão do responsável.

import type { MeasurementType } from './types';

type Range = readonly [number, number];

const inRange = (value: number, [min, max]: Range) => value >= min && value <= max;

export const Plausibility = {
  weightRangeKg: [0.5, 30.0] as Range,
  heightRangeCm: [30.0, 130.0] as Range,
  headRangeCm: [25.0, 60.0] as Range,
  temperatureRangeCelsius: [34.0, 43.0] as Range,

  measurementWarning(type: MeasurementType, value: number): string | null {
    switch (type) {
      case 'WEIGHT':
        return inRange(value, Plausibility.weightRangeKg)
          ? null
          : 'Peso fora do comum para bebês (entre 0,5 e 30 kg). Confira se o valor está em quilos, por exemplo 3,450.';
      case 'HEIGHT':
        return inRange(value, Plausibility.heightRangeCm)
          ? null
          : 'Altura fora do comum para bebês (entre 30 e 130 cm). Confira se o valor está em centímetros.';
      case 'HEAD_CIRCUMFERENCE':
        return inRange(value, Plausibility.headRangeCm)
          ? null
          : 'Perímetro cefálico fora do comum (entre 25 e 60 cm). Confira se o valor está em centímetros.';
    }
  },

  temperatureWarning(value: number): string | null {
    return inRange(value, Plausibility.temperatureRangeCelsius)
      ? null
      : 'Temperatura incomum. Confira o valor digitado.';
  },
};
