// Utilitários de data sem fuso horário (equivalem a LocalDate/LocalDateTime/YearMonth do Java).
// Todo cálculo é feito em "dias desde 1970" ou "minutos desde 1970" sobre o calendário civil,
// então horário de verão nunca interfere.

import type { LocalDate, LocalDateTime, LocalTime } from './types';

const pad = (n: number, size = 2) => String(n).padStart(size, '0');

export interface Ymd {
  year: number;
  month: number; // 1-12
  day: number;
}

export interface YearMonth {
  year: number;
  month: number; // 1-12
}

export function parseDate(date: LocalDate): Ymd {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(date);
  if (!m) throw new Error(`Data inválida: ${date}`);
  return { year: Number(m[1]), month: Number(m[2]), day: Number(m[3]) };
}

export function formatDate({ year, month, day }: Ymd): LocalDate {
  return `${pad(year, 4)}-${pad(month)}-${pad(day)}`;
}

export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

export function lengthOfMonth(year: number, month: number): number {
  return [31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1];
}

/** Dias desde 1970-01-01 (algoritmo de calendário civil, sem usar Date). */
export function toEpochDay(date: LocalDate): number {
  const { year, month, day } = parseDate(date);
  const y = month <= 2 ? year - 1 : year;
  const era = Math.floor(y / 400);
  const yoe = y - era * 400;
  const doy = Math.floor((153 * (month + (month > 2 ? -3 : 9)) + 2) / 5) + day - 1;
  const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
  return era * 146097 + doe - 719468;
}

export function fromEpochDay(epochDay: number): LocalDate {
  const z = epochDay + 719468;
  const era = Math.floor(z / 146097);
  const doe = z - era * 146097;
  const yoe = Math.floor((doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365);
  const y = yoe + era * 400;
  const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
  const mp = Math.floor((5 * doy + 2) / 153);
  const day = doy - Math.floor((153 * mp + 2) / 5) + 1;
  const month = mp < 10 ? mp + 3 : mp - 9;
  return formatDate({ year: month <= 2 ? y + 1 : y, month, day });
}

export const plusDays = (date: LocalDate, days: number): LocalDate => fromEpochDay(toEpochDay(date) + days);
export const minusDays = (date: LocalDate, days: number): LocalDate => plusDays(date, -days);

/** Soma meses; se o dia não existir no mês de destino, usa o último dia (igual ao Java). */
export function plusMonths(date: LocalDate, months: number): LocalDate {
  const { year, month, day } = parseDate(date);
  const total = year * 12 + (month - 1) + months;
  const newYear = Math.floor(total / 12);
  const newMonth = (((total % 12) + 12) % 12) + 1;
  return formatDate({ year: newYear, month: newMonth, day: Math.min(day, lengthOfMonth(newYear, newMonth)) });
}

export const plusYears = (date: LocalDate, years: number): LocalDate => plusMonths(date, years * 12);
export const minusYears = (date: LocalDate, years: number): LocalDate => plusYears(date, -years);

export const daysBetween = (from: LocalDate, to: LocalDate): number => toEpochDay(to) - toEpochDay(from);

export const isBefore = (a: LocalDate, b: LocalDate): boolean => a < b;
export const isAfter = (a: LocalDate, b: LocalDate): boolean => a > b;

export interface Period {
  years: number;
  months: number;
  days: number;
}

/** Equivale a Period.between do Java, para [from] <= [to]. */
export function periodBetween(from: LocalDate, to: LocalDate): Period {
  const a = parseDate(from);
  const b = parseDate(to);
  let totalMonths = b.year * 12 + b.month - (a.year * 12 + a.month);
  let days = b.day - a.day;
  if (totalMonths > 0 && days < 0) {
    totalMonths -= 1;
    days = daysBetween(plusMonths(from, totalMonths), to);
  }
  return { years: Math.trunc(totalMonths / 12), months: totalMonths % 12, days };
}

/** 0 = domingo ... 6 = sábado. */
export function dayOfWeek(date: LocalDate): number {
  return (((toEpochDay(date) + 4) % 7) + 7) % 7;
}

// --- Hora e data+hora ---

export const dateOf = (dateTime: LocalDateTime): LocalDate => dateTime.slice(0, 10);
export const timeOf = (dateTime: LocalDateTime): LocalTime => dateTime.slice(11, 16);
export const atTime = (date: LocalDate, time: LocalTime = '00:00'): LocalDateTime => `${date}T${time}`;
export const startOfDay = (date: LocalDate): LocalDateTime => atTime(date, '00:00');

function toEpochMinutes(dateTime: LocalDateTime): number {
  const [h, m] = timeOf(dateTime).split(':').map(Number);
  return toEpochDay(dateOf(dateTime)) * 1440 + h * 60 + m;
}

function fromEpochMinutes(minutes: number): LocalDateTime {
  const day = Math.floor(minutes / 1440);
  const rest = minutes - day * 1440;
  return atTime(fromEpochDay(day), `${pad(Math.floor(rest / 60))}:${pad(rest % 60)}`);
}

export const plusHours = (dateTime: LocalDateTime, hours: number): LocalDateTime =>
  fromEpochMinutes(toEpochMinutes(dateTime) + hours * 60);

export const plusMinutes = (dateTime: LocalDateTime, minutes: number): LocalDateTime =>
  fromEpochMinutes(toEpochMinutes(dateTime) + minutes);

export const plusDaysTime = (dateTime: LocalDateTime, days: number): LocalDateTime =>
  atTime(plusDays(dateOf(dateTime), days), timeOf(dateTime));

/** Diferença em minutos (to - from). */
export const minutesBetween = (from: LocalDateTime, to: LocalDateTime): number =>
  toEpochMinutes(to) - toEpochMinutes(from);

// --- YearMonth ---

export const yearMonthOf = (date: LocalDate): YearMonth => {
  const { year, month } = parseDate(date);
  return { year, month };
};

export const atDay = (ym: YearMonth, day: number): LocalDate => formatDate({ year: ym.year, month: ym.month, day });

export const plusMonthsYm = (ym: YearMonth, months: number): YearMonth => {
  const total = ym.year * 12 + (ym.month - 1) + months;
  return { year: Math.floor(total / 12), month: (((total % 12) + 12) % 12) + 1 };
};

// --- Relógio do aparelho ---

export function today(): LocalDate {
  const d = new Date();
  return formatDate({ year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() });
}

export function now(): LocalDateTime {
  const d = new Date();
  return atTime(today(), `${pad(d.getHours())}:${pad(d.getMinutes())}`);
}
