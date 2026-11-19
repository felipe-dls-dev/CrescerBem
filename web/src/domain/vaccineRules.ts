import { isAfter, isBefore, minusDays, plusMonths } from './dates';
import type { Baby, LocalDate, Vaccine } from './types';

export interface SuggestedVaccine {
  name: string;
  dose: string;
  ageMonths: number;
}

const s = (name: string, dose: string, ageMonths: number): SuggestedVaccine => ({ name, dose, ageMonths });

/**
 * Calendário sugerido (referência educativa, editável pelo responsável).
 * Baseado no calendário nacional para os primeiros 15 meses. A caderneta e o pediatra
 * são a referência oficial; ver Texts.VACCINE_NOTE.
 */
export const VaccineSchedule = {
  suggestions: [
    s('BCG', 'Dose única', 0),
    s('Hepatite B', 'Dose ao nascer', 0),
    s('Penta (DTP/Hib/Hepatite B)', '1ª dose', 2),
    s('VIP (poliomielite inativada)', '1ª dose', 2),
    s('Pneumocócica 10-valente', '1ª dose', 2),
    s('Rotavírus', '1ª dose', 2),
    s('Meningocócica C', '1ª dose', 3),
    s('Penta (DTP/Hib/Hepatite B)', '2ª dose', 4),
    s('VIP (poliomielite inativada)', '2ª dose', 4),
    s('Pneumocócica 10-valente', '2ª dose', 4),
    s('Rotavírus', '2ª dose', 4),
    s('Meningocócica C', '2ª dose', 5),
    s('Penta (DTP/Hib/Hepatite B)', '3ª dose', 6),
    s('VIP (poliomielite inativada)', '3ª dose', 6),
    s('Febre amarela', '1 dose', 9),
    s('Tríplice viral (sarampo, caxumba, rubéola)', '1ª dose', 12),
    s('Pneumocócica 10-valente', 'Reforço', 12),
    s('Meningocócica C', 'Reforço', 12),
    s('DTP (tríplice bacteriana)', '1º reforço', 15),
    s('Hepatite A', 'Dose única', 15),
    s('Tetra viral (sarampo, caxumba, rubéola, varicela)', 'Dose única', 15),
  ] as SuggestedVaccine[],

  /** Vacinas previstas para o bebê, com data calculada a partir do nascimento. */
  suggestedFor(baby: Baby): Vaccine[] {
    return VaccineSchedule.suggestions.map((it) => ({
      babyId: baby.id ?? 0,
      name: it.name,
      dose: it.dose,
      recommendedAgeMonths: it.ageMonths,
      scheduledDate: plusMonths(baby.birthDate, it.ageMonths),
      status: 'SCHEDULED' as const,
    }));
  },

  /** Remove sugestões que já existem (mesmo nome e dose), para não duplicar. */
  missingFor(baby: Baby, existing: Vaccine[]): Vaccine[] {
    const keys = new Set(existing.map((v) => key(v.name, v.dose)));
    return VaccineSchedule.suggestedFor(baby).filter((v) => !keys.has(key(v.name, v.dose)));
  },

  ageGroupLabel(months: number | null | undefined): string {
    if (months === null || months === undefined) return 'Outras vacinas';
    if (months === 0) return 'Ao nascer';
    if (months === 1) return '1 mês';
    return `${months} meses`;
  },
};

const key = (name: string, dose: string | null | undefined): string =>
  `${name.trim().toLowerCase()}|${(dose ?? '').trim().toLowerCase()}`;

export type VaccineDisplayStatus = 'APPLIED' | 'PLANNED' | 'PENDING' | 'OVERDUE' | 'NOT_APPLICABLE';

export const vaccineDisplayLabel: Record<VaccineDisplayStatus, string> = {
  APPLIED: 'Aplicada',
  PLANNED: 'Prevista',
  PENDING: 'Pendente',
  OVERDUE: 'Atrasada',
  NOT_APPLICABLE: 'Não se aplica',
};

/** Dias depois da data prevista a partir dos quais a vacina aparece como atrasada. */
export const VACCINE_OVERDUE_AFTER_DAYS = 30;

/** Data usada no calendário: a da aplicação ou, se ainda não aplicada, a prevista. */
export const vaccineReferenceDate = (v: Vaccine): LocalDate | null => v.appliedDate ?? v.scheduledDate ?? null;

export function vaccineDisplayStatus(v: Vaccine, today: LocalDate): VaccineDisplayStatus {
  switch (v.status) {
    case 'APPLIED':
      return 'APPLIED';
    case 'NOT_APPLICABLE':
      return 'NOT_APPLICABLE';
    case 'SCHEDULED': {
      const date = v.scheduledDate;
      if (!date || isAfter(date, today)) return 'PLANNED';
      if (isBefore(date, minusDays(today, VACCINE_OVERDUE_AFTER_DAYS))) return 'OVERDUE';
      return 'PENDING';
    }
  }
}

export const vaccineTitle = (v: Vaccine): string => (v.dose && v.dose.trim() ? `${v.name} · ${v.dose}` : v.name);
