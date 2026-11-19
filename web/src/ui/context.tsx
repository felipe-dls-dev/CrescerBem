import { createContext, useContext } from 'react';
import type { Baby, LocalDate } from '../domain/types';

export interface BabyContextValue {
  /** Bebê selecionado no topo do app (ou null se ainda não há nenhum). */
  baby: Baby | null;
  babies: Baby[];
  /** Abre a folha "Registrar", opcionalmente já com uma data escolhida. */
  openPicker: (date?: LocalDate) => void;
}

export const BabyContext = createContext<BabyContextValue>({ baby: null, babies: [], openPicker: () => {} });

export const useBabyContext = (): BabyContextValue => useContext(BabyContext);
