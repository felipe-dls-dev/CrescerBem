// Instalação do app (PWA): guarda o convite do navegador para oferecer o botão "Instalar no aparelho".

import { useSyncExternalStore } from 'react';

interface InstallEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

let deferred: InstallEvent | null = null;
const listeners = new Set<() => void>();
const notify = () => listeners.forEach((l) => l());

if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferred = event as InstallEvent;
    notify();
  });
  window.addEventListener('appinstalled', () => {
    deferred = null;
    notify();
  });
}

const subscribe = (l: () => void) => {
  listeners.add(l);
  return () => listeners.delete(l);
};

export const useCanInstall = (): boolean => useSyncExternalStore(subscribe, () => deferred !== null, () => false);

export async function promptInstall(): Promise<boolean> {
  if (!deferred) return false;
  await deferred.prompt();
  const { outcome } = await deferred.userChoice;
  deferred = null;
  notify();
  return outcome === 'accepted';
}

/** O app já está aberto como aplicativo instalado? */
export const isInstalled = (): boolean =>
  typeof window !== 'undefined' && (window.matchMedia?.('(display-mode: standalone)').matches ?? false);
