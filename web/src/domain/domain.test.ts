// Testes portados de app/src/test/java/br/com/crescerbem/domain/*.kt.
// Mesmos cenários e mesmos valores esperados; todos os dados são fictícios.

import { describe, expect, it } from 'vitest';
import { AttentionSigns } from './attentionSigns';
import { BabyAge } from './babyAge';
import { CalendarGrid } from './calendarGrid';
import { atTime, dateOf, minusDays, minusYears, plusDays, plusHours, plusMonths } from './dates';
import { FeedingSummaries } from './feedingSummary';
import { Formats, plural } from './formats';
import { MedicationSchedule } from './medicationSchedule';
import { Plausibility } from './plausibility';
import { buildReport, emptyReportData, filterReportData, recordKey, type ReportData, type ReportDocument } from './report';
import { Texts } from './texts';
import {
  describeFeeding,
  feedingToTimelineItem,
  medicationStatusText,
  sortedChronologically,
  type TimelineItem,
} from './timeline';
import { SYMPTOMS, type Baby, type Feeding, type HealthEvent, type MedicationDose, type Symptom, type Vaccine } from './types';
import { recordTypePluralLabel } from './labels';
import { VaccineSchedule, vaccineDisplayStatus, vaccineReferenceDate } from './vaccineRules';

describe('BabyAge', () => {
  const birth = '2026-01-01';

  it('dia do nascimento', () => {
    expect(BabyAge.describe(birth, birth)).toBe('Nasceu hoje');
  });

  it('primeiras duas semanas em dias', () => {
    expect(BabyAge.describe(birth, plusDays(birth, 1))).toBe('1 dia');
    expect(BabyAge.describe(birth, plusDays(birth, 13))).toBe('13 dias');
  });

  it('até 59 dias em semanas e dias', () => {
    expect(BabyAge.describe(birth, plusDays(birth, 14))).toBe('2 semanas');
    expect(BabyAge.describe(birth, plusDays(birth, 16))).toBe('2 semanas e 2 dias');
    expect(BabyAge.describe(birth, plusDays(birth, 59))).toBe('8 semanas e 3 dias');
  });

  it('primeiro ano em meses e dias', () => {
    expect(BabyAge.describe(birth, '2026-03-05')).toBe('2 meses e 4 dias');
    expect(BabyAge.describe(birth, '2026-07-01')).toBe('6 meses');
    // Fim de mês: 31/01 a 01/04 são 60 dias, contados como 2 meses e 1 dia.
    expect(BabyAge.describe('2026-01-31', '2026-04-01')).toBe('2 meses e 1 dia');
  });

  it('depois de um ano em anos e meses', () => {
    expect(BabyAge.describe(birth, '2027-01-01')).toBe('1 ano');
    expect(BabyAge.describe(birth, '2027-03-10')).toBe('1 ano e 2 meses');
    expect(BabyAge.describe(birth, '2028-02-01')).toBe('2 anos e 1 mês');
  });

  it('data futura não quebra', () => {
    expect(BabyAge.describe(birth, minusDays(birth, 3))).toBe('Nascimento previsto para 01/01/2026');
    expect(BabyAge.months(birth, minusDays(birth, 3))).toBe(0);
  });

  it('frase do cabeçalho sem erro gramatical', () => {
    expect(BabyAge.sentence('Ana Teste', birth, birth)).toBe('Ana Teste nasceu hoje');
    expect(BabyAge.sentence('Ana Teste', birth, plusDays(birth, 5))).toBe('Ana Teste tem 5 dias');
    expect(BabyAge.sentence('Ana Teste', birth, '2027-03-10')).toBe('Ana Teste tem 1 ano e 2 meses');
    expect(BabyAge.sentence('Ana Teste', birth, minusDays(birth, 1))).toBe('Nascimento previsto para 01/01/2026');
  });

  it('meses completos', () => {
    expect(BabyAge.months(birth, '2026-01-31')).toBe(0);
    expect(BabyAge.months(birth, '2026-02-01')).toBe(1);
    expect(BabyAge.months(birth, '2027-03-01')).toBe(14);
  });
});

describe('Regras de vacina', () => {
  const baby: Baby = { id: 7, name: 'Bebê Teste', birthDate: '2026-01-15', colorIndex: 0 };
  const today = '2026-06-01';
  const vaccine = (v: Partial<Vaccine> = {}): Vaccine => ({ babyId: 7, name: 'X', status: 'SCHEDULED', ...v });

  it('calendário sugerido usa a data de nascimento', () => {
    const suggested = VaccineSchedule.suggestedFor(baby);
    expect(suggested).toHaveLength(VaccineSchedule.suggestions.length);
    expect(suggested.every((v) => v.babyId === 7 && v.status === 'SCHEDULED')).toBe(true);
    expect(suggested.find((v) => v.name === 'BCG')!.scheduledDate).toBe('2026-01-15');
    const penta2 = suggested.find((v) => v.name.startsWith('Penta') && v.dose === '2ª dose')!;
    expect(penta2.scheduledDate).toBe('2026-05-15');
    expect(penta2.recommendedAgeMonths).toBe(4);
  });

  it('sugestões não duplicam vacinas existentes', () => {
    const existing = [
      vaccine({ name: 'bcg', dose: 'dose única' }),
      vaccine({ name: 'Hepatite B', dose: 'Dose ao nascer' }),
    ];
    const missing = VaccineSchedule.missingFor(baby, existing);
    expect(missing).toHaveLength(VaccineSchedule.suggestions.length - 2);
    expect(missing.some((v) => v.name === 'BCG')).toBe(false);
  });

  it('status calculado pela data', () => {
    const scheduled = (date: string | null) => vaccine({ scheduledDate: date });
    expect(vaccineDisplayStatus(scheduled(plusDays(today, 1)), today)).toBe('PLANNED');
    expect(vaccineDisplayStatus(scheduled(null), today)).toBe('PLANNED');
    expect(vaccineDisplayStatus(scheduled(today), today)).toBe('PENDING');
    expect(vaccineDisplayStatus(scheduled(minusDays(today, 30)), today)).toBe('PENDING');
    expect(vaccineDisplayStatus(scheduled(minusDays(today, 31)), today)).toBe('OVERDUE');
  });

  it('aplicada e não se aplica ignoram a data', () => {
    const old = minusYears(today, 1);
    expect(vaccineDisplayStatus(vaccine({ scheduledDate: old, status: 'APPLIED' }), today)).toBe('APPLIED');
    expect(vaccineDisplayStatus(vaccine({ scheduledDate: old, status: 'NOT_APPLICABLE' }), today)).toBe(
      'NOT_APPLICABLE',
    );
  });

  it('data de referência prefere a aplicação', () => {
    expect(vaccineReferenceDate(vaccine({ scheduledDate: '2026-03-15', appliedDate: '2026-03-20' }))).toBe('2026-03-20');
    expect(vaccineReferenceDate(vaccine())).toBeNull();
  });

  it('rótulos dos grupos de idade', () => {
    expect(VaccineSchedule.ageGroupLabel(0)).toBe('Ao nascer');
    expect(VaccineSchedule.ageGroupLabel(1)).toBe('1 mês');
    expect(VaccineSchedule.ageGroupLabel(12)).toBe('12 meses');
    expect(VaccineSchedule.ageGroupLabel(null)).toBe('Outras vacinas');
  });
});

describe('MedicationSchedule', () => {
  const start = '2026-11-18T08:00';

  it('8 em 8 horas por 7 dias gera 21 doses e a última não passa do período', () => {
    const times = MedicationSchedule.times(start, 8, 7);
    expect(times).toHaveLength(21);
    expect(times[0]).toBe(start);
    expect(times[times.length - 1]).toBe('2026-11-25T00:00');
  });

  it('intervalo que não divide o dia deixa de fora a dose que passaria do limite', () => {
    // 5 em 5 horas por 1 dia: 08h, 13h, 18h, 23h e 04h do dia seguinte; 09h já passaria do limite.
    const times = MedicationSchedule.times(start, 5, 1);
    expect(times).toHaveLength(5);
    expect(times[times.length - 1]).toBe('2026-11-19T04:00');
  });

  it('1 dia com intervalo de 24 horas gera uma dose', () => {
    expect(MedicationSchedule.times(start, 24, 1)).toEqual([start]);
  });

  it('validação recusa valores fora dos limites', () => {
    expect(MedicationSchedule.validate(8, 7)).toBeNull();
    expect(MedicationSchedule.validate(null, 7)).not.toBeNull();
    expect(MedicationSchedule.validate(0, 7)).not.toBeNull();
    expect(MedicationSchedule.validate(73, 7)).not.toBeNull();
    expect(MedicationSchedule.validate(8, 0)).not.toBeNull();
    expect(MedicationSchedule.validate(8, MedicationSchedule.MAX_DAYS + 1)).not.toBeNull();
    // 1 hora por 30 dias = 720 doses: acima do máximo
    expect(MedicationSchedule.validate(1, 30)).not.toBeNull();
    // 1 hora por 10 dias = 240 doses: no limite
    expect(MedicationSchedule.validate(1, 10)).toBeNull();
  });
});

describe('AttentionSigns', () => {
  const birth = '2026-01-01';
  const event = (
    daysOld: number,
    symptoms: Symptom[] = [],
    temperature: number | null = null,
    intensity: HealthEvent['intensity'] = 'MILD',
  ): HealthEvent => ({
    babyId: 1,
    dateTime: atTime(plusDays(birth, daysOld), '12:00'),
    symptoms,
    temperatureCelsius: temperature,
    intensity,
    soughtCare: false,
  });

  it('sem sinais para sintoma leve em bebê maior', () => {
    expect(AttentionSigns.evaluate(event(120, ['RUNNY_NOSE']), birth)).toEqual([]);
  });

  it('febre antes dos 3 meses é sinal de atenção', () => {
    expect(AttentionSigns.evaluate(event(30, ['FEVER']), birth).some((s) => s.includes('menos de 3 meses'))).toBe(true);
  });

  it('temperatura de febre conta mesmo sem marcar o chip', () => {
    expect(AttentionSigns.evaluate(event(30, [], 37.9), birth).some((s) => s.includes('Febre'))).toBe(true);
  });

  it('temperatura alta em qualquer idade', () => {
    expect(AttentionSigns.evaluate(event(200, [], 38.5), birth)).toEqual(['Temperatura de 38 °C ou mais.']);
  });

  it('temperatura baixa', () => {
    expect(AttentionSigns.evaluate(event(10, [], 35.0), birth).some((s) => s.includes('abaixo de 35,5'))).toBe(true);
  });

  it('dificuldade para respirar', () => {
    expect(AttentionSigns.evaluate(event(300, ['BREATHING_DIFFICULTY']), birth)).toEqual([
      'Dificuldade para respirar.',
    ]);
  });

  it('vômito e diarreia juntos', () => {
    const signs = AttentionSigns.evaluate(event(300, ['VOMITING', 'DIARRHEA']), birth);
    expect(signs).toHaveLength(1);
    expect(signs[0]).toContain('desidratação');
  });

  it('vômito forte sozinho', () => {
    const signs = AttentionSigns.evaluate(event(300, ['VOMITING'], null, 'STRONG'), birth);
    expect(signs).toHaveLength(1);
    expect(signs[0]).toContain('intensos');
  });

  it('intensidade forte sem outro sinal', () => {
    expect(AttentionSigns.evaluate(event(300, ['COLIC'], null, 'STRONG'), birth)).toEqual([
      'Mal-estar com intensidade forte.',
    ]);
  });

  it('recusa alimentar em recém-nascido', () => {
    expect(AttentionSigns.evaluate(event(20, ['FEEDING_REFUSAL']), birth).some((s) => s.includes('Recusa'))).toBe(true);
    expect(AttentionSigns.evaluate(event(200, ['FEEDING_REFUSAL']), birth)).toEqual([]);
  });

  it('mensagens nunca usam linguagem de diagnóstico', () => {
    const all = [
      ...AttentionSigns.evaluate(event(10, [...SYMPTOMS], 39.0, 'STRONG'), birth),
      ...AttentionSigns.evaluate(event(10, [], 35.0), birth),
      Texts.ATTENTION_ADVICE,
    ];
    for (const text of all) {
      for (const word of ['diagnóstico', 'grave', 'anormal']) {
        expect(text.toLowerCase(), `'${word}' em: ${text}`).not.toContain(word);
      }
    }
  });
});

describe('FeedingSummaries', () => {
  const day = '2026-11-17';
  const at = (hour: number, d = day) => atTime(d, `${String(hour).padStart(2, '0')}:00`);
  const feeding = (f: Partial<Feeding> & Pick<Feeding, 'dateTime' | 'type'>): Feeding => ({
    babyId: 1,
    firstTime: false,
    ...f,
  });

  it('resume mamadas, fórmula, água e refeições', () => {
    const s = FeedingSummaries.summarize([
      feeding({ dateTime: at(6), type: 'BREAST_MILK', breastSide: 'LEFT', durationMinutes: 15 }),
      feeding({ dateTime: at(9), type: 'BREAST_MILK', durationMinutes: 10 }),
      feeding({ dateTime: at(12), type: 'FORMULA', amountMl: 90 }),
      feeding({ dateTime: at(15), type: 'FORMULA', amountMl: 120 }),
      feeding({ dateTime: at(16), type: 'WATER', amountMl: 30 }),
      feeding({ dateTime: at(17), type: 'FRUIT', food: ' Banana ', firstTime: true, acceptance: 'GOOD' }),
      feeding({ dateTime: at(18), type: 'SOLID_MEAL', food: 'banana', firstTime: true }),
    ]);
    expect(s.total).toBe(7);
    expect(s.breastfeedings).toBe(2);
    expect(s.breastMinutes).toBe(25);
    expect(s.formulaCount).toBe(2);
    expect(s.formulaMl).toBe(210);
    expect(s.waterMl).toBe(30);
    expect(s.solidMeals).toBe(2);
    expect(s.newFoods).toEqual(['Banana']);
  });

  it('dia vazio', () => {
    expect(FeedingSummaries.isEmpty(FeedingSummaries.summarize([]))).toBe(true);
  });

  it('agrupa por dia em ordem', () => {
    const days = FeedingSummaries.byDay([
      feeding({ dateTime: at(8, plusDays(day, 1)), type: 'FORMULA', amountMl: 100 }),
      feeding({ dateTime: at(8), type: 'FORMULA', amountMl: 50 }),
    ]);
    expect(days.map(([d]) => d)).toEqual([day, plusDays(day, 1)]);
    expect(days[0][1].formulaMl).toBe(50);
  });
});

describe('Plausibility', () => {
  it('peso em gramas pede confirmação', () => {
    expect(Plausibility.measurementWarning('WEIGHT', 3450.0)).not.toBeNull();
    expect(Plausibility.measurementWarning('WEIGHT', 3.45)).toBeNull();
  });

  it('comprimentos fora da faixa', () => {
    expect(Plausibility.measurementWarning('HEIGHT', 5.0)).not.toBeNull();
    expect(Plausibility.measurementWarning('HEIGHT', 52.0)).toBeNull();
    expect(Plausibility.measurementWarning('HEAD_CIRCUMFERENCE', 80.0)).not.toBeNull();
    expect(Plausibility.measurementWarning('HEAD_CIRCUMFERENCE', 35.0)).toBeNull();
  });

  it('temperatura incomum', () => {
    expect(Plausibility.temperatureWarning(378.0)).not.toBeNull();
    expect(Plausibility.temperatureWarning(37.8)).toBeNull();
  });
});

describe('CalendarGrid', () => {
  it('novembro de 2026 começa no domingo', () => {
    const cells = CalendarGrid.cells({ year: 2026, month: 11 });
    expect(cells[0]).toBe('2026-11-01');
    expect(cells.length % 7).toBe(0);
    expect(cells.filter((c) => c !== null)).toHaveLength(30);
  });

  it('preenche o início até o dia da semana certo', () => {
    // 1º de outubro de 2026 é quinta-feira: 4 células vazias antes (D, S, T, Q).
    const cells = CalendarGrid.cells({ year: 2026, month: 10 });
    expect(cells.findIndex((c) => c !== null)).toBe(4);
    expect(cells[4]).toBe('2026-10-01');
    expect(cells).toHaveLength(35);
  });

  it('semana começando na segunda-feira', () => {
    const cells = CalendarGrid.cells({ year: 2026, month: 10 }, 1);
    expect(cells.findIndex((c) => c !== null)).toBe(3);
  });

  it('iniciais dos dias', () => {
    expect(CalendarGrid.weekdayInitials()).toEqual(['D', 'S', 'T', 'Q', 'Q', 'S', 'S']);
    expect(CalendarGrid.weekdayNames(1)[0]).toBe('segunda-feira');
  });

  it('marcadores por dia sem repetir tipo', () => {
    const day = '2026-11-17';
    const items: TimelineItem[] = [
      { type: 'FEEDING', id: 1, date: day, time: '12:00', title: 'a' },
      { type: 'FEEDING', id: 2, date: day, time: '00:00', title: 'b' },
      { type: 'VACCINE', id: 3, date: day, time: null, title: 'c' },
      { type: 'MOMENT', id: 4, date: plusDays(day, 1), time: null, title: 'd' },
    ];
    const markers = CalendarGrid.markersByDay(items);
    expect(markers.get(day)).toEqual(['VACCINE', 'FEEDING']);
    expect(markers.get(plusDays(day, 1))).toEqual(['MOMENT']);
  });
});

describe('Formats', () => {
  it('datas no padrão brasileiro', () => {
    const d = '2026-11-17';
    expect(Formats.date(d)).toBe('17/11/2026');
    expect(Formats.shortDate(d)).toBe('17/11');
    expect(Formats.longDate(d)).toBe('Terça-feira, 17 de novembro');
    expect(Formats.monthYear({ year: 2026, month: 11 })).toBe('Novembro de 2026');
    expect(Formats.dateTime('2026-11-17T08:05')).toBe('17/11/2026 08:05');
  });

  it('números com vírgula', () => {
    expect(Formats.measurement('WEIGHT', 3.45)).toBe('3,450 kg');
    expect(Formats.measurement('HEIGHT', 52.0)).toBe('52,0 cm');
    expect(Formats.temperature(37.8)).toBe('37,8 °C');
  });

  it('leitura de números digitados', () => {
    expect(Formats.parseDecimal('3,45')).toBeCloseTo(3.45, 4);
    expect(Formats.parseDecimal(' 3.45 ')).toBeCloseTo(3.45, 4);
    expect(Formats.parseDecimal('')).toBeNull();
    expect(Formats.parseDecimal('abc')).toBeNull();
    expect(Formats.parseInt('120')).toBe(120);
    expect(Formats.parseInt('')).toBeNull();
  });

  it('plural simples', () => {
    expect(plural(1, 'dia', 'dias')).toBe('1 dia');
    expect(plural(2, 'dia', 'dias')).toBe('2 dias');
  });
});

describe('Datas (equivalentes aos conversores e ao java.time)', () => {
  it('soma de meses limita ao último dia do mês', () => {
    expect(plusMonths('2026-01-31', 1)).toBe('2026-02-28');
    expect(plusMonths('2028-01-31', 1)).toBe('2028-02-29');
    expect(plusMonths('2026-11-30', 3)).toBe('2027-02-28');
  });

  it('ida e volta de datas e horas, e ordem preservada', () => {
    expect(dateOf('2026-02-28T23:59')).toBe('2026-02-28');
    expect(plusHours('2026-02-28T23:00', 2)).toBe('2026-03-01T01:00');
    expect('2026-01-01T23:59' < '2026-01-02T00:00').toBe(true);
  });
});

describe('Linha do tempo', () => {
  it('descrição da alimentação', () => {
    const f: Feeding = {
      id: 1,
      babyId: 1,
      dateTime: '2026-11-17T09:00',
      type: 'BREAST_MILK',
      breastSide: 'RIGHT',
      durationMinutes: 12,
      firstTime: false,
    };
    expect(describeFeeding(f)).toBe('lado direito · 12 min');
    expect(feedingToTimelineItem(f).time).toBe('09:00');
  });

  it('ordem cronológica com registros só de data primeiro', () => {
    const day = '2026-11-17';
    const items = sortedChronologically([
      { type: 'FEEDING', id: 1, date: day, time: '10:00', title: 'b' },
      { type: 'GROWTH', id: 2, date: day, time: null, title: 'a' },
      { type: 'MOMENT', id: 3, date: minusDays(day, 1), time: '23:00', title: 'z' },
    ]);
    expect(items.map((i) => i.id)).toEqual([3, 2, 1]);
  });
});

describe('Relatório', () => {
  const baby: Baby = { id: 1, name: 'Bebê Teste', birthDate: '2026-09-01', colorIndex: 0 };
  const start = '2026-11-01';
  const end = '2026-11-30';
  const now = '2026-12-01T10:00';

  const data: ReportData = {
    ...emptyReportData(),
    measurements: [
      { id: 1, babyId: 1, date: '2026-11-05', type: 'WEIGHT', value: 4.2 },
      { id: 2, babyId: 1, date: '2026-11-20', type: 'WEIGHT', value: 4.6 },
      // Fora do período e de outro bebê: não podem aparecer.
      { id: 3, babyId: 1, date: '2026-10-31', type: 'WEIGHT', value: 4.0 },
      { id: 4, babyId: 2, date: '2026-11-10', type: 'WEIGHT', value: 9.9 },
    ],
    vaccines: [
      {
        id: 10,
        babyId: 1,
        name: 'Rotavírus',
        dose: '1ª dose',
        scheduledDate: '2026-11-01',
        appliedDate: '2026-11-03',
        status: 'APPLIED',
        lot: 'L123',
      },
      { id: 11, babyId: 1, name: 'Meningocócica C', scheduledDate: '2026-12-01', status: 'SCHEDULED' },
    ],
    appointments: [
      {
        id: 20,
        babyId: 1,
        title: 'Pediatra',
        dateTime: '2026-11-30T23:30',
        status: 'SCHEDULED',
        guidance: 'Manter aleitamento',
      },
      { id: 21, babyId: 1, title: 'Retorno', dateTime: '2026-12-01T00:00', status: 'SCHEDULED' },
    ],
    healthEvents: [
      {
        id: 30,
        babyId: 1,
        dateTime: '2026-11-12T03:00',
        symptoms: ['COLIC'],
        intensity: 'MILD',
        soughtCare: false,
      },
    ],
    feedings: [
      { id: 40, babyId: 1, dateTime: '2026-11-12T06:00', type: 'BREAST_MILK', durationMinutes: 15, firstTime: false },
      { id: 41, babyId: 1, dateTime: '2026-11-12T09:00', type: 'FORMULA', amountMl: 90, firstTime: false },
    ],
    moments: [{ id: 50, babyId: 1, dateTime: '2026-11-02T15:00', title: 'Primeiro sorriso' }],
    photos: { [recordKey('MOMENT', 50)]: [900] },
  };

  const ALL = ['GROWTH', 'VACCINE', 'APPOINTMENT', 'HEALTH_EVENT', 'MEDICATION', 'FEEDING', 'MOMENT'] as const;
  const request = (types: (typeof ALL)[number][] = [...ALL], includePhotos = false) => ({
    baby,
    start,
    end,
    types,
    includePhotos,
  });

  const allText = (doc: ReportDocument): string => {
    const out: string[] = [doc.title, ...doc.headerLines];
    for (const section of doc.sections) {
      out.push(section.title);
      for (const block of section.blocks) {
        if (block.kind === 'paragraph') out.push(block.text);
        else if (block.kind === 'table') [block.headers, ...block.rows].forEach((r) => out.push(r.join(' | ')));
        else out.push(block.heading, ...block.lines);
      }
    }
    return out.join('\n');
  };

  it('filtra por bebê e período, inclusive nas bordas', () => {
    const filtered = filterReportData(request(), data);
    expect(filtered.measurements.map((m) => m.id)).toEqual([1, 2]);
    expect(filtered.vaccines.map((v) => v.id)).toEqual([10]);
    // 30/11 às 23:30 entra; 01/12 à meia-noite fica de fora.
    expect(filtered.appointments.map((a) => a.id)).toEqual([20]);
    expect(filtered.healthEvents).toHaveLength(1);
    expect(filtered.feedings).toHaveLength(2);
    expect(filtered.moments).toHaveLength(1);
  });

  it('documento contém cabeçalho, resumo e rodapé de aviso', () => {
    const doc = buildReport(request(), data, now);
    expect(doc.footer).toBe(Texts.REPORT_FOOTER);
    expect(doc.headerLines).toContain('Período: 01/11/2026 a 30/11/2026');
    expect(doc.headerLines).toContain('Bebê: Bebê Teste');
    expect(doc.sections[0].title).toBe('Resumo do período');
    const summary = doc.sections[0].blocks[0];
    if (summary.kind !== 'table') throw new Error('Resumo deveria ser tabela');
    expect(summary.rows).toContainEqual(['Crescimento', '2']);
    expect(summary.rows).toContainEqual(['Vacinas', '1']);
  });

  it('nada fora do período aparece no texto', () => {
    const text = allText(buildReport(request(), data, now));
    expect(text).toContain('4,200 kg');
    expect(text).toContain('4,600 kg');
    expect(text).not.toContain('4,000 kg');
    expect(text).not.toContain('9,900 kg');
    expect(text).not.toContain('Retorno');
    expect(text).not.toContain('Meningocócica');
    expect(text).toContain('Lote L123');
    expect(text).toContain('Orientações recebidas: Manter aleitamento');
    expect(text).toContain('Último registro de peso no período: 4,600 kg em 20/11/2026.');
  });

  it('só inclui as seções escolhidas', () => {
    const doc = buildReport(request(['VACCINE', 'MOMENT']), data, now);
    expect(doc.sections.map((s) => s.title)).toEqual(['Resumo do período', 'Vacinas', 'Momentos']);
  });

  it('seção vazia explica que não há registros', () => {
    const empty = buildReport(request(['HEALTH_EVENT']), emptyReportData(), now);
    const section = empty.sections[empty.sections.length - 1];
    expect(section.blocks).toEqual([{ kind: 'paragraph', text: Texts.EMPTY_PERIOD }]);
  });

  it('fotos só entram quando pedidas', () => {
    const momentPhotos = (doc: ReportDocument) =>
      doc.sections
        .find((s) => s.title === 'Momentos')!
        .blocks.flatMap((b) => (b.kind === 'entry' ? b.photos : []));
    expect(momentPhotos(buildReport(request(ALL.slice(), false), data, now))).toEqual([]);
    expect(momentPhotos(buildReport(request(ALL.slice(), true), data, now))).toEqual([900]);
  });

  it('alimentação resumida por dia', () => {
    const doc = buildReport(request(['FEEDING']), data, now);
    const table = doc.sections[doc.sections.length - 1].blocks[0];
    if (table.kind !== 'table') throw new Error('Esperava tabela');
    expect(table.rows).toEqual([['12/11/2026', '1 (15)', '90', '-', '-', '-']]);
  });

  it('período invertido é rejeitado', () => {
    expect(() => buildReport({ ...request(['GROWTH']), start: end, end: start }, data, now)).toThrow();
  });
});

describe('Medicamentos no relatório', () => {
  const baby: Baby = { id: 1, name: 'Bebê Teste', birthDate: '2026-09-01', colorIndex: 0 };
  const now = '2026-11-18T12:00';

  const dose = (
    id: number,
    hour: number,
    status: MedicationDose['status'] = 'SCHEDULED',
    babyId = 1,
    givenAt: string | null = null,
  ): MedicationDose => ({
    id,
    babyId,
    name: `Remédio ${id}`,
    dosage: '5 ml',
    scheduledAt: `2026-11-18T${String(hour).padStart(2, '0')}:00`,
    status,
    givenAt,
  });

  it('status exibido distingue realizado, não realizado, não registrado e agendado', () => {
    expect(medicationStatusText(dose(1, 8, 'GIVEN'), now)).toBe('Realizado');
    expect(medicationStatusText(dose(2, 9, 'SKIPPED'), now)).toBe('Não realizado');
    expect(medicationStatusText(dose(3, 10), now)).toBe('Não registrado');
    expect(medicationStatusText(dose(4, 20), now)).toBe('Agendado');
  });

  it('relatório lista medicamentos do bebê com a situação de cada dose', () => {
    const data: ReportData = {
      ...emptyReportData(),
      medications: [
        dose(1, 8, 'GIVEN', 1, '2026-11-18T08:20'),
        dose(2, 10, 'SKIPPED'),
        dose(3, 11),
        dose(4, 20),
        dose(5, 9, 'SCHEDULED', 2),
      ],
    };
    const doc = buildReport({ baby, start: '2026-11-01', end: '2026-11-30', types: ['MEDICATION'], includePhotos: false }, data, now);
    const section = doc.sections.find((s) => s.title === recordTypePluralLabel.MEDICATION)!;
    const entries = section.blocks.flatMap((b) => (b.kind === 'entry' ? [b] : []));
    expect(entries).toHaveLength(4);
    expect(entries.map((e) => e.lines.find((l) => l.startsWith('Situação')))).toEqual([
      'Situação: Realizado',
      'Situação: Não realizado',
      'Situação: Não registrado',
      'Situação: Agendado',
    ]);
    expect(entries.some((e) => e.heading.includes('Remédio 5'))).toBe(false);
    expect(entries[0].lines.some((l) => l.startsWith('Horário previsto'))).toBe(true);
  });
});
