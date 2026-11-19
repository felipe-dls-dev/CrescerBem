// Relatório independente de formato; o PDF (ver report/pdfReport.ts) é só uma forma de desenhá-lo.

import { BabyAge } from './babyAge';
import { dateOf } from './dates';
import { FeedingSummaries } from './feedingSummary';
import { Formats } from './formats';
import {
  acceptanceLabel,
  appointmentStatusLabel,
  feedingTypeLabel,
  foodTextureLabel,
  intensityLabel,
  isSolid,
  measurementLabel,
  recordTypePluralLabel,
  yesNo,
} from './labels';
import { Texts } from './texts';
import { medicationReferenceTime, medicationStatusText, symptomsText } from './timeline';
import { vaccineDisplayLabel, vaccineDisplayStatus, vaccineReferenceDate, vaccineTitle } from './vaccineRules';
import {
  MEASUREMENT_TYPES,
  type Appointment,
  type Baby,
  type Feeding,
  type GrowthMeasurement,
  type HealthEvent,
  type LocalDate,
  type LocalDateTime,
  type MedicationDose,
  type Moment,
  type RecordType,
  type Vaccine,
} from './types';

/** Pedido do responsável na tela de relatórios. */
export interface ReportRequest {
  baby: Baby;
  start: LocalDate;
  end: LocalDate;
  types: RecordType[];
  includePhotos: boolean;
}

export function validateReportRequest(request: ReportRequest): void {
  if (request.end < request.start) throw new Error('O fim do período não pode ser antes do início.');
}

/** Chave "TIPO:id" de um registro, usada para ligar fotos aos registros. */
export const recordKey = (type: RecordType, id: number): string => `${type}:${id}`;

/** Registros carregados do banco para montar o relatório. */
export interface ReportData {
  measurements: GrowthMeasurement[];
  vaccines: Vaccine[];
  appointments: Appointment[];
  healthEvents: HealthEvent[];
  feedings: Feeding[];
  moments: Moment[];
  medications: MedicationDose[];
  /** Identificadores das fotos de cada registro (chave de recordKey). */
  photos: Record<string, number[]>;
}

export const emptyReportData = (): ReportData => ({
  measurements: [],
  vaccines: [],
  appointments: [],
  healthEvents: [],
  feedings: [],
  moments: [],
  medications: [],
  photos: {},
});

export type ReportBlock =
  | { kind: 'paragraph'; text: string }
  | {
      kind: 'table';
      headers: string[];
      rows: string[][];
      /** Peso relativo da largura de cada coluna. */
      weights: number[];
    }
  | { kind: 'entry'; heading: string; lines: string[]; photos: number[] };

export interface ReportSection {
  title: string;
  blocks: ReportBlock[];
}

export interface ReportDocument {
  title: string;
  headerLines: string[];
  sections: ReportSection[];
  footer: string;
}

/** Quantidade de registros de cada tipo, usada na pré-visualização e no resumo. */
export function countFor(data: ReportData, type: RecordType): number {
  switch (type) {
    case 'GROWTH':
      return data.measurements.length;
    case 'VACCINE':
      return data.vaccines.length;
    case 'APPOINTMENT':
      return data.appointments.length;
    case 'HEALTH_EVENT':
      return data.healthEvents.length;
    case 'FEEDING':
      return data.feedings.length;
    case 'MOMENT':
      return data.moments.length;
    case 'MEDICATION':
      return data.medications.length;
    case 'BABY':
      return 0;
  }
}

/** Ordem das seções no relatório. */
export const REPORTABLE_TYPES: RecordType[] = [
  'GROWTH',
  'VACCINE',
  'APPOINTMENT',
  'HEALTH_EVENT',
  'MEDICATION',
  'FEEDING',
  'MOMENT',
];

const recordTypeLabelFor = (f: Feeding): string => feedingTypeLabel[f.type];
const line = (label: string, value: string | null | undefined): string | null =>
  value && value.trim() ? `${label}: ${value.trim()}` : null;
const compact = (items: (string | null)[]): string[] => items.filter((i): i is string => i !== null);
const byKey = <T>(items: T[], key: (item: T) => string): T[] =>
  [...items].sort((a, b) => (key(a) < key(b) ? -1 : key(a) > key(b) ? 1 : 0));

/** Mantém apenas registros do bebê, do período e dos tipos pedidos. */
export function filterReportData(request: ReportRequest, data: ReportData): ReportData {
  const babyId = request.baby.id;
  const inPeriod = (date: LocalDate) => date >= request.start && date <= request.end;
  const wants = (type: RecordType) => request.types.includes(type);
  return {
    measurements: wants('GROWTH')
      ? byKey(
          data.measurements.filter((m) => m.babyId === babyId && inPeriod(m.date)),
          (m) => m.date,
        )
      : [],
    vaccines: wants('VACCINE')
      ? byKey(
          data.vaccines.filter((v) => {
            const ref = vaccineReferenceDate(v);
            return v.babyId === babyId && ref !== null && inPeriod(ref);
          }),
          (v) => vaccineReferenceDate(v)!,
        )
      : [],
    appointments: wants('APPOINTMENT')
      ? byKey(
          data.appointments.filter((a) => a.babyId === babyId && inPeriod(dateOf(a.dateTime))),
          (a) => a.dateTime,
        )
      : [],
    healthEvents: wants('HEALTH_EVENT')
      ? byKey(
          data.healthEvents.filter((e) => e.babyId === babyId && inPeriod(dateOf(e.dateTime))),
          (e) => e.dateTime,
        )
      : [],
    feedings: wants('FEEDING')
      ? byKey(
          data.feedings.filter((f) => f.babyId === babyId && inPeriod(dateOf(f.dateTime))),
          (f) => f.dateTime,
        )
      : [],
    moments: wants('MOMENT')
      ? byKey(
          data.moments.filter((m) => m.babyId === babyId && inPeriod(dateOf(m.dateTime))),
          (m) => m.dateTime,
        )
      : [],
    medications: wants('MEDICATION')
      ? byKey(
          data.medications.filter((m) => m.babyId === babyId && inPeriod(dateOf(medicationReferenceTime(m)))),
          medicationReferenceTime,
        )
      : [],
    photos: request.includePhotos ? data.photos : {},
  };
}

/**
 * Monta o documento. Filtra de novo por bebê e período, para garantir que nada
 * fora do pedido entre no relatório mesmo que a consulta tenha trazido a mais.
 */
export function buildReport(request: ReportRequest, data: ReportData, generatedAt: LocalDateTime): ReportDocument {
  validateReportRequest(request);
  const filtered = filterReportData(request, data);
  const baby = request.baby;
  const header = [
    `Bebê: ${baby.name}`,
    `Nascimento: ${Formats.date(baby.birthDate)}`,
    `Idade no fim do período: ${BabyAge.describe(baby.birthDate, request.end)}`,
    `Período: ${Formats.date(request.start)} a ${Formats.date(request.end)}`,
    `Gerado em: ${Formats.dateTime(generatedAt)}`,
  ];
  const selected = REPORTABLE_TYPES.filter((t) => request.types.includes(t));
  const sections: ReportSection[] = [summarySection(selected, filtered)];
  for (const type of selected) {
    switch (type) {
      case 'GROWTH':
        sections.push(growthSection(filtered, request.includePhotos));
        break;
      case 'VACCINE':
        sections.push(vaccineSection(filtered, request.end, request.includePhotos));
        break;
      case 'APPOINTMENT':
        sections.push(appointmentSection(filtered, request.includePhotos));
        break;
      case 'HEALTH_EVENT':
        sections.push(healthSection(filtered, request.includePhotos));
        break;
      case 'FEEDING':
        sections.push(feedingSection(filtered, request.includePhotos));
        break;
      case 'MOMENT':
        sections.push(momentSection(filtered, request.includePhotos));
        break;
      case 'MEDICATION':
        sections.push(medicationSection(filtered, generatedAt));
        break;
      case 'BABY':
        throw new Error('Tipo não reportável');
    }
  }
  return { title: 'Relatório de acompanhamento', headerLines: header, sections, footer: Texts.REPORT_FOOTER };
}

const emptySection = (type: RecordType): ReportSection => ({
  title: recordTypePluralLabel[type],
  blocks: [{ kind: 'paragraph', text: Texts.EMPTY_PERIOD }],
});

const photosFor = (data: ReportData, type: RecordType, id: number, include: boolean): number[] =>
  include ? (data.photos[recordKey(type, id)] ?? []) : [];

function photoEntries(
  data: ReportData,
  type: RecordType,
  include: boolean,
  items: { id: number; heading: string }[],
): ReportBlock[] {
  if (!include) return [];
  const blocks: ReportBlock[] = [];
  for (const { id, heading } of items) {
    const photos = photosFor(data, type, id, true);
    if (photos.length > 0) blocks.push({ kind: 'entry', heading: `Fotos: ${heading}`, lines: [], photos });
  }
  return blocks;
}

function summarySection(types: RecordType[], data: ReportData): ReportSection {
  return {
    title: 'Resumo do período',
    blocks: [
      {
        kind: 'table',
        headers: ['Tipo de registro', 'Quantidade'],
        rows: types.map((t) => [recordTypePluralLabel[t], String(countFor(data, t))]),
        weights: [3, 1],
      },
    ],
  };
}

function medicationSection(data: ReportData, generatedAt: LocalDateTime): ReportSection {
  const items = data.medications;
  if (items.length === 0) return emptySection('MEDICATION');
  return {
    title: recordTypePluralLabel.MEDICATION,
    blocks: items.map((m) => ({
      kind: 'entry' as const,
      heading: `${Formats.dateTime(medicationReferenceTime(m))} · ${m.name}`,
      lines: compact([
        `Situação: ${medicationStatusText(m, generatedAt)}`,
        line('Dose', m.dosage),
        m.status === 'GIVEN' && m.givenAt && m.givenAt !== m.scheduledAt
          ? `Horário previsto: ${Formats.dateTime(m.scheduledAt)}`
          : null,
        line('Observações', m.notes),
      ]),
      photos: [],
    })),
  };
}

function growthSection(data: ReportData, includePhotos: boolean): ReportSection {
  const items = data.measurements;
  if (items.length === 0) return emptySection('GROWTH');
  const blocks: ReportBlock[] = [
    {
      kind: 'table',
      headers: ['Data', 'Medida', 'Valor', 'Local / observação'],
      rows: items.map((m) => [
        Formats.date(m.date),
        measurementLabel[m.type],
        Formats.measurement(m.type, m.value),
        [m.place, m.notes].filter((x): x is string => !!x && x.trim() !== '').join(' · '),
      ]),
      weights: [1.2, 1.5, 1.2, 2.6],
    },
  ];
  for (const type of MEASUREMENT_TYPES) {
    const ofType = items.filter((m) => m.type === type);
    if (ofType.length === 0) continue;
    // Último pela data; em empate fica o primeiro da lista (igual ao maxByOrNull do Kotlin).
    const last = ofType.reduce((best, m) => (m.date > best.date ? m : best));
    blocks.push({
      kind: 'paragraph',
      text:
        `Último registro de ${measurementLabel[type].toLowerCase()} no período: ` +
        `${Formats.measurement(type, last.value)} em ${Formats.date(last.date)}.`,
    });
  }
  blocks.push(
    ...photoEntries(
      data,
      'GROWTH',
      includePhotos,
      items.map((m) => ({ id: m.id!, heading: `${measurementLabel[m.type]} em ${Formats.date(m.date)}` })),
    ),
  );
  return { title: recordTypePluralLabel.GROWTH, blocks };
}

function vaccineSection(data: ReportData, end: LocalDate, includePhotos: boolean): ReportSection {
  const items = data.vaccines;
  if (items.length === 0) return emptySection('VACCINE');
  const blocks: ReportBlock[] = [
    {
      kind: 'table',
      headers: ['Vacina', 'Dose', 'Situação', 'Data', 'Lote / local / reação'],
      rows: items.map((v) => [
        v.name,
        v.dose ?? '',
        vaccineDisplayLabel[vaccineDisplayStatus(v, end)],
        vaccineReferenceDate(v) ? Formats.date(vaccineReferenceDate(v)!) : '',
        [
          v.lot && v.lot.trim() ? `Lote ${v.lot}` : null,
          v.place,
          v.reaction && v.reaction.trim() ? `Reação: ${v.reaction}` : null,
          v.notes,
        ]
          .filter((x): x is string => !!x && x.trim() !== '')
          .join(' · '),
      ]),
      weights: [2.4, 1.1, 1.1, 1.1, 2.4],
    },
    { kind: 'paragraph', text: Texts.VACCINE_NOTE },
    ...photoEntries(
      data,
      'VACCINE',
      includePhotos,
      items.map((v) => ({ id: v.id!, heading: vaccineTitle(v) })),
    ),
  ];
  return { title: recordTypePluralLabel.VACCINE, blocks };
}

function appointmentSection(data: ReportData, includePhotos: boolean): ReportSection {
  const items = data.appointments;
  if (items.length === 0) return emptySection('APPOINTMENT');
  return {
    title: recordTypePluralLabel.APPOINTMENT,
    blocks: items.map((a) => ({
      kind: 'entry' as const,
      heading: `${Formats.dateTime(a.dateTime)} · ${a.title}`,
      lines: compact([
        `Situação: ${appointmentStatusLabel[a.status]}`,
        line('Profissional', a.professional),
        line('Local', a.place),
        line('Motivo', a.reason),
        line('Orientações recebidas', a.guidance),
        line('Observações', a.notes),
      ]),
      photos: photosFor(data, 'APPOINTMENT', a.id!, includePhotos),
    })),
  };
}

function healthSection(data: ReportData, includePhotos: boolean): ReportSection {
  const items = data.healthEvents;
  if (items.length === 0) return emptySection('HEALTH_EVENT');
  return {
    title: recordTypePluralLabel.HEALTH_EVENT,
    blocks: items.map((e) => ({
      kind: 'entry' as const,
      heading: `${Formats.dateTime(e.dateTime)} · ${symptomsText(e)}`,
      lines: compact([
        `Intensidade: ${intensityLabel[e.intensity]}`,
        e.temperatureCelsius != null ? `Temperatura: ${Formats.temperature(e.temperatureCelsius)}` : null,
        line('Duração', e.duration),
        line('O que foi feito', e.actionsTaken),
        `Procurou atendimento: ${yesNo(e.soughtCare)}`,
        line('Observações', e.notes),
      ]),
      photos: photosFor(data, 'HEALTH_EVENT', e.id!, includePhotos),
    })),
  };
}

function feedingSection(data: ReportData, includePhotos: boolean): ReportSection {
  const items = data.feedings;
  if (items.length === 0) return emptySection('FEEDING');
  const blocks: ReportBlock[] = [
    {
      kind: 'table',
      headers: ['Dia', 'Mamadas (min)', 'Fórmula (ml)', 'Água (ml)', 'Refeições', 'Alimentos novos'],
      rows: FeedingSummaries.byDay(items).map(([day, s]) => [
        Formats.date(day),
        s.breastfeedings === 0 ? '-' : `${s.breastfeedings} (${s.breastMinutes})`,
        s.formulaCount === 0 ? '-' : String(s.formulaMl),
        s.waterMl === 0 ? '-' : String(s.waterMl),
        s.solidMeals === 0 ? '-' : String(s.solidMeals),
        s.newFoods.join(', ') || '-',
      ]),
      weights: [1.2, 1.3, 1.1, 1, 1, 2],
    },
  ];
  const solids = items.filter((f) => isSolid(f.type));
  if (solids.length > 0) {
    blocks.push({ kind: 'paragraph', text: 'Introdução alimentar' });
    blocks.push({
      kind: 'table',
      headers: ['Data', 'Alimento', 'Forma', 'Aceitação', 'Reação / observação'],
      rows: solids.map((f) => [
        Formats.dateTime(f.dateTime),
        ((f.food ?? '').trim() || recordTypeLabelFor(f)) + (f.firstTime ? ' (1ª vez)' : ''),
        f.texture ? foodTextureLabel[f.texture] : '',
        f.acceptance ? acceptanceLabel[f.acceptance] : '',
        [f.reaction, f.notes].filter((x): x is string => !!x && x.trim() !== '').join(' · '),
      ]),
      weights: [1.5, 1.6, 1.1, 1.1, 2],
    });
  }
  blocks.push(
    ...photoEntries(
      data,
      'FEEDING',
      includePhotos,
      items.map((f) => ({ id: f.id!, heading: `${recordTypeLabelFor(f)} em ${Formats.dateTime(f.dateTime)}` })),
    ),
  );
  return { title: recordTypePluralLabel.FEEDING, blocks };
}

function momentSection(data: ReportData, includePhotos: boolean): ReportSection {
  const items = data.moments;
  if (items.length === 0) return emptySection('MOMENT');
  return {
    title: recordTypePluralLabel.MOMENT,
    blocks: items.map((m) => ({
      kind: 'entry' as const,
      heading: `${Formats.dateTime(m.dateTime)} · ${m.title}`,
      lines: compact([m.description && m.description.trim() ? m.description : null]),
      photos: photosFor(data, 'MOMENT', m.id!, includePhotos),
    })),
  };
}
