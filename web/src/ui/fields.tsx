// Campos de formulário acessíveis: rótulo sempre visível, dica e erro ligados ao campo.

import { useId, type ReactNode } from 'react';

interface FieldFrameProps {
  label: string;
  hint?: string;
  error?: string | null;
  children: (ids: { id: string; describedBy: string | undefined; invalid: boolean }) => ReactNode;
  required?: boolean;
}

function FieldFrame({ label, hint, error, children, required }: FieldFrameProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {required ? <span aria-hidden="true"> *</span> : null}
      </label>
      {children({ id, describedBy, invalid: !!error })}
      {hint && (
        <span className="hint" id={hintId}>
          {hint}
        </span>
      )}
      {error && (
        <span className="error-text" id={errorId} role="alert">
          {error}
        </span>
      )}
    </div>
  );
}

export function TextField({
  label,
  value,
  onChange,
  hint,
  error,
  placeholder,
  inputMode,
  required,
  multiline,
  autoFocus,
  secret,
  maxLength = 500,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  hint?: string;
  error?: string | null;
  placeholder?: string;
  inputMode?: 'text' | 'decimal' | 'numeric';
  required?: boolean;
  multiline?: boolean;
  autoFocus?: boolean;
  /** Campo de senha: esconde o que é digitado. */
  secret?: boolean;
  maxLength?: number;
}) {
  return (
    <FieldFrame label={label} hint={hint} error={error} required={required}>
      {({ id, describedBy, invalid }) =>
        multiline ? (
          <textarea
            id={id}
            className="textarea"
            value={value}
            maxLength={maxLength * 4}
            onChange={(e) => onChange(e.target.value)}
            aria-describedby={describedBy}
            aria-invalid={invalid}
            placeholder={placeholder}
          />
        ) : (
          <input
            id={id}
            className="input"
            type={secret ? 'password' : 'text'}
            value={value}
            maxLength={maxLength}
            inputMode={inputMode}
            onChange={(e) => onChange(e.target.value)}
            aria-describedby={describedBy}
            aria-invalid={invalid}
            aria-required={required}
            placeholder={placeholder}
            autoFocus={autoFocus}
            autoComplete={secret ? 'new-password' : 'off'}
          />
        )
      }
    </FieldFrame>
  );
}

export function DateField({
  label,
  value,
  onChange,
  error,
  hint,
  required,
  max,
  min,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string | null;
  hint?: string;
  required?: boolean;
  max?: string;
  min?: string;
}) {
  return (
    <FieldFrame label={label} error={error} hint={hint} required={required}>
      {({ id, describedBy, invalid }) => (
        <input
          id={id}
          className="input"
          type="date"
          value={value}
          max={max}
          min={min}
          onChange={(e) => onChange(e.target.value)}
          aria-describedby={describedBy}
          aria-invalid={invalid}
          aria-required={required}
        />
      )}
    </FieldFrame>
  );
}

export function TimeField({
  label,
  value,
  onChange,
  error,
  required,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string | null;
  required?: boolean;
}) {
  return (
    <FieldFrame label={label} error={error} required={required}>
      {({ id, describedBy, invalid }) => (
        <input
          id={id}
          className="input"
          type="time"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          aria-describedby={describedBy}
          aria-invalid={invalid}
          aria-required={required}
        />
      )}
    </FieldFrame>
  );
}

export function SelectField<T extends string>({
  label,
  value,
  onChange,
  options,
  hint,
}: {
  label: string;
  value: T;
  onChange: (value: T) => void;
  options: { value: T; label: string }[];
  hint?: string;
}) {
  return (
    <FieldFrame label={label} hint={hint}>
      {({ id, describedBy }) => (
        <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value as T)} aria-describedby={describedBy}>
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </FieldFrame>
  );
}

/** Escolha de uma opção entre poucas (botões em grupo). */
export function ChoiceChips<T extends string>({
  legend,
  value,
  onChange,
  options,
  hint,
  allowNone,
}: {
  legend: string;
  value: T | null;
  onChange: (value: T | null) => void;
  options: { value: T; label: string }[];
  hint?: string;
  /** Permite desmarcar a opção escolhida. */
  allowNone?: boolean;
}) {
  return (
    <fieldset className="field">
      <legend>{legend}</legend>
      <div className="chips">
        {options.map((o) => (
          <button
            key={o.value}
            type="button"
            className="chip"
            aria-pressed={value === o.value}
            onClick={() => onChange(allowNone && value === o.value ? null : o.value)}
          >
            {o.label}
          </button>
        ))}
      </div>
      {hint && <span className="hint">{hint}</span>}
    </fieldset>
  );
}

/** Escolha de várias opções. */
export function MultiChips<T extends string>({
  legend,
  values,
  onChange,
  options,
  error,
}: {
  legend: string;
  values: T[];
  onChange: (values: T[]) => void;
  options: { value: T; label: string }[];
  error?: string | null;
}) {
  const toggle = (v: T) => onChange(values.includes(v) ? values.filter((x) => x !== v) : [...values, v]);
  return (
    <fieldset className="field">
      <legend>{legend}</legend>
      <div className="chips">
        {options.map((o) => (
          <button key={o.value} type="button" className="chip" aria-pressed={values.includes(o.value)} onClick={() => toggle(o.value)}>
            {o.label}
          </button>
        ))}
      </div>
      {error && (
        <span className="error-text" role="alert">
          {error}
        </span>
      )}
    </fieldset>
  );
}

export function CheckField({
  label,
  hint,
  checked,
  onChange,
}: {
  label: string;
  hint?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  const id = useId();
  return (
    <div className="check">
      <input id={id} type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} aria-describedby={hint ? `${id}-hint` : undefined} />
      <label htmlFor={id} className="text">
        <span style={{ fontWeight: 600 }}>{label}</span>
        {hint && (
          <span className="hint muted small-text" id={`${id}-hint`}>
            {hint}
          </span>
        )}
      </label>
    </div>
  );
}
