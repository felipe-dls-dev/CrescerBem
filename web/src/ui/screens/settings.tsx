import { useEffect, useState } from 'react';
import { MIN_PASSWORD_LENGTH, InvalidBackupError, backupFileName, createBackup, isEncrypted, restoreBackup } from '../../backup/backup';
import { WrongPasswordError } from '../../backup/crypto';
import { downloadFile, readFileText } from '../../backup/files';
import { Prefs, themeModeLabel, type ThemeMode } from '../../data/prefs';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { Formats } from '../../domain/formats';
import { Texts } from '../../domain/texts';
import { today } from '../../domain/dates';
import type { Baby } from '../../domain/types';
import { Card, ConfirmDialog, Dialog, Row, Section, showToast } from '../components';
import { useBabyContext } from '../context';
import { SelectField, TextField } from '../fields';
import { Icon } from '../icons';
import { isInstalled, promptInstall, useCanInstall } from '../install';
import { Link } from '../link';
import { formPath, goBack, navigate } from '../router';
import { paletteEntry, useThemeMode } from '../theme';

const APP_VERSION = '0.1.0';

export function SettingsScreen() {
  const { babies } = useBabyContext();
  const mode = useThemeMode();
  const [deleting, setDeleting] = useState<{ baby: Baby; step: 1 | 2 } | null>(null);
  const [erasing, setErasing] = useState<1 | 2 | null>(null);
  const canInstall = useCanInstall();

  return (
    <>
      <header style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: -8 }}>
        <button type="button" className="icon-btn" onClick={goBack} aria-label="Voltar">
          <Icon name="back" />
        </button>
        <h1>Ajustes</h1>
      </header>

      <Section title="Bebês" subtitle={babies.length === 0 ? 'Nenhum bebê cadastrado.' : `${babies.length} cadastrado(s)`} icon={<Icon name="baby" />} defaultOpen>
        {babies.map((b) => (
          <div key={b.id} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <Row
                badge={<span className="badge" style={{ background: paletteEntry(b.colorIndex).primary }} aria-hidden="true" />}
                headline={b.name}
                support={`Nascimento: ${Formats.date(b.birthDate)}`}
              />
            </div>
            <Link className="icon-btn" to={formPath({ type: 'BABY', id: b.id })} ariaLabel={`Editar dados de ${b.name}`}>
              <Icon name="edit" />
            </Link>
            <button type="button" className="icon-btn" aria-label={`Excluir ${b.name}`} onClick={() => setDeleting({ baby: b, step: 1 })}>
              <Icon name="delete" />
            </button>
          </div>
        ))}
        <div style={{ padding: '4px 8px' }}>
          <Link className="btn tonal small" to={formPath({ type: 'BABY' })}>
            <Icon name="add" size={18} />
            Adicionar bebê
          </Link>
        </div>
      </Section>

      <Card>
        <h2>Aparência</h2>
        <SelectField
          label="Tema"
          value={mode}
          onChange={(v: ThemeMode) => Prefs.setTheme(v)}
          options={(Object.keys(themeModeLabel) as ThemeMode[]).map((m) => ({ value: m, label: themeModeLabel[m] }))}
          hint="Automático segue o modo claro ou escuro do aparelho. O botão de sol e lua no topo também alterna."
        />
      </Card>

      <BackupSection />

      <Card>
        <h2>Dados e privacidade</h2>
        <p>{Texts.PRIVACY_SUMMARY}</p>
        <p className="muted">{Texts.DATA_LOSS_WARNING}</p>
        <StorageProtection />
        <div className="btn-row">
          <Link className="btn outline" to="sobre">
            Privacidade e ajuda
          </Link>
          <button type="button" className="btn danger-outline" onClick={() => setErasing(1)}>
            <Icon name="delete" size={20} />
            Apagar todos os dados
          </button>
        </div>
      </Card>

      {!isInstalled() && (
        <Card>
          <h2>Instalar no aparelho</h2>
          <p>Instale o Crescer Bem para abrir em tela cheia e usar sem internet.</p>
          {canInstall ? (
            <div>
              <button type="button" className="btn" onClick={() => void promptInstall()}>
                Instalar no aparelho
              </button>
            </div>
          ) : (
            <p className="small-text muted">Se o botão não aparecer, use o menu do navegador (“Instalar app” ou “Adicionar à Tela de Início”).</p>
          )}
        </Card>
      )}

      <Card>
        <h2>Sobre o Crescer Bem</h2>
        <p>Versão web {APP_VERSION}</p>
        <p className="muted">{Texts.DISCLAIMER}</p>
        <p className="muted">Em caso de emergência, ligue 192 (SAMU).</p>
      </Card>

      <ConfirmDialog
        open={deleting?.step === 1}
        title={`Excluir ${deleting?.baby.name ?? ''}?`}
        message={`Todos os registros e fotos de ${deleting?.baby.name ?? ''} serão apagados deste aparelho.`}
        confirmLabel="Continuar"
        onCancel={() => setDeleting(null)}
        onConfirm={() => setDeleting((d) => (d ? { ...d, step: 2 } : d))}
      />
      <ConfirmDialog
        open={deleting?.step === 2}
        title="Tem certeza?"
        message="Essa ação não pode ser desfeita. Se quiser guardar uma cópia, faça uma cópia de segurança ou gere um relatório em PDF antes."
        confirmLabel="Excluir definitivamente"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={async () => {
          const target = deleting!.baby;
          setDeleting(null);
          await repos.babies.delete(target);
          showToast(`${target.name} e todos os registros foram apagados.`);
        }}
      />
      <ConfirmDialog
        open={erasing === 1}
        title="Apagar todos os dados?"
        message="Todos os bebês, registros e fotos serão apagados deste aparelho."
        confirmLabel="Continuar"
        onCancel={() => setErasing(null)}
        onConfirm={() => setErasing(2)}
      />
      <ConfirmDialog
        open={erasing === 2}
        title="Tem certeza?"
        message="Essa ação não pode ser desfeita."
        confirmLabel="Apagar tudo"
        danger
        onCancel={() => setErasing(null)}
        onConfirm={async () => {
          setErasing(null);
          await repos.eraseEverything();
          showToast('Todos os dados foram apagados deste aparelho.');
          navigate('hoje');
        }}
      />
    </>
  );
}

// ---------------------------------------------------------------------------
// Proteção contra limpeza automática
// ---------------------------------------------------------------------------

function StorageProtection() {
  const [persisted, setPersisted] = useState<boolean | null>(null);
  useEffect(() => {
    void navigator.storage?.persisted?.().then(setPersisted, () => setPersisted(null));
  }, []);
  if (!navigator.storage?.persist) return null;
  return persisted ? (
    <p className="small-text" role="status">
      <Icon name="checkCircle" size={16} /> O navegador protege os dados deste app contra limpeza automática.
    </p>
  ) : (
    <div>
      <p className="small-text muted">O navegador pode apagar os dados do site se o aparelho ficar sem espaço.</p>
      <button
        type="button"
        className="btn tonal small"
        onClick={async () => {
          const granted = await navigator.storage.persist();
          setPersisted(granted);
          showToast(granted ? 'Proteção ativada.' : 'O navegador não permitiu. Instalar o app ajuda.');
        }}
      >
        Proteger dados contra limpeza automática
      </button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Cópia de segurança
// ---------------------------------------------------------------------------

function BackupSection() {
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [exporting, setExporting] = useState(false);
  const [pending, setPending] = useState<{ text: string; encrypted: boolean } | null>(null);
  const [importPassword, setImportPassword] = useState('');
  const [importError, setImportError] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);
  const hasData = (useLive(() => repos.db.babies.count(), []).value ?? 0) > 0;

  const passwordError =
    password === '' ? null : password.length < MIN_PASSWORD_LENGTH ? `Use pelo menos ${MIN_PASSWORD_LENGTH} caracteres.` : confirm !== '' && confirm !== password ? 'As senhas não são iguais.' : null;
  const canExport = hasData && password.length >= MIN_PASSWORD_LENGTH && confirm === password && !exporting;

  const doExport = async () => {
    setExporting(true);
    try {
      const file = await createBackup(repos.db, password);
      downloadFile(file, backupFileName(today()), 'application/json');
      showToast('Cópia de segurança salva. Guarde o arquivo e a senha em lugar seguro.');
      setPassword('');
      setConfirm('');
    } catch {
      showToast('Não foi possível gerar a cópia. Tente de novo.');
    } finally {
      setExporting(false);
    }
  };

  const chooseFile = async (file: File) => {
    try {
      const text = await readFileText(file);
      setImportPassword('');
      setImportError(null);
      setPending({ text, encrypted: isEncrypted(text) });
    } catch (e) {
      showToast(e instanceof InvalidBackupError ? e.message : 'Não foi possível ler o arquivo.');
    }
  };

  const doImport = async () => {
    if (!pending) return;
    setImporting(true);
    setImportError(null);
    try {
      const summary = await restoreBackup(repos.db, pending.text, pending.encrypted ? importPassword : null);
      setPending(null);
      showToast(`Cópia restaurada: ${summary.babies} bebê(s), ${summary.records} registro(s) e ${summary.photos} foto(s).`);
      navigate('hoje');
    } catch (e) {
      setImportError(e instanceof WrongPasswordError || e instanceof InvalidBackupError ? e.message : 'Não foi possível restaurar a cópia. Seus dados atuais não foram alterados.');
    } finally {
      setImporting(false);
    }
  };

  return (
    <Card>
      <h2>Cópia de segurança</h2>
      <p>
        Como os dados ficam só neste navegador, o arquivo de cópia é a única forma de guardá-los fora dele ou levá-los a outro aparelho. O arquivo é protegido por senha e é criado aqui mesmo, sem enviar nada pela internet.
      </p>
      <TextField label="Senha da cópia" value={password} onChange={setPassword} secret hint={`Pelo menos ${MIN_PASSWORD_LENGTH} caracteres. Se esquecer, não há como recuperar a cópia.`} error={passwordError} />
      <TextField label="Repita a senha" value={confirm} onChange={setConfirm} secret />
      <div>
        <button type="button" className="btn" disabled={!canExport} onClick={() => void doExport()}>
          <Icon name="download" size={20} />
          {exporting ? 'Gerando…' : 'Salvar cópia de segurança'}
        </button>
      </div>
      {!hasData && <p className="small-text muted">Cadastre um bebê para poder fazer uma cópia.</p>}
      <hr className="divider" />
      <p className="label-text">Restaurar uma cópia</p>
      <p className="small-text muted">Os dados que estão agora neste navegador serão trocados pelos da cópia.</p>
      <div>
        <label className="btn tonal" style={{ cursor: 'pointer' }}>
          <Icon name="upload" size={20} />
          Restaurar de um arquivo
          <input
            type="file"
            accept="application/json,.json"
            className="sr-only"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void chooseFile(file);
              e.target.value = '';
            }}
          />
        </label>
      </div>
      <Dialog
        open={pending !== null}
        onClose={() => !importing && setPending(null)}
        title="Restaurar cópia de segurança?"
        actions={
          <>
            <button type="button" className="btn text" disabled={importing} onClick={() => setPending(null)}>
              Cancelar
            </button>
            <button type="button" className="btn danger" disabled={importing || (pending?.encrypted && importPassword === '')} onClick={() => void doImport()}>
              {importing ? 'Restaurando…' : 'Substituir meus dados'}
            </button>
          </>
        }
      >
        <p>Todos os dados deste navegador serão trocados pelos da cópia. Isso não pode ser desfeito.</p>
        {pending?.encrypted && <TextField label="Senha da cópia" value={importPassword} onChange={setImportPassword} secret error={importError} autoFocus />}
        {!pending?.encrypted && importError && (
          <p className="error-text" role="alert">
            {importError}
          </p>
        )}
      </Dialog>
    </Card>
  );
}
