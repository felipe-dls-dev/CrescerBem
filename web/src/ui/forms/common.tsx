import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Card, ConfirmDialog, Loading, showToast } from '../components';
import { Icon } from '../icons';
import { goBack } from '../router';
import { useBabyContext } from '../context';
import { Link } from '../link';

/** Troca texto vazio por null (os campos opcionais ficam sem valor, não com texto em branco). */
export const nz = (text: string): string | null => (text.trim() === '' ? null : text.trim());

/** Carrega um registro para edição. Sem [id] (registro novo) não busca nada. */
export function useLoaded<T>(id: number | undefined, load: (id: number) => Promise<T | undefined>) {
  const [state, setState] = useState<{ loading: boolean; value: T | undefined }>({ loading: id !== undefined, value: undefined });
  useEffect(() => {
    if (id === undefined) return;
    let cancelled = false;
    void load(id).then((value) => {
      if (!cancelled) setState({ loading: false, value });
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);
  return state;
}

export interface FormPageProps {
  title: string;
  children: ReactNode;
  onSave: () => Promise<void> | void;
  errors?: string[];
  saveLabel?: string;
  /** Presente só na edição: texto e ação de excluir. */
  onDelete?: () => Promise<void>;
  deleteTitle?: string;
  deleteMessage?: string;
  extraActions?: ReactNode;
}

/** Moldura de formulário: título, voltar, erros no topo, salvar e (na edição) excluir com confirmação. */
export function FormPage({
  title,
  children,
  onSave,
  errors = [],
  saveLabel = 'Salvar',
  onDelete,
  deleteTitle = 'Excluir este registro?',
  deleteMessage = 'As fotos dele também serão apagadas deste aparelho. Essa ação não pode ser desfeita.',
  extraActions,
}: FormPageProps) {
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [showErrors, setShowErrors] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setShowErrors(true);
    if (saving || errors.length > 0) return;
    setSaving(true);
    try {
      await onSave();
    } catch {
      showToast('Não foi possível salvar. Tente de novo.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <header style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: -8 }}>
        <button type="button" className="icon-btn" onClick={goBack} aria-label="Voltar">
          <Icon name="back" />
        </button>
        <h1>{title}</h1>
      </header>
      <form className="form" onSubmit={submit} noValidate>
        {showErrors && errors.length > 0 && (
          <div className="error-summary" role="alert">
            <p>Confira o que falta:</p>
            <ul style={{ margin: '4px 0 0', paddingLeft: 20 }}>
              {errors.map((er) => (
                <li key={er}>{er}</li>
              ))}
            </ul>
          </div>
        )}
        {children}
        <div className="btn-row">
          <button type="submit" className="btn" disabled={saving}>
            {saving ? 'Salvando…' : saveLabel}
          </button>
          <button type="button" className="btn text" onClick={goBack}>
            Cancelar
          </button>
          {extraActions}
          {onDelete && (
            <button type="button" className="btn danger-outline" onClick={() => setConfirmDelete(true)}>
              <Icon name="delete" size={20} />
              Excluir
            </button>
          )}
        </div>
      </form>
      {onDelete && (
        <ConfirmDialog
          open={confirmDelete}
          title={deleteTitle}
          message={deleteMessage}
          confirmLabel="Excluir"
          danger
          onCancel={() => setConfirmDelete(false)}
          onConfirm={async () => {
            setConfirmDelete(false);
            await onDelete();
          }}
        />
      )}
    </>
  );
}

/** Aviso quando ainda não há bebê cadastrado (formulários de registro precisam de um). */
export function NeedsBaby({ children }: { children: (babyId: number) => ReactNode }) {
  const { baby } = useBabyContext();
  if (!baby) {
    return (
      <Card>
        <p>Cadastre o bebê primeiro para fazer registros.</p>
        <Link className="btn" to="form/BABY">
          Cadastrar bebê
        </Link>
      </Card>
    );
  }
  return <>{children(baby.id!)}</>;
}

export const FormLoading = Loading;
