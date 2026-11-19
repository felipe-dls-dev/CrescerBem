// Consultas reativas: a tela se atualiza sozinha quando os dados do IndexedDB mudam
// (equivale aos Flows do Room no app Android).

import { liveQuery } from 'dexie';
import { useEffect, useState } from 'react';

export interface LiveState<T> {
  loading: boolean;
  value: T | undefined;
  error: unknown;
}

export function useLive<T>(querier: () => Promise<T> | T, deps: readonly unknown[]): LiveState<T> {
  const [state, setState] = useState<LiveState<T>>({ loading: true, value: undefined, error: undefined });

  useEffect(() => {
    setState((s) => (s.loading ? s : { ...s, loading: true }));
    const subscription = liveQuery(querier).subscribe({
      next: (value) => setState({ loading: false, value, error: undefined }),
      error: (error) => setState({ loading: false, value: undefined, error }),
    });
    return () => subscription.unsubscribe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}
