import { atDay, dayOfWeek, lengthOfMonth, type YearMonth } from './dates';
import { WEEKDAY_NAMES } from './formats';
import type { TimelineItem } from './timeline';
import { recordTypeOrdinal, type LocalDate, type RecordType } from './types';

/** Dia da semana: 0 = domingo ... 6 = sábado. */
export type FirstDayOfWeek = number;

const INITIALS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];

/** Grade mensal do calendário, começando no domingo (padrão brasileiro). */
export const CalendarGrid = {
  /** Células da grade: null preenche o começo e o fim para completar semanas de 7 dias. */
  cells(month: YearMonth, firstDayOfWeek: FirstDayOfWeek = 0): (LocalDate | null)[] {
    const first = atDay(month, 1);
    const leading = (dayOfWeek(first) - firstDayOfWeek + 7) % 7;
    const days: (LocalDate | null)[] = Array.from({ length: lengthOfMonth(month.year, month.month) }, (_, i) =>
      atDay(month, i + 1),
    );
    const withLeading: (LocalDate | null)[] = [...Array<null>(leading).fill(null), ...days];
    const trailing = (7 - (withLeading.length % 7)) % 7;
    return [...withLeading, ...Array<null>(trailing).fill(null)];
  },

  weekdayInitials(firstDayOfWeek: FirstDayOfWeek = 0): string[] {
    return Array.from({ length: 7 }, (_, i) => INITIALS[(firstDayOfWeek + i) % 7]);
  },

  weekdayNames(firstDayOfWeek: FirstDayOfWeek = 0): string[] {
    return Array.from({ length: 7 }, (_, i) => WEEKDAY_NAMES[(firstDayOfWeek + i) % 7]);
  },

  /** Tipos de registro presentes em cada dia (sem repetir, na ordem dos tipos), para os marcadores. */
  markersByDay(items: TimelineItem[]): Map<LocalDate, RecordType[]> {
    const result = new Map<LocalDate, Set<RecordType>>();
    for (const item of items) {
      if (!result.has(item.date)) result.set(item.date, new Set());
      result.get(item.date)!.add(item.type);
    }
    return new Map(
      [...result].map(([day, types]) => [day, [...types].sort((a, b) => recordTypeOrdinal(a) - recordTypeOrdinal(b))]),
    );
  },
};
