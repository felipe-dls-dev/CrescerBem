// Tema do Crescer Bem na web: mesma paleta do app Android (ui/theme/Color.kt).
// Cada bebê tem uma cor; só a cor principal muda, o resto da paleta é fixo.

import { useEffect, useSyncExternalStore } from 'react';
import { Prefs, subscribePrefs, type ThemeMode } from '../data/prefs';
import type { RecordType } from '../domain/types';

export interface BabyPaletteEntry {
  label: string;
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  primaryDark: string;
  onPrimaryDark: string;
  primaryContainerDark: string;
  onPrimaryContainerDark: string;
}

/** 8 tons curados com contraste validado. O índice fica gravado no bebê (colorIndex). */
export const BABY_PALETTE: BabyPaletteEntry[] = [
  { label: 'Verde-menta', primary: '#2F6F6A', onPrimary: '#FFFFFF', primaryContainer: '#CFEDE8', onPrimaryContainer: '#00201D', primaryDark: '#96D3CB', onPrimaryDark: '#00201D', primaryContainerDark: '#1E504C', onPrimaryContainerDark: '#CFEDE8' },
  { label: 'Azul', primary: '#2A5FA5', onPrimary: '#FFFFFF', primaryContainer: '#D3E4FF', onPrimaryContainer: '#001B3D', primaryDark: '#9AC4F8', onPrimaryDark: '#003062', primaryContainerDark: '#08458A', onPrimaryContainerDark: '#D3E4FF' },
  { label: 'Lilás', primary: '#63559A', onPrimary: '#FFFFFF', primaryContainer: '#E7DEFF', onPrimaryContainer: '#1F1147', primaryDark: '#CBBEFF', onPrimaryDark: '#1F1147', primaryContainerDark: '#4B3D80', onPrimaryContainerDark: '#E7DEFF' },
  { label: 'Rosa', primary: '#9C4A6B', onPrimary: '#FFFFFF', primaryContainer: '#FFD8E6', onPrimaryContainer: '#3D0024', primaryDark: '#F4B8D0', onPrimaryDark: '#5C1133', primaryContainerDark: '#7B2F51', onPrimaryContainerDark: '#FFD8E6' },
  { label: 'Amarelo', primary: '#7A6000', onPrimary: '#FFFFFF', primaryContainer: '#FFE07A', onPrimaryContainer: '#261A00', primaryDark: '#EDD56A', onPrimaryDark: '#3D2F00', primaryContainerDark: '#5C4700', onPrimaryContainerDark: '#FFE07A' },
  { label: 'Pêssego', primary: '#9A4A38', onPrimary: '#FFFFFF', primaryContainer: '#FFDAD1', onPrimaryContainer: '#3A0A00', primaryDark: '#FFB4A2', onPrimaryDark: '#3A0A00', primaryContainerDark: '#7A3626', onPrimaryContainerDark: '#FFDAD1' },
  { label: 'Turquesa', primary: '#1A6E7A', onPrimary: '#FFFFFF', primaryContainer: '#B3EEFF', onPrimaryContainer: '#001F26', primaryDark: '#8AD4DF', onPrimaryDark: '#003640', primaryContainerDark: '#00525C', onPrimaryContainerDark: '#B3EEFF' },
  { label: 'Areia', primary: '#7A5C3A', onPrimary: '#FFFFFF', primaryContainer: '#FFDDBB', onPrimaryContainer: '#2C1600', primaryDark: '#E0C8A8', onPrimaryDark: '#442B10', primaryContainerDark: '#5E4124', onPrimaryContainerDark: '#FFDDBB' },
];

export const paletteEntry = (index: number): BabyPaletteEntry => BABY_PALETTE[index] ?? BABY_PALETTE[0];

/** Cor de cada categoria de registro (marcadores, ícones, chips). Sempre acompanha ícone e texto. */
export const CATEGORY_COLORS: Record<RecordType, string> = {
  BABY: '#9333EA',
  MOMENT: '#C2185B',
  VACCINE: '#2E7D32',
  APPOINTMENT: '#1565C0',
  HEALTH_EVENT: '#B94600',
  GROWTH: '#9333EA',
  FEEDING: '#00796B',
  MEDICATION: '#3949AB',
};

export type EffectiveTheme = 'light' | 'dark';

const media = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

export function effectiveTheme(mode: ThemeMode): EffectiveTheme {
  if (mode === 'LIGHT') return 'light';
  if (mode === 'DARK') return 'dark';
  return media?.matches ? 'dark' : 'light';
}

/** Aplica o tema e a cor do bebê na raiz do documento. */
export function applyTheme(mode: ThemeMode, colorIndex: number): EffectiveTheme {
  const theme = effectiveTheme(mode);
  const p = paletteEntry(colorIndex);
  const root = document.documentElement;
  root.dataset.theme = theme;
  const dark = theme === 'dark';
  root.style.setProperty('--primary', dark ? p.primaryDark : p.primary);
  root.style.setProperty('--on-primary', dark ? p.onPrimaryDark : p.onPrimary);
  root.style.setProperty('--primary-container', dark ? p.primaryContainerDark : p.primaryContainer);
  root.style.setProperty('--on-primary-container', dark ? p.onPrimaryContainerDark : p.onPrimaryContainer);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#121413' : p.primary);
  return theme;
}

export function useThemeMode(): ThemeMode {
  return useSyncExternalStore(subscribePrefs, Prefs.getTheme, () => 'SYSTEM' as ThemeMode);
}

export function useActiveBabyId(): number | null {
  return useSyncExternalStore(subscribePrefs, Prefs.getActiveBabyId, () => null);
}

// Pré-visualização da cor enquanto o formulário do bebê está aberto (nada é gravado).
let colorPreview: number | null = null;
const previewListeners = new Set<() => void>();
const subscribePreview = (cb: () => void) => {
  previewListeners.add(cb);
  return () => previewListeners.delete(cb);
};

/** Mostra uma cor no app sem salvar; use null para voltar à cor do bebê ativo. */
export function setColorPreview(index: number | null): void {
  if (colorPreview === index) return;
  colorPreview = index;
  previewListeners.forEach((cb) => cb());
}

/** Reaplica o tema quando o modo, a cor do bebê, a pré-visualização ou a preferência do sistema mudam. */
export function useApplyTheme(babyColorIndex: number): EffectiveTheme {
  const mode = useThemeMode();
  const preview = useSyncExternalStore(subscribePreview, () => colorPreview, () => null);
  const colorIndex = preview ?? babyColorIndex;
  useEffect(() => {
    applyTheme(mode, colorIndex);
    if (!media) return;
    const listener = () => applyTheme(mode, colorIndex);
    media.addEventListener('change', listener);
    return () => media.removeEventListener('change', listener);
  }, [mode, colorIndex]);
  return effectiveTheme(mode);
}
