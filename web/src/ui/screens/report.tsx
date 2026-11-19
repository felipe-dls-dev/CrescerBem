import { useEffect, useRef, useState } from 'react';
import { downloadFile } from '../../backup/files';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { minusDays, now, today } from '../../domain/dates';
import { Formats } from '../../domain/formats';
import { recordTypePluralLabel } from '../../domain/labels';
import { REPORTABLE_TYPES, buildReport, countFor } from '../../domain/report';
import type { LocalDate, RecordType } from '../../domain/types';
import { pdfPhotoLoader } from '../../report/photoLoader';
import { reportFileName } from '../../report/reportName';
import { Card, Loading } from '../components';
import { useBabyContext } from '../context';
import { CheckField, ChoiceChips, DateField, SelectField } from '../fields';
import { Icon } from '../icons';
import { goBack } from '../router';
import { NoBaby } from './shared';

type Preset = 'week' | 'month' | 'birth' | 'custom';

interface Ready {
  blob: Blob;
  url: string;
  name: string;
  start: LocalDate;
  end: LocalDate;
}

export function ReportScreen() {
  const { baby, babies } = useBabyContext();
  const [babyId, setBabyId] = useState<number | null>(null);
  const selected = babies.find((b) => b.id === babyId) ?? baby;
  const todayDate = today();
  const [preset, setPreset] = useState<Preset>('month');
  const [customStart, setCustomStart] = useState(minusDays(todayDate, 30));
  const [customEnd, setCustomEnd] = useState(todayDate);
  const [types, setTypes] = useState<RecordType[]>([...REPORTABLE_TYPES]);
  const [photos, setPhotos] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState<Ready | null>(null);
  const urlRef = useRef<string | null>(null);

  const start = preset === 'week' ? minusDays(todayDate, 6) : preset === 'month' ? minusDays(todayDate, 29) : preset === 'birth' ? (selected?.birthDate ?? todayDate) : customStart;
  const end = preset === 'custom' ? customEnd : todayDate;
  const periodError = !start || !end ? 'Informe as duas datas.' : end < start ? 'O fim precisa ser depois do início.' : null;

  const counts = useLive(async () => (selected && !periodError ? repos.timeline.loadData(selected.id!, start, end) : null), [selected?.id, start, end, periodError]).value;

  // Libera a memória do PDF anterior ao trocar de relatório ou sair da tela.
  const dropReady = () => {
    if (urlRef.current) URL.revokeObjectURL(urlRef.current);
    urlRef.current = null;
    setReady(null);
  };
  useEffect(() => () => {
    if (urlRef.current) URL.revokeObjectURL(urlRef.current);
  }, []);

  if (!selected) return <NoBaby>Cadastre o bebê e faça alguns registros para gerar um relatório.</NoBaby>;

  const generate = async () => {
    setBusy(true);
    setError(null);
    dropReady();
    try {
      const data = await repos.timeline.loadReportData(selected.id!, start, end, photos);
      const document = buildReport({ baby: selected, start, end, types, includePhotos: photos }, data, now());
      // O gerador de PDF só é carregado quando o usuário pede um relatório.
      const { renderReportPdf } = await import('../../report/pdfReport');
      const { pdf } = await renderReportPdf(document, photos ? pdfPhotoLoader(repos) : null);
      const blob = pdf.output('blob');
      const url = URL.createObjectURL(blob);
      urlRef.current = url;
      setReady({ blob, url, name: reportFileName(selected.name, start, end), start, end });
    } catch {
      setError('Não foi possível gerar o PDF. Tente de novo.');
    } finally {
      setBusy(false);
    }
  };

  const toggle = (type: RecordType, on: boolean) => setTypes((list) => (on ? REPORTABLE_TYPES.filter((t) => t === type || list.includes(t)) : list.filter((t) => t !== type)));

  return (
    <>
      <header style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: -8 }}>
        <button type="button" className="icon-btn" onClick={goBack} aria-label="Voltar">
          <Icon name="back" />
        </button>
        <h1>Relatório em PDF</h1>
      </header>
      <p className="muted">Escolha o período e o que entra no relatório. O PDF é criado neste aparelho e não é enviado para lugar nenhum.</p>

      {babies.length > 1 && (
        <SelectField
          label="Bebê"
          value={String(selected.id)}
          onChange={(v) => {
            setBabyId(Number(v));
            dropReady();
          }}
          options={babies.map((b) => ({ value: String(b.id), label: b.name }))}
        />
      )}

      <ChoiceChips
        legend="Período"
        value={preset}
        onChange={(v) => {
          if (v) {
            setPreset(v);
            dropReady();
          }
        }}
        options={[
          { value: 'week', label: 'Últimos 7 dias' },
          { value: 'month', label: 'Últimos 30 dias' },
          { value: 'birth', label: 'Desde o nascimento' },
          { value: 'custom', label: 'Personalizado' },
        ]}
      />
      {preset === 'custom' && (
        <div className="field-row">
          <DateField label="De" value={customStart} onChange={(v) => { setCustomStart(v); dropReady(); }} max={todayDate} />
          <DateField label="Até" value={customEnd} onChange={(v) => { setCustomEnd(v); dropReady(); }} max={todayDate} error={periodError} />
        </div>
      )}
      {preset !== 'custom' && (
        <p className="small-text muted">
          {Formats.date(start)} a {Formats.date(end)}
        </p>
      )}

      <fieldset className="field">
        <legend>O que incluir</legend>
        {REPORTABLE_TYPES.map((type) => (
          <CheckField
            key={type}
            label={recordTypePluralLabel[type]}
            hint={counts ? `${countFor(counts, type)} registro(s)` : undefined}
            checked={types.includes(type)}
            onChange={(on) => {
              toggle(type, on);
              dropReady();
            }}
          />
        ))}
      </fieldset>

      <CheckField label="Incluir miniaturas das fotos" hint="Desligado por padrão: o arquivo fica menor e mais discreto." checked={photos} onChange={(v) => { setPhotos(v); dropReady(); }} />

      {error && (
        <div className="error-summary" role="alert">
          {error}
        </div>
      )}
      {!counts && !periodError && <Loading />}

      <div>
        <button type="button" className="btn" disabled={busy || types.length === 0 || !!periodError} onClick={() => void generate()}>
          <Icon name="report" size={20} />
          {busy ? 'Gerando…' : 'Gerar PDF'}
        </button>
        {types.length === 0 && <p className="small-text muted">Escolha pelo menos um tipo de registro.</p>}
      </div>

      {ready && (
        <Card tone="primary">
          <h2>Relatório pronto</h2>
          <p>
            {Formats.date(ready.start)} a {Formats.date(ready.end)}. O arquivo só sai deste aparelho se você enviar por conta própria.
          </p>
          <div className="btn-row">
            <button type="button" className="btn" onClick={() => downloadFile(ready.blob, ready.name, 'application/pdf')}>
              <Icon name="download" size={20} />
              Baixar
            </button>
            <button type="button" className="btn outline" onClick={() => window.open(ready.url, '_blank', 'noopener')}>
              <Icon name="open" size={20} />
              Abrir
            </button>
          </div>
        </Card>
      )}
    </>
  );
}
