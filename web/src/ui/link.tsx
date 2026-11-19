import type { ReactNode } from 'react';

/** Link interno (rota por hash). Fica como <a> de verdade: abre com teclado, leitor de tela e "abrir em nova aba". */
export function Link({ to, className, children, ariaLabel }: { to: string; className?: string; children: ReactNode; ariaLabel?: string }) {
  return (
    <a className={className} href={`#/${to}`} aria-label={ariaLabel}>
      {children}
    </a>
  );
}
