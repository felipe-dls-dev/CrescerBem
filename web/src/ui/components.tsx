import { useEffect, useRef, useState, type ReactNode } from 'react';
import { recordTypeLabel } from '../domain/labels';
import { Texts } from '../domain/texts';
import { Formats } from '../domain/formats';
import type { TimelineItem } from '../domain/timeline';
import type { MeasurementType, RecordType } from '../domain/types';
import { Icon, RECORD_ICON } from './icons';
import { formPath } from './router';
import { CATEGORY_COLORS } from './theme';

// ---------------------------------------------------------------------------
// Marca
// ---------------------------------------------------------------------------

export function Logo({ size = 28 }: { size?: number }) {
  return (
    <svg className="logo-mark" width={size} height={(size * 42) / 44} viewBox="0 0 44 42" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M22,42 C22,42 0,28 0,13 C0,5.5 5.5,0 12.5,0 C16.6,0 20.1,2 22,5.2 C23.9,2 27.4,0 31.5,0 C38.5,0 44,5.5 44,13 C44,28 22,42 22,42 Z"
      />
      <path
        fill="var(--bg)"
        d="M21,30 L21,16 C21,11 17,8 13,8 C13,13 16.5,16.5 21,16.8 Z M23,22 L23,14 C23,10 26.5,7 31,7 C31,11.5 27.5,14.8 23,15 Z M21,16 L23,16 L23,30 L21,30 Z"
      />
    </svg>
  );
}

// ---------------------------------------------------------------------------
// Blocos de conteúdo
// ---------------------------------------------------------------------------

export function Card({ children, tone, className = '' }: { children: ReactNode; tone?: 'primary' | 'warn' | 'error'; className?: string }) {
  return <section className={`card ${tone ?? ''} ${className}`}>{children}</section>;
}

export function DisclaimerCard() {
  return (
    <Card>
      <p className="small-text">{Texts.DISCLAIMER}</p>
      <p className="small-text muted">Em caso de emergência, ligue 192 (SAMU).</p>
    </Card>
  );
}

export function EmptyHint({ children }: { children: ReactNode }) {
  return <p className="empty">{children}</p>;
}

export function Loading() {
  return (
    <p className="empty" role="status">
      Carregando…
    </p>
  );
}

/** Seção que abre e fecha (usa <details>, que já funciona com teclado e leitor de tela). */
export function Section({
  title,
  subtitle,
  icon,
  defaultOpen = false,
  children,
}: {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  defaultOpen?: boolean;
  children: ReactNode;
}) {
  return (
    <details className="section" open={defaultOpen}>
      <summary>
        {icon}
        <span className="titles">
          <span className="title">{title}</span>
          {subtitle && <span className="small-text muted" style={{ display: 'block' }}>{subtitle}</span>}
        </span>
        <span className="chev">
          <Icon name="expand" />
        </span>
      </summary>
      <div className="body">{children}</div>
    </details>
  );
}

export function TypeBadge({ type }: { type: RecordType }) {
  return (
    <span className="badge" style={{ background: CATEGORY_COLORS[type] }}>
      <Icon name={RECORD_ICON[type]} size={22} />
    </span>
  );
}

export function StatusPill({ label, color, icon }: { label: string; color: string; icon?: Parameters<typeof Icon>[0]['name'] }) {
  return (
    <span className="pill" style={{ color }}>
      {icon && <Icon name={icon} size={14} />}
      {label}
    </span>
  );
}

export function StatTile({ value, label, onClick, onClickLabel }: { value: string; label: string; onClick?: () => void; onClickLabel?: string }) {
  const content = (
    <>
      <span className="value">{value}</span>
      <span className="caption">{label}</span>
    </>
  );
  if (!onClick) return <div className="stat">{content}</div>;
  return (
    <button type="button" className="stat" onClick={onClick} aria-label={`${label}: ${value}. ${onClickLabel ?? ''}`.trim()}>
      {content}
    </button>
  );
}

/** Linha de lista: ícone, textos e ação à direita. Vira link quando tem [href]. */
export function Row({
  badge,
  overline,
  headline,
  support,
  trail,
  href,
  onClick,
  actionLabel,
}: {
  badge?: ReactNode;
  overline?: string;
  headline: ReactNode;
  support?: ReactNode;
  trail?: ReactNode;
  href?: string;
  onClick?: () => void;
  /** Texto de apoio para leitores de tela, ex.: "Abrir consulta". */
  actionLabel?: string;
}) {
  const inner = (
    <>
      {badge}
      <span className="main">
        {overline && <span className="overline" style={{ display: 'block' }}>{overline}</span>}
        <span className="headline" style={{ display: 'block' }}>{headline}</span>
        {support && <span className="support" style={{ display: 'block' }}>{support}</span>}
      </span>
      {trail && <span className="trail">{trail}</span>}
      {actionLabel && <span className="sr-only">{actionLabel}</span>}
    </>
  );
  if (href) {
    return (
      <a className="row" href={href}>
        {inner}
      </a>
    );
  }
  if (onClick) {
    return (
      <button type="button" className="row" onClick={onClick}>
        {inner}
      </button>
    );
  }
  return <div className="row static">{inner}</div>;
}

export function TimelineRow({ item }: { item: TimelineItem }) {
  return (
    <Row
      href={`#/${formPath({ type: item.type, id: item.id })}`}
      badge={<TypeBadge type={item.type} />}
      overline={`${recordTypeLabel[item.type]}${item.time ? ` · ${Formats.time(item.time)}` : ''}`}
      headline={item.title}
      support={item.detail}
      actionLabel="Abrir registro"
    />
  );
}

// ---------------------------------------------------------------------------
// Abas
// ---------------------------------------------------------------------------

export function Tabs<T extends string>({
  tabs,
  value,
  onChange,
  label,
}: {
  tabs: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  label: string;
}) {
  return (
    <div className="tabs" role="tablist" aria-label={label}>
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          role="tab"
          className="tab"
          id={`tab-${tab.value}`}
          aria-selected={tab.value === value}
          aria-controls={`panel-${tab.value}`}
          onClick={() => onChange(tab.value)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

export function TabPanel({ id, children }: { id: string; children: ReactNode }) {
  return (
    <div role="tabpanel" id={`panel-${id}`} aria-labelledby={`tab-${id}`} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {children}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Diálogos (elemento <dialog> nativo: prende o foco e fecha com Esc)
// ---------------------------------------------------------------------------

export function Dialog({
  open,
  onClose,
  title,
  children,
  actions,
  sheet = false,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  actions?: ReactNode;
  sheet?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      className={sheet ? 'sheet' : ''}
      aria-labelledby="dialog-title"
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
    >
      {open && (
        <div className="dialog-body">
          <h2 id="dialog-title">{title}</h2>
          {children}
          {actions && <div className="dialog-actions">{actions}</div>}
        </div>
      )}
    </dialog>
  );
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel = 'Cancelar',
  danger = false,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  message: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Dialog
      open={open}
      onClose={onCancel}
      title={title}
      actions={
        <>
          <button type="button" className="btn text" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button type="button" className={`btn ${danger ? 'danger' : ''}`} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </>
      }
    >
      <div>{message}</div>
    </Dialog>
  );
}

// ---------------------------------------------------------------------------
// Aviso rápido (toast)
// ---------------------------------------------------------------------------

type ToastListener = (message: string) => void;
const toastListeners = new Set<ToastListener>();
export const showToast = (message: string): void => toastListeners.forEach((l) => l(message));

export function ToastHost() {
  const [message, setMessage] = useState<string | null>(null);
  useEffect(() => {
    let timer: number | undefined;
    const listener: ToastListener = (text) => {
      setMessage(text);
      window.clearTimeout(timer);
      timer = window.setTimeout(() => setMessage(null), 5000);
    };
    toastListeners.add(listener);
    return () => {
      toastListeners.delete(listener);
      window.clearTimeout(timer);
    };
  }, []);
  return (
    <div role="status" aria-live="polite">
      {message && <div className="toast">{message}</div>}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Gráfico de linha (SVG, sem biblioteca)
// ---------------------------------------------------------------------------

export function LineChart({ points, type, description }: { points: { date: string; value: number }[]; type: MeasurementType; description: string }) {
  const W = 320;
  const H = 150;
  const pad = { l: 44, r: 12, t: 12, b: 26 };
  const values = points.map((p) => p.value);
  let min = Math.min(...values);
  let max = Math.max(...values);
  if (min === max) {
    min -= 1;
    max += 1;
  }
  const span = max - min;
  const x = (i: number) => pad.l + (points.length === 1 ? (W - pad.l - pad.r) / 2 : (i * (W - pad.l - pad.r)) / (points.length - 1));
  const y = (v: number) => pad.t + (1 - (v - min) / span) * (H - pad.t - pad.b);
  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.value).toFixed(1)}`).join(' ');
  return (
    <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={description}>
      {[min, (min + max) / 2, max].map((v) => (
        <g key={v}>
          <line className="grid" x1={pad.l} x2={W - pad.r} y1={y(v)} y2={y(v)} />
          <text x={pad.l - 6} y={y(v) + 4} textAnchor="end">
            {Formats.decimal(v, type === 'WEIGHT' ? 2 : 0)}
          </text>
        </g>
      ))}
      <path className="line" d={path} />
      {points.map((p, i) => (
        <circle key={`${p.date}-${i}`} className="pt" cx={x(i)} cy={y(p.value)} r="3.5" />
      ))}
      <text x={pad.l} y={H - 6}>{Formats.shortDate(points[0].date)}</text>
      <text x={W - pad.r} y={H - 6} textAnchor="end">{Formats.shortDate(points[points.length - 1].date)}</text>
    </svg>
  );
}
