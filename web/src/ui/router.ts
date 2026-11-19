// Roteamento por hash (#/hoje, #/form/MOMENT?id=3). Funciona em qualquer hospedagem estática,
// inclusive GitHub Pages, sem configuração de servidor.

import { useEffect, useState } from 'react';
import { RECORD_TYPES, type LocalDate, type RecordType } from '../domain/types';

export const TABS = [
  { path: 'hoje', label: 'Hoje', icon: 'today' },
  { path: 'calendario', label: 'Calendário', icon: 'calendar' },
  { path: 'bebe', label: 'Bebê', icon: 'baby' },
  { path: 'saude', label: 'Saúde', icon: 'health' },
  { path: 'alimentacao', label: 'Alimentação', icon: 'food' },
] as const;

export type TabPath = (typeof TABS)[number]['path'];

export interface Route {
  path: string;
  params: URLSearchParams;
}

function parse(hash: string): Route {
  const raw = hash.replace(/^#\/?/, '');
  const [path, query = ''] = raw.split('?');
  return { path: path || 'hoje', params: new URLSearchParams(query) };
}

let navigatedInApp = false;

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() => parse(window.location.hash));
  useEffect(() => {
    const onChange = () => {
      navigatedInApp = true;
      setRoute(parse(window.location.hash));
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}

export function navigate(to: string): void {
  window.location.hash = to.startsWith('#') ? to : `#/${to}`;
}

/** Volta à tela anterior; se o app foi aberto direto num formulário, vai para Hoje. */
export function goBack(): void {
  if (navigatedInApp) window.history.back();
  else navigate('hoje');
}

export interface FormTarget {
  type: RecordType;
  id?: number;
  date?: LocalDate;
  preset?: string;
}

export const formPath = ({ type, id, date, preset }: FormTarget): string => {
  const params = new URLSearchParams();
  if (id !== undefined) params.set('id', String(id));
  if (date) params.set('date', date);
  if (preset) params.set('preset', preset);
  const query = params.toString();
  return `form/${type}${query ? `?${query}` : ''}`;
};

export function parseFormRoute(route: Route): FormTarget {
  const type = route.path.split('/')[1] as RecordType;
  const id = Number(route.params.get('id'));
  const date = route.params.get('date');
  return {
    type: RECORD_TYPES.includes(type) ? type : 'MOMENT',
    id: Number.isInteger(id) && id > 0 ? id : undefined,
    date: date && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : undefined,
    preset: route.params.get('preset') ?? undefined,
  };
}
