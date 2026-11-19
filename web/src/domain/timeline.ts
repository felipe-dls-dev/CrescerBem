// Item da linha do tempo: forma comum para listar qualquer registro no calendário e em Hoje.

import { dateOf, now, timeOf } from './dates';
import { Formats } from './formats';
import {
  appointmentStatusLabel,
  breastSideLabel,
  acceptanceLabel,
  feedingTypeLabel,
  intensityLabel,
  measurementLabel,
  medicationStatusLabel,
  symptomLabel,
} from './labels';
import { vaccineDisplayLabel, vaccineDisplayStatus, vaccineReferenceDate, vaccineTitle } from './vaccineRules';
import {
  recordTypeOrdinal,
  type Appointment,
  type Feeding,
  type GrowthMeasurement,
  type HealthEvent,
  type LocalDate,
  type LocalDateTime,
  type LocalTime,
  type MedicationDose,
  type Moment,
  type RecordType,
  type Vaccine,
} from './types';

export interface TimelineItem {
  type: RecordType;
  id: number;
  date: LocalDate;
  time: LocalTime | null;
  title: string;
  detail?: string | null;
}

/** Chave de ordenação: registros só de data valem 00:00 do dia. */
export const sortKey = (item: TimelineItem): LocalDateTime => `${item.date}T${item.time ?? '00:00'}`;

export function sortedChronologically(items: TimelineItem[]): TimelineItem[] {
  return [...items].sort((a, b) => {
    const ka = sortKey(a);
    const kb = sortKey(b);
    if (ka !== kb) return ka < kb ? -1 : 1;
    const ta = recordTypeOrdinal(a.type);
    const tb = recordTypeOrdinal(b.type);
    if (ta !== tb) return ta - tb;
    return a.id - b.id;
  });
}

const nonBlank = (text: string | null | undefined): string | null => (text && text.trim() ? text : null);
const joinOrNull = (parts: (string | null | undefined)[], separator: string): string | null => {
  const joined = parts.filter((p): p is string => !!p).join(separator);
  return joined === '' ? null : joined;
};

export function growthToTimelineItem(g: GrowthMeasurement): TimelineItem {
  return {
    type: 'GROWTH',
    id: g.id!,
    date: g.date,
    time: null,
    title: `${measurementLabel[g.type]}: ${Formats.measurement(g.type, g.value)}`,
    detail: joinOrNull([nonBlank(g.place), nonBlank(g.notes)], ' · '),
  };
}

export function vaccineToTimelineItem(v: Vaccine, today: LocalDate): TimelineItem | null {
  const date = vaccineReferenceDate(v);
  if (!date) return null;
  return {
    type: 'VACCINE',
    id: v.id!,
    date,
    time: null,
    title: `Vacina: ${vaccineTitle(v)}`,
    detail: vaccineDisplayLabel[vaccineDisplayStatus(v, today)],
  };
}

export function appointmentToTimelineItem(a: Appointment): TimelineItem {
  return {
    type: 'APPOINTMENT',
    id: a.id!,
    date: dateOf(a.dateTime),
    time: timeOf(a.dateTime),
    title: a.title,
    detail: joinOrNull(
      [a.status !== 'SCHEDULED' ? appointmentStatusLabel[a.status] : null, nonBlank(a.professional), nonBlank(a.place)],
      ' · ',
    ),
  };
}

/** Horário que vale para a dose: o da administração, se já foi dada; senão o agendado. */
export const medicationReferenceTime = (m: MedicationDose): LocalDateTime =>
  m.status === 'GIVEN' ? (m.givenAt ?? m.scheduledAt) : m.scheduledAt;

/** Dose agendada cujo horário já passou sem ser confirmada. */
export const isMedicationOverdue = (m: MedicationDose, at: LocalDateTime): boolean =>
  m.status === 'SCHEDULED' && m.scheduledAt < at;

/** Rótulo exibido: dose agendada que já passou sem marcação vira "Não registrado". */
export const medicationStatusText = (m: MedicationDose, at: LocalDateTime): string =>
  isMedicationOverdue(m, at) ? 'Não registrado' : medicationStatusLabel[m.status];

export function medicationToTimelineItem(m: MedicationDose, at: LocalDateTime = now()): TimelineItem {
  const ref = medicationReferenceTime(m);
  return {
    type: 'MEDICATION',
    id: m.id!,
    date: dateOf(ref),
    time: timeOf(ref),
    title: joinOrNull([m.name, nonBlank(m.dosage)], ' · ') ?? m.name,
    detail: medicationStatusText(m, at),
  };
}

export function symptomsText(e: HealthEvent): string {
  const names: string[] = e.symptoms.filter((s) => s !== 'OTHER').map((s) => symptomLabel[s]);
  if (e.symptoms.includes('OTHER') || nonBlank(e.otherSymptom)) {
    names.push(nonBlank(e.otherSymptom) ?? symptomLabel.OTHER);
  }
  return names.join(', ') || 'Mal-estar';
}

export function healthToTimelineItem(e: HealthEvent): TimelineItem {
  return {
    type: 'HEALTH_EVENT',
    id: e.id!,
    date: dateOf(e.dateTime),
    time: timeOf(e.dateTime),
    title: symptomsText(e),
    detail: joinOrNull(
      [
        `Intensidade ${intensityLabel[e.intensity].toLowerCase()}`,
        e.temperatureCelsius != null ? Formats.temperature(e.temperatureCelsius) : null,
      ],
      ' · ',
    ),
  };
}

export function describeFeeding(f: Feeding): string {
  const parts: string[] = [];
  switch (f.type) {
    case 'BREAST_MILK':
      if (f.breastSide) parts.push(`lado ${breastSideLabel[f.breastSide].toLowerCase()}`);
      if (f.durationMinutes != null) parts.push(`${f.durationMinutes} min`);
      if (f.amountMl != null) parts.push(`${f.amountMl} ml`);
      break;
    case 'FORMULA':
    case 'WATER':
      if (f.amountMl != null) parts.push(`${f.amountMl} ml`);
      break;
    default:
      if (nonBlank(f.food)) parts.push(f.food!.trim());
      if (f.amountMl != null) parts.push(`${f.amountMl} ml`);
      if (f.acceptance) parts.push(acceptanceLabel[f.acceptance].toLowerCase());
  }
  if (f.firstTime) parts.push('primeira vez');
  return parts.join(' · ');
}

export function feedingToTimelineItem(f: Feeding): TimelineItem {
  return {
    type: 'FEEDING',
    id: f.id!,
    date: dateOf(f.dateTime),
    time: timeOf(f.dateTime),
    title: feedingTypeLabel[f.type],
    detail: describeFeeding(f) || null,
  };
}

export function momentToTimelineItem(m: Moment): TimelineItem {
  return {
    type: 'MOMENT',
    id: m.id!,
    date: dateOf(m.dateTime),
    time: timeOf(m.dateTime),
    title: m.title,
    detail: nonBlank(m.description),
  };
}
