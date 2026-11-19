import { useState } from 'react';
import { repos } from '../../data/repo';
import { atTime, dateOf, now, timeOf, today } from '../../domain/dates';
import { Formats } from '../../domain/formats';
import { appointmentStatusLabel, measurementLabel, measurementUnit } from '../../domain/labels';
import { Plausibility } from '../../domain/plausibility';
import { Texts } from '../../domain/texts';
import {
  APPOINTMENT_STATUSES,
  MEASUREMENT_TYPES,
  type Appointment,
  type AppointmentStatus,
  type GrowthMeasurement,
  type MeasurementType,
  type Moment,
  type Vaccine,
  type VaccineStatus,
} from '../../domain/types';
import { Card, ConfirmDialog, Loading, showToast } from '../components';
import { ChoiceChips, DateField, TextField, TimeField } from '../fields';
import { PhotoAttachments, usePhotoDraft } from '../photos';
import { goBack, type FormTarget } from '../router';
import { FormPage, nz, NeedsBaby, useLoaded } from './common';

// ---------------------------------------------------------------------------
// Momento
// ---------------------------------------------------------------------------

export function MomentForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getMoment);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <MomentBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function MomentBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: Moment }) {
  const [title, setTitle] = useState(initial?.title ?? '');
  const [date, setDate] = useState(initial ? dateOf(initial.dateTime) : (target.date ?? today()));
  const [time, setTime] = useState(initial ? timeOf(initial.dateTime) : timeOf(now()));
  const [description, setDescription] = useState(initial?.description ?? '');
  const photos = usePhotoDraft('MOMENT', initial?.id);

  const errors = [!title.trim() && 'Dê um título ao momento.', !date && 'Informe a data.'].filter(Boolean) as string[];

  return (
    <FormPage
      title={initial ? 'Editar momento' : 'Novo momento'}
      errors={errors}
      onSave={async () => {
        const id = await repos.records.saveMoment({
          ...(initial ?? {}),
          babyId,
          title: title.trim(),
          dateTime: atTime(date, time || '00:00'),
          description: nz(description),
        });
        await photos.commit(babyId, id);
        showToast('Momento salvo.');
        goBack();
      }}
      onDelete={
        initial
          ? async () => {
              await repos.records.delete('MOMENT', initial.id!);
              showToast('Momento excluído.');
              goBack();
            }
          : undefined
      }
    >
      <TextField label="Título" value={title} onChange={setTitle} required placeholder="Ex.: Primeiro sorriso" autoFocus={!initial} />
      <div className="field-row">
        <DateField label="Data" value={date} onChange={setDate} required />
        <TimeField label="Hora" value={time} onChange={setTime} />
      </div>
      <TextField label="Descrição (opcional)" value={description} onChange={setDescription} multiline />
      <PhotoAttachments draft={photos} />
    </FormPage>
  );
}

// ---------------------------------------------------------------------------
// Medida (peso, altura, perímetro cefálico)
// ---------------------------------------------------------------------------

const PLACES = ['Posto de saúde', 'Consultório', 'Farmácia'];
const MEASUREMENT_OPTIONS = MEASUREMENT_TYPES.map((t) => ({ value: t, label: measurementLabel[t] }));
const MEASUREMENT_HINT: Record<MeasurementType, string> = {
  WEIGHT: 'Em quilos, ex.: 4,250',
  HEIGHT: 'Em centímetros, ex.: 54,5',
  HEAD_CIRCUMFERENCE: 'Em centímetros, ex.: 36,0',
};

export function GrowthForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getGrowth);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <GrowthBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function GrowthBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: GrowthMeasurement }) {
  const preset = MEASUREMENT_TYPES.find((t) => t === target.preset);
  const [type, setType] = useState<MeasurementType>(initial?.type ?? preset ?? 'WEIGHT');
  const [valueText, setValueText] = useState(initial ? Formats.decimal(initial.value, type === 'WEIGHT' ? 3 : 1) : '');
  const [date, setDate] = useState(initial?.date ?? target.date ?? today());
  const [place, setPlace] = useState(initial?.place ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [warning, setWarning] = useState<string | null>(null);
  const photos = usePhotoDraft('GROWTH', initial?.id);

  const parsed = Formats.parseDecimal(valueText);
  const errors = [
    parsed === null && `Informe o valor em ${measurementUnit[type]}.`,
    parsed !== null && parsed <= 0 && 'O valor precisa ser maior que zero.',
    !date && 'Informe a data da medida.',
    date > today() && 'A data da medida não pode estar no futuro.',
  ].filter(Boolean) as string[];

  const persist = async () => {
    const id = await repos.records.saveGrowth({
      ...(initial ?? {}),
      babyId,
      type,
      value: parsed!,
      date,
      place: nz(place),
      notes: nz(notes),
    });
    await photos.commit(babyId, id);
    showToast('Medida salva.');
    goBack();
  };

  return (
    <>
      <FormPage
        title={initial ? 'Editar medida' : 'Nova medida'}
        errors={errors}
        onSave={() => {
          // Valor improvável (ex.: peso em gramas): pede confirmação, mas respeita a decisão.
          const w = parsed !== null ? Plausibility.measurementWarning(type, parsed) : null;
          if (w) {
            setWarning(w);
            return;
          }
          return persist();
        }}
        onDelete={
          initial
            ? async () => {
                await repos.records.delete('GROWTH', initial.id!);
                showToast('Medida excluída.');
                goBack();
              }
            : undefined
        }
      >
        <ChoiceChips legend="O que você mediu?" value={type} onChange={(v) => v && setType(v)} options={MEASUREMENT_OPTIONS} />
        <TextField label={`Valor (${measurementUnit[type]})`} value={valueText} onChange={setValueText} inputMode="decimal" hint={MEASUREMENT_HINT[type]} required />
        <DateField label="Data" value={date} onChange={setDate} max={today()} required />
        <ChoiceChips
          legend="Onde foi medido (opcional)"
          value={PLACES.includes(place) ? place : null}
          onChange={(v) => setPlace(v ?? '')}
          options={PLACES.map((p) => ({ value: p, label: p }))}
          allowNone
        />
        <TextField label="Outro local (opcional)" value={PLACES.includes(place) ? '' : place} onChange={setPlace} />
        <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
        <PhotoAttachments draft={photos} hint="Ex.: foto da balança ou da caderneta. Fica só neste aparelho." />
      </FormPage>
      <ConfirmDialog
        open={warning !== null}
        title="Valor fora do comum"
        message={warning}
        confirmLabel="Salvar mesmo assim"
        cancelLabel="Corrigir"
        onCancel={() => setWarning(null)}
        onConfirm={() => {
          setWarning(null);
          void persist();
        }}
      />
    </>
  );
}

// ---------------------------------------------------------------------------
// Vacina
// ---------------------------------------------------------------------------

const VACCINE_STATUS_OPTIONS: { value: VaccineStatus; label: string }[] = [
  { value: 'SCHEDULED', label: 'Prevista' },
  { value: 'APPLIED', label: 'Aplicada' },
  { value: 'NOT_APPLICABLE', label: 'Não se aplica' },
];
const EXTRA_VACCINES = ['Influenza (gripe)', 'COVID-19'];

export function VaccineForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getVaccine);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <VaccineBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function VaccineBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: Vaccine }) {
  const [name, setName] = useState(initial?.name ?? '');
  const [dose, setDose] = useState(initial?.dose ?? '');
  const [status, setStatus] = useState<VaccineStatus>(initial?.status ?? 'SCHEDULED');
  const [appliedDate, setAppliedDate] = useState(initial?.appliedDate ?? today());
  const [scheduledDate, setScheduledDate] = useState(initial?.scheduledDate ?? target.date ?? '');
  const [lot, setLot] = useState(initial?.lot ?? '');
  const [place, setPlace] = useState(initial?.place ?? '');
  const [reaction, setReaction] = useState(initial?.reaction ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const photos = usePhotoDraft('VACCINE', initial?.id);

  const errors = [
    !name.trim() && 'Informe o nome da vacina.',
    status === 'APPLIED' && !appliedDate && 'Informe a data da aplicação.',
    status === 'SCHEDULED' && !scheduledDate && 'Informe a data prevista.',
  ].filter(Boolean) as string[];

  return (
    <FormPage
      title={initial ? 'Editar vacina' : 'Nova vacina'}
      errors={errors}
      onSave={async () => {
        const id = await repos.records.saveVaccine({
          ...(initial ?? {}),
          babyId,
          name: name.trim(),
          dose: nz(dose),
          status,
          scheduledDate: status === 'SCHEDULED' ? scheduledDate : (initial?.scheduledDate ?? (scheduledDate || null)),
          appliedDate: status === 'APPLIED' ? appliedDate : null,
          lot: nz(lot),
          place: nz(place),
          reaction: nz(reaction),
          notes: nz(notes),
        });
        await photos.commit(babyId, id);
        showToast('Vacina salva.');
        goBack();
      }}
      onDelete={
        initial
          ? async () => {
              await repos.records.delete('VACCINE', initial.id!);
              showToast('Vacina excluída.');
              goBack();
            }
          : undefined
      }
    >
      <TextField label="Nome da vacina" value={name} onChange={setName} required autoFocus={!initial} />
      {!initial && (
        <ChoiceChips
          legend="Sugestões"
          value={EXTRA_VACCINES.includes(name) ? name : null}
          onChange={(v) => v && setName(v)}
          options={EXTRA_VACCINES.map((v) => ({ value: v, label: v }))}
        />
      )}
      <TextField label="Dose (ex.: 1ª dose, reforço)" value={dose} onChange={setDose} />
      <ChoiceChips legend="Situação" value={status} onChange={(v) => v && setStatus(v)} options={VACCINE_STATUS_OPTIONS} />
      {status === 'APPLIED' && <DateField label="Data da aplicação" value={appliedDate} onChange={setAppliedDate} required />}
      {status === 'SCHEDULED' && <DateField label="Data prevista" value={scheduledDate} onChange={setScheduledDate} required />}
      {status === 'APPLIED' && (
        <>
          <TextField label="Lote (opcional)" value={lot} onChange={setLot} />
          <TextField label="Local (opcional)" value={place} onChange={setPlace} />
          <TextField label="Reação observada (opcional)" value={reaction} onChange={setReaction} />
        </>
      )}
      <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
      <PhotoAttachments draft={photos} label="Foto da caderneta" />
      <Card>
        <p className="small-text">{Texts.VACCINE_NOTE}</p>
      </Card>
    </FormPage>
  );
}

// ---------------------------------------------------------------------------
// Consulta
// ---------------------------------------------------------------------------

const APPOINTMENT_QUICK = ['Pediatra', 'Teste do pezinho', 'Teste da orelhinha', 'Teste do olhinho'];

export function AppointmentForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getAppointment);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <AppointmentBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function AppointmentBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: Appointment }) {
  const [title, setTitle] = useState(initial?.title ?? '');
  const [date, setDate] = useState(initial ? dateOf(initial.dateTime) : (target.date ?? today()));
  const [time, setTime] = useState(initial ? timeOf(initial.dateTime) : '09:00');
  const [status, setStatus] = useState<AppointmentStatus>(initial?.status ?? 'SCHEDULED');
  const [professional, setProfessional] = useState(initial?.professional ?? '');
  const [place, setPlace] = useState(initial?.place ?? '');
  const [reason, setReason] = useState(initial?.reason ?? '');
  const [guidance, setGuidance] = useState(initial?.guidance ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const photos = usePhotoDraft('APPOINTMENT', initial?.id);

  const dateTime = atTime(date || today(), time || '00:00');
  const askDone = status === 'SCHEDULED' && !!date && dateTime < now();
  const errors = [!title.trim() && 'Informe o tipo de consulta ou especialidade.', !date && 'Informe a data.'].filter(Boolean) as string[];

  return (
    <FormPage
      title={initial ? 'Editar consulta' : 'Nova consulta'}
      errors={errors}
      onSave={async () => {
        const id = await repos.records.saveAppointment({
          ...(initial ?? {}),
          babyId,
          title: title.trim(),
          dateTime,
          status,
          professional: nz(professional),
          place: nz(place),
          reason: nz(reason),
          guidance: nz(guidance),
          notes: nz(notes),
        });
        await photos.commit(babyId, id);
        showToast('Consulta salva.');
        goBack();
      }}
      onDelete={
        initial
          ? async () => {
              await repos.records.delete('APPOINTMENT', initial.id!);
              showToast('Consulta excluída.');
              goBack();
            }
          : undefined
      }
    >
      <TextField label="Consulta ou especialidade" value={title} onChange={setTitle} required autoFocus={!initial} />
      <ChoiceChips
        legend="Sugestões"
        value={APPOINTMENT_QUICK.includes(title) ? title : null}
        onChange={(v) => v && setTitle(v)}
        options={APPOINTMENT_QUICK.map((v) => ({ value: v, label: v }))}
      />
      <div className="field-row">
        <DateField label="Data" value={date} onChange={setDate} required />
        <TimeField label="Horário" value={time} onChange={setTime} />
      </div>
      {askDone && (
        <Card tone="warn">
          <p>Essa consulta já passou. Ela foi realizada?</p>
          <div className="btn-row">
            <button type="button" className="btn small" onClick={() => setStatus('DONE')}>
              Sim, foi realizada
            </button>
            <button type="button" className="btn outline small" onClick={() => setStatus('CANCELED')}>
              Foi cancelada
            </button>
          </div>
        </Card>
      )}
      <ChoiceChips
        legend="Situação"
        value={status}
        onChange={(v) => v && setStatus(v)}
        options={APPOINTMENT_STATUSES.map((s) => ({ value: s, label: appointmentStatusLabel[s] }))}
      />
      <TextField label="Profissional (opcional)" value={professional} onChange={setProfessional} />
      <TextField label="Local (opcional)" value={place} onChange={setPlace} />
      <TextField label="Motivo (opcional)" value={reason} onChange={setReason} />
      <TextField label="Orientações recebidas" value={guidance} onChange={setGuidance} multiline />
      <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
      <PhotoAttachments draft={photos} hint="Receitas, pedidos de exame ou resultados. Ficam só neste aparelho." />
    </FormPage>
  );
}
