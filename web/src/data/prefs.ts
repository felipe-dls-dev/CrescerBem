// Preferências do aparelho (tema e bebê ativo) em localStorage.
// Guardam só o id do bebê e o modo do tema: nenhum dado de saúde.
// Todo acesso é protegido: em janela privada ou com dados de site bloqueados o app continua funcionando.

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

export const themeModeLabel: Record<ThemeMode, string> = {
  SYSTEM: 'Automático',
  LIGHT: 'Claro',
  DARK: 'Escuro',
};

export const PREF_KEYS = {
  activeBaby: 'crescerbem.bebeAtivo',
  theme: 'crescerbem.tema',
  welcomeSeen: 'crescerbem.boasVindasVistas',
} as const;

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    // Sem armazenamento disponível: segue sem lembrar a preferência.
  }
}

type Listener = () => void;
const listeners = new Set<Listener>();
const notify = () => listeners.forEach((l) => l());

/** Para uso com useSyncExternalStore. */
export function subscribePrefs(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export const Prefs = {
  getActiveBabyId(): number | null {
    const raw = read(PREF_KEYS.activeBaby);
    const id = raw === null ? NaN : Number(raw);
    return Number.isInteger(id) && id > 0 ? id : null;
  },

  setActiveBabyId(id: number | null): void {
    write(PREF_KEYS.activeBaby, id === null ? null : String(id));
    notify();
  },

  getTheme(): ThemeMode {
    const raw = read(PREF_KEYS.theme);
    return raw === 'LIGHT' || raw === 'DARK' || raw === 'SYSTEM' ? raw : 'SYSTEM';
  },

  setTheme(mode: ThemeMode): void {
    write(PREF_KEYS.theme, mode);
    notify();
  },

  /** Remove todas as preferências (parte do "apagar tudo"). */
  clearAll(): void {
    Object.values(PREF_KEYS).forEach((key) => write(key, null));
    notify();
  },
};
