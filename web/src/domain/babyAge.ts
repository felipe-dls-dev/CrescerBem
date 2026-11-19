// Idade do bebê descrita do jeito que pais e pediatras costumam falar.

import { daysBetween, isBefore, periodBetween } from './dates';
import { Formats, plural } from './formats';
import type { LocalDate } from './types';

const join = (first: string, second: string | null): string => (second === null ? first : `${first} e ${second}`);

export const BabyAge = {
  days: (birth: LocalDate, on: LocalDate): number => daysBetween(birth, on),

  months(birth: LocalDate, on: LocalDate): number {
    if (isBefore(on, birth)) return 0;
    const p = periodBetween(birth, on);
    return p.years * 12 + p.months;
  },

  /**
   * - até 13 dias: "5 dias";
   * - até 59 dias: semanas e dias ("3 semanas e 2 dias");
   * - até 1 ano: meses e dias ("4 meses e 10 dias");
   * - depois: anos e meses ("1 ano e 2 meses").
   */
  describe(birth: LocalDate, on: LocalDate): string {
    if (isBefore(on, birth)) return `Nascimento previsto para ${Formats.date(birth)}`;
    const totalDays = daysBetween(birth, on);
    if (totalDays === 0) return 'Nasceu hoje';
    if (totalDays < 14) return plural(totalDays, 'dia', 'dias');
    if (totalDays < 60) {
      const weeks = Math.floor(totalDays / 7);
      const rest = totalDays % 7;
      return join(plural(weeks, 'semana', 'semanas'), rest > 0 ? plural(rest, 'dia', 'dias') : null);
    }
    const period = periodBetween(birth, on);
    if (period.years === 0) {
      return join(plural(period.months, 'mês', 'meses'), period.days > 0 ? plural(period.days, 'dia', 'dias') : null);
    }
    return join(plural(period.years, 'ano', 'anos'), period.months > 0 ? plural(period.months, 'mês', 'meses') : null);
  },

  /** Frase completa para o cabeçalho de Hoje: "Ana tem 3 semanas", "Ana nasceu hoje". */
  sentence(name: string, birth: LocalDate, on: LocalDate): string {
    if (isBefore(on, birth)) return BabyAge.describe(birth, on);
    if (on === birth) return `${name} nasceu hoje`;
    return `${name} tem ${BabyAge.describe(birth, on)}`;
  },
};
