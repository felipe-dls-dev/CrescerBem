// Regras do agendamento repetido de medicamentos: de tantas em tantas horas, por N dias.

import { isBeforeTime } from './timeCompare';
import { plusDaysTime, plusHours } from './dates';
import type { LocalDateTime } from './types';

export const MedicationSchedule = {
  MAX_DAYS: 30,
  MAX_INTERVAL_HOURS: 72,
  MAX_DOSES: 240,

  /**
   * Horários das doses: a primeira em [start] e as seguintes a cada [intervalHours],
   * enquanto couberem nos [days] dias a partir de [start] (o fim do período não entra).
   * Ex.: 8 em 8 horas por 7 dias = 21 doses.
   */
  times(start: LocalDateTime, intervalHours: number, days: number): LocalDateTime[] {
    if (!(intervalHours >= 1 && days >= 1)) throw new Error('Intervalo e dias precisam ser positivos.');
    const end = plusDaysTime(start, days);
    const result: LocalDateTime[] = [];
    for (let t = start; isBeforeTime(t, end); t = plusHours(t, intervalHours)) result.push(t);
    return result;
  },

  /** Mensagem de erro para os valores digitados, ou null se estiverem válidos. */
  validate(intervalHours: number | null, days: number | null): string | null {
    if (intervalHours === null || intervalHours < 1 || intervalHours > MedicationSchedule.MAX_INTERVAL_HOURS) {
      return `Informe o intervalo em horas (de 1 a ${MedicationSchedule.MAX_INTERVAL_HOURS}).`;
    }
    if (days === null || days < 1 || days > MedicationSchedule.MAX_DAYS) {
      return `Informe por quantos dias repetir (de 1 a ${MedicationSchedule.MAX_DAYS}).`;
    }
    if (Math.floor((days * 24 + intervalHours - 1) / intervalHours) > MedicationSchedule.MAX_DOSES) {
      return `São doses demais para agendar de uma vez (máximo ${MedicationSchedule.MAX_DOSES}). Aumente o intervalo ou reduza os dias.`;
    }
    return null;
  },
};
