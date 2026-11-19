import { dateOf } from './dates';
import { isSolid } from './labels';
import type { Feeding, LocalDate } from './types';

export interface FeedingDaySummary {
  total: number;
  breastfeedings: number;
  breastMinutes: number;
  formulaCount: number;
  formulaMl: number;
  waterMl: number;
  solidMeals: number;
  newFoods: string[];
}

export const emptyFeedingSummary = (): FeedingDaySummary => ({
  total: 0,
  breastfeedings: 0,
  breastMinutes: 0,
  formulaCount: 0,
  formulaMl: 0,
  waterMl: 0,
  solidMeals: 0,
  newFoods: [],
});

const sum = (items: Feeding[], pick: (f: Feeding) => number | null | undefined) =>
  items.reduce((total, f) => total + (pick(f) ?? 0), 0);

export const FeedingSummaries = {
  summarize(feedings: Feeding[]): FeedingDaySummary {
    const breast = feedings.filter((f) => f.type === 'BREAST_MILK');
    const formula = feedings.filter((f) => f.type === 'FORMULA');
    const seen = new Set<string>();
    const newFoods: string[] = [];
    for (const f of feedings) {
      const food = f.food?.trim();
      if (f.firstTime && food && !seen.has(food.toLowerCase())) {
        seen.add(food.toLowerCase());
        newFoods.push(food);
      }
    }
    return {
      total: feedings.length,
      breastfeedings: breast.length,
      breastMinutes: sum(breast, (f) => f.durationMinutes),
      formulaCount: formula.length,
      formulaMl: sum(formula, (f) => f.amountMl),
      waterMl: sum(
        feedings.filter((f) => f.type === 'WATER'),
        (f) => f.amountMl,
      ),
      solidMeals: feedings.filter((f) => isSolid(f.type)).length,
      newFoods,
    };
  },

  isEmpty: (summary: FeedingDaySummary): boolean => summary.total === 0,

  /** Resumo por dia, em ordem cronológica. */
  byDay(feedings: Feeding[]): [LocalDate, FeedingDaySummary][] {
    const groups = new Map<LocalDate, Feeding[]>();
    for (const f of feedings) {
      const day = dateOf(f.dateTime);
      if (!groups.has(day)) groups.set(day, []);
      groups.get(day)!.push(f);
    }
    return [...groups.keys()].sort().map((day) => [day, FeedingSummaries.summarize(groups.get(day)!)]);
  },
};
