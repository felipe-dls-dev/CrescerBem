// Formatação no padrão brasileiro (datas dd/MM/aaaa, vírgula decimal).
// Nomes de meses e dias ficam em tabelas próprias para não depender do ICU do navegador.

import { dateOf, dayOfWeek, parseDate, timeOf, type YearMonth } from './dates';
import { measurementDecimals, measurementLabel, measurementUnit } from './labels';
import type { LocalDate, LocalDateTime, LocalTime, MeasurementType } from './types';

const pad = (n: number) => String(n).padStart(2, '0');

export const MONTH_NAMES = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
];

/** Nomes dos dias, começando no domingo. */
export const WEEKDAY_NAMES = [
  'domingo',
  'segunda-feira',
  'terça-feira',
  'quarta-feira',
  'quinta-feira',
  'sexta-feira',
  'sábado',
];

const capitalizeFirst = (text: string): string => text.charAt(0).toLocaleUpperCase('pt-BR') + text.slice(1);

export const Formats = {
  /** "17/11/2026" */
  date(value: LocalDate): string {
    const { year, month, day } = parseDate(value);
    return `${pad(day)}/${pad(month)}/${year}`;
  },

  /** "17/11" */
  shortDate(value: LocalDate): string {
    const { month, day } = parseDate(value);
    return `${pad(day)}/${pad(month)}`;
  },

  /** Ex.: "Terça-feira, 17 de novembro". */
  longDate(value: LocalDate): string {
    const { month, day } = parseDate(value);
    return capitalizeFirst(`${WEEKDAY_NAMES[dayOfWeek(value)]}, ${day} de ${MONTH_NAMES[month - 1]}`);
  },

  /** Ex.: "Novembro de 2026". */
  monthYear(value: YearMonth): string {
    return capitalizeFirst(`${MONTH_NAMES[value.month - 1]} de ${value.year}`);
  },

  time(value: LocalTime): string {
    return value.slice(0, 5);
  },

  dateTime(value: LocalDateTime): string {
    return `${Formats.date(dateOf(value))} ${Formats.time(timeOf(value))}`;
  },

  /** "2026-11-17" (usado em nomes de arquivo). */
  fileStamp(value: LocalDate): string {
    return value.slice(0, 10);
  },

  decimal(value: number, decimals: number): string {
    return value.toFixed(Math.max(0, decimals)).replace('.', ',');
  },

  /** Ex.: "3,450 kg", "52,0 cm". */
  measurement(type: MeasurementType, value: number): string {
    return `${Formats.decimal(value, measurementDecimals[type])} ${measurementUnit[type]}`;
  },

  measurementTitle(type: MeasurementType, value: number): string {
    return `${measurementLabel[type]}: ${Formats.measurement(type, value)}`;
  },

  temperature(value: number): string {
    return `${Formats.decimal(value, 1)} °C`;
  },

  /** Aceita "3,45" e "3.45". Retorna null para texto vazio ou inválido. */
  parseDecimal(text: string): number | null {
    const cleaned = text.trim().replace(',', '.');
    if (cleaned === '' || !/^[+-]?(\d+\.?\d*|\.\d+)$/.test(cleaned)) return null;
    const value = Number(cleaned);
    return Number.isFinite(value) ? value : null;
  },

  parseInt(text: string): number | null {
    const cleaned = text.trim();
    return /^[+-]?\d+$/.test(cleaned) ? Number(cleaned) : null;
  },
};

export function plural(count: number, singular: string, pluralForm: string): string {
  return `${count} ${count === 1 ? singular : pluralForm}`;
}
