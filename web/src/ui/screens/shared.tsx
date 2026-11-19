import type { ReactNode } from 'react';
import type { VaccineDisplayStatus } from '../../domain/vaccineRules';
import { Card } from '../components';
import { Icon } from '../icons';
import { Link } from '../link';

export function NoBaby({ children }: { children: ReactNode }) {
  return (
    <Card tone="primary">
      <h1>Boas-vindas ao Crescer Bem</h1>
      <p>{children}</p>
      <Link className="btn" to="form/BABY">
        Cadastrar bebê
      </Link>
    </Card>
  );
}

export const VACCINE_STATUS_COLOR: Record<VaccineDisplayStatus, string> = {
  APPLIED: 'var(--ok)',
  PLANNED: 'var(--neutral)',
  PENDING: 'var(--pending)',
  OVERDUE: 'var(--overdue)',
  NOT_APPLICABLE: 'var(--neutral)',
};

export const VACCINE_STATUS_ICON: Record<VaccineDisplayStatus, Parameters<typeof Icon>[0]['name']> = {
  APPLIED: 'checkCircle',
  PLANNED: 'clock',
  PENDING: 'warning',
  OVERDUE: 'warning',
  NOT_APPLICABLE: 'cancel',
};

export const PageTitle = ({ children }: { children: ReactNode }) => (
  <h1 className="sr-only">{children}</h1>
);
