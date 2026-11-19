import { useState } from 'react';
import { repos } from '../../data/repo';
import { AttentionSigns } from '../../domain/attentionSigns';
import { atTime, dateOf, minutesBetween, now, timeOf, today } from '../../domain/dates';
import { Formats } from '../../domain/formats';
import {
  acceptanceLabel,
  breastSideLabel,
  feedingTypeLabel,
  foodTextureLabel,
  intensityLabel,
  medicationStatusLabel,
  symptomLabel,
} from '../../domain/labels';
import { MedicationSchedule } from '../../domain/medicationSchedule';
import { Plausibility } from '../../domain/plausibility';
import { Texts } from '../../domain/texts';
import { isMedicationOverdue } from '../../domain/timeline';
import {
  ACCEPTANCES,
  BREAST_SIDES,
  FEEDING_TYPES,
  FOOD_TEXTURES,
  INTENSITIES,
  MEDICATION_STATUSES,
  SYMPTOMS,
  type Feeding,
  type FeedingType,
  type HealthEvent,
  type MedicationDose,
  type MedicationStatus,
  type Symptom,
} from '../../domain/types';
import { Card, ConfirmDialog, Dialog, Loading, showToast } from '../components';
import { CheckField, ChoiceChips, DateField, MultiChips, TextField, TimeField } from '../fields';
import { PhotoAttachments, usePhotoDraft } from '../photos';
import { useBabyContext } from '../context';
import { goBack, type FormTarget } from '../router';
import { FormPage, nz, NeedsBaby, useLoaded } from './common';

// ---------------------------------------------------------------------------
// Sintomas e mal-estar
// ---------------------------------------------------------------------------

export function HealthEventForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getHealthEvent);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <HealthBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function HealthBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: HealthEvent }) {
  const { baby } = useBabyContext();
  const [date, setDate] = useState(initial ? dateOf(initial.dateTime) : (target.date ?? today()));
  const [time, setTime] = useState(initial ? timeOf(initial.dateTime) : timeOf(now()));
  const [symptoms, setSymptoms] = useState<Symptom[]>(initial?.symptoms ?? []);
  const [other, setOther] = useState(initial?.otherSymptom ?? '');
  const [intensity, setIntensity] = useState(initial?.intensity ?? 'MILD');
  const [temperature, setTemperature] = useState(initial?.temperatureCelsius != null ? Formats.decimal(initial.temperatureCelsius, 1) : '');
  const [duration, setDuration] = useState(initial?.duration ?? '');
  const [actions, setActions] = useState(initial?.actionsTaken ?? '');
  const [sought, setSought] = useState(initial?.soughtCare ?? false);
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [signs, setSigns] = useState<string[] | null>(null);
  const [tempWarning, setTempWarning] = useState<string | null>(null);
  const photos = usePhotoDraft('HEALTH_EVENT', initial?.id);

  const temp = Formats.parseDecimal(temperature);
  const tempInvalid = temperature.trim() !== '' && temp === null;
  const errors = [
    symptoms.length === 0 && !other.trim() && 'Escolha pelo menos um sintoma.',
    tempInvalid && 'Digite só números na temperatura, ex.: 37,8',
    !date && 'Informe a data.',
  ].filter(Boolean) as string[];

  const persist = async () => {
    const event: HealthEvent = {
      ...(initial ?? {}),
      babyId,
      dateTime: atTime(date, time || '00:00'),
      symptoms: symptoms.length === 0 ? ['OTHER'] : symptoms,
      otherSymptom: nz(other),
      intensity,
      temperatureCelsius: temp,
      duration: nz(duration),
      actionsTaken: nz(actions),
      soughtCare: sought,
      notes: nz(notes),
    };
    const id = await repos.records.saveHealthEvent(event);
    await photos.commit(babyId, id);
    // Depois de salvar, mostra os sinais de atenção (se houver), sempre com o aviso educativo.
    const found = baby ? AttentionSigns.evaluate(event, baby.birthDate) : [];
    if (found.length > 0) setSigns(found);
    else {
      showToast('Registro salvo.');
      goBack();
    }
  };

  return (
    <>
      <FormPage
        title={initial ? 'Editar sintoma' : 'Registrar sintoma'}
        errors={errors}
        onSave={() => {
          const w = temp !== null ? Plausibility.temperatureWarning(temp) : null;
          if (w) {
            setTempWarning(w);
            return;
          }
          return persist();
        }}
        onDelete={
          initial
            ? async () => {
                await repos.records.delete('HEALTH_EVENT', initial.id!);
                showToast('Registro excluído.');
                goBack();
              }
            : undefined
        }
      >
        <div className="field-row">
          <DateField label="Data" value={date} onChange={setDate} required />
          <TimeField label="Hora" value={time} onChange={setTime} />
        </div>
        <MultiChips
          legend="O que você observou?"
          values={symptoms}
          onChange={setSymptoms}
          options={SYMPTOMS.map((s) => ({ value: s, label: symptomLabel[s] }))}
        />
        <TextField label="Outro sintoma (opcional)" value={other} onChange={setOther} />
        <ChoiceChips legend="Intensidade" value={intensity} onChange={(v) => v && setIntensity(v)} options={INTENSITIES.map((i) => ({ value: i, label: intensityLabel[i] }))} />
        <TextField label="Temperatura (opcional)" value={temperature} onChange={setTemperature} inputMode="decimal" hint="Medida na axila, ex.: 37,8" error={tempInvalid ? 'Digite só números, ex.: 37,8' : null} />
        <TextField label="Há quanto tempo? (opcional)" value={duration} onChange={setDuration} hint="Ex.: desde a manhã, 2 dias" />
        <TextField label="O que foi feito (opcional)" value={actions} onChange={setActions} multiline />
        <CheckField label="Procurou atendimento" checked={sought} onChange={setSought} />
        <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
        <PhotoAttachments draft={photos} hint="Ex.: manchas na pele. A foto fica só neste aparelho." />
      </FormPage>
      <ConfirmDialog
        open={tempWarning !== null}
        title="Temperatura fora do comum"
        message={tempWarning}
        confirmLabel="Salvar mesmo assim"
        cancelLabel="Corrigir"
        onCancel={() => setTempWarning(null)}
        onConfirm={() => {
          setTempWarning(null);
          void persist();
        }}
      />
      <Dialog
        open={signs !== null}
        onClose={goBack}
        title={Texts.ATTENTION_TITLE}
        actions={
          <button type="button" className="btn" onClick={goBack}>
            Entendi
          </button>
        }
      >
        <p style={{ fontWeight: 600 }}>Registro salvo.</p>
        <ul style={{ margin: 0, paddingLeft: 20 }}>
          {signs?.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ul>
        <p>{Texts.ATTENTION_ADVICE}</p>
      </Dialog>
    </>
  );
}

// ---------------------------------------------------------------------------
// Alimentação
// ---------------------------------------------------------------------------

export function FeedingForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.records.getFeeding);
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <FeedingBody babyId={babyId} target={target} initial={value} />}</NeedsBaby>;
}

function FeedingBody({ babyId, target, initial }: { babyId: number; target: FormTarget; initial?: Feeding }) {
  const preset = FEEDING_TYPES.find((t) => t === target.preset);
  const [type, setType] = useState<FeedingType>(initial?.type ?? preset ?? 'BREAST_MILK');
  const [date, setDate] = useState(initial ? dateOf(initial.dateTime) : (target.date ?? today()));
  const [time, setTime] = useState(initial ? timeOf(initial.dateTime) : timeOf(now()));
  const [side, setSide] = useState(initial?.breastSide ?? null);
  const [minutes, setMinutes] = useState(initial?.durationMinutes != null ? String(initial.durationMinutes) : '');
  const [amount, setAmount] = useState(initial?.amountMl != null ? String(initial.amountMl) : '');
  const [food, setFood] = useState(initial?.food ?? '');
  const [texture, setTexture] = useState(initial?.texture ?? null);
  const [acceptance, setAcceptance] = useState(initial?.acceptance ?? null);
  const [firstTime, setFirstTime] = useState(initial?.firstTime ?? false);
  const [reaction, setReaction] = useState(initial?.reaction ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const photos = usePhotoDraft('FEEDING', initial?.id);

  const isBreast = type === 'BREAST_MILK';
  const hasAmount = type !== 'FRUIT' && type !== 'SOLID_MEAL';
  const isFoodType = type === 'FRUIT' || type === 'SOLID_MEAL' || type === 'OTHER';
  const isSolidType = type === 'FRUIT' || type === 'SOLID_MEAL';
  const min = Formats.parseInt(minutes);
  const ml = Formats.parseInt(amount);

  const errors = [
    isFoodType && !food.trim() && 'Conte o que foi oferecido.',
    minutes.trim() !== '' && (min === null || min < 0) && 'Duração: digite só números, em minutos.',
    amount.trim() !== '' && (ml === null || ml < 0) && 'Quantidade: digite só números, em ml.',
    !date && 'Informe a data.',
  ].filter(Boolean) as string[];

  return (
    <FormPage
      title={initial ? 'Editar alimentação' : 'Registrar alimentação'}
      errors={errors}
      onSave={async () => {
        const id = await repos.records.saveFeeding({
          ...(initial ?? {}),
          babyId,
          dateTime: atTime(date, time || '00:00'),
          type,
          breastSide: isBreast ? side : null,
          durationMinutes: isBreast ? min : null,
          amountMl: hasAmount ? ml : null,
          food: isFoodType ? nz(food) : null,
          texture: isSolidType ? texture : null,
          acceptance: isFoodType ? acceptance : null,
          firstTime: isFoodType ? firstTime : false,
          reaction: isFoodType ? nz(reaction) : null,
          notes: nz(notes),
        });
        await photos.commit(babyId, id);
        showToast('Alimentação salva.');
        goBack();
      }}
      onDelete={
        initial
          ? async () => {
              await repos.records.delete('FEEDING', initial.id!);
              showToast('Registro excluído.');
              goBack();
            }
          : undefined
      }
    >
      <ChoiceChips legend="Alimentação" value={type} onChange={(v) => v && setType(v)} options={FEEDING_TYPES.map((t) => ({ value: t, label: feedingTypeLabel[t] }))} />
      <div className="field-row">
        <DateField label="Data" value={date} onChange={setDate} required />
        <TimeField label="Hora" value={time} onChange={setTime} />
      </div>
      {isBreast && (
        <>
          <ChoiceChips legend="Lado (opcional)" value={side} onChange={setSide} options={BREAST_SIDES.map((s) => ({ value: s, label: breastSideLabel[s] }))} allowNone />
          <TextField label="Duração em minutos (opcional)" value={minutes} onChange={setMinutes} inputMode="numeric" />
        </>
      )}
      {isFoodType && (
        <TextField label={type === 'FRUIT' ? 'Qual fruta?' : 'O que foi oferecido?'} value={food} onChange={setFood} required />
      )}
      {isSolidType && (
        <ChoiceChips legend="Forma (opcional)" value={texture} onChange={setTexture} options={FOOD_TEXTURES.map((t) => ({ value: t, label: foodTextureLabel[t] }))} allowNone />
      )}
      {hasAmount && (
        <TextField
          label="Quantidade em ml (opcional)"
          value={amount}
          onChange={setAmount}
          inputMode="numeric"
          hint={isBreast ? 'Se foi leite materno na mamadeira ou no copinho.' : undefined}
        />
      )}
      {isFoodType && (
        <>
          <ChoiceChips legend="Aceitação (opcional)" value={acceptance} onChange={setAcceptance} options={ACCEPTANCES.map((a) => ({ value: a, label: acceptanceLabel[a] }))} allowNone />
          <CheckField label="Primeira vez com este alimento" hint="Ajuda a acompanhar a introdução alimentar." checked={firstTime} onChange={setFirstTime} />
          <TextField label="Reação observada (opcional)" value={reaction} onChange={setReaction} hint="Ex.: manchas na pele, vômito, mudança nas fezes." />
        </>
      )}
      <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
      <PhotoAttachments draft={photos} />
    </FormPage>
  );
}

// ---------------------------------------------------------------------------
// Medicamento
// ---------------------------------------------------------------------------

export function MedicationForm({ target }: { target: FormTarget }) {
  // Busca a dose e quantas doses da mesma série ainda vêm depois dela.
  const { loading, value } = useLoaded(target.id, async (id) => {
    const dose = await repos.records.getMedication(id);
    return dose ? { dose, followingCount: (await repos.records.followingInSeries(dose)).length } : undefined;
  });
  if (loading) return <Loading />;
  return <NeedsBaby>{(babyId) => <MedicationBody babyId={babyId} target={target} initial={value?.dose} followingCount={value?.followingCount ?? 0} />}</NeedsBaby>;
}

type Scope = 'one' | 'following';

function MedicationBody({ babyId, target, initial, followingCount }: { babyId: number; target: FormTarget; initial?: MedicationDose; followingCount: number }) {
  const givenPreset = target.preset === 'given';
  const [name, setName] = useState(initial?.name ?? '');
  const [dosage, setDosage] = useState(initial?.dosage ?? '');
  const [status, setStatus] = useState<MedicationStatus>(initial?.status ?? (givenPreset ? 'GIVEN' : 'SCHEDULED'));
  const [schedDate, setSchedDate] = useState(initial ? dateOf(initial.scheduledAt) : (target.date ?? today()));
  const [schedTime, setSchedTime] = useState(initial ? timeOf(initial.scheduledAt) : timeOf(now()));
  const [givenDate, setGivenDate] = useState(initial?.givenAt ? dateOf(initial.givenAt) : today());
  const [givenTime, setGivenTime] = useState(initial?.givenAt ? timeOf(initial.givenAt) : timeOf(now()));
  const [repeat, setRepeat] = useState(false);
  const [interval, setIntervalText] = useState('8');
  const [days, setDays] = useState('7');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [ask, setAsk] = useState<null | { kind: 'save' | 'delete' }>(null);

  const scheduledAt = atTime(schedDate || today(), schedTime || '00:00');
  const givenAt = atTime(givenDate || today(), givenTime || '00:00');
  const overdue = initial ? isMedicationOverdue(initial, now()) : false;
  const canRepeat = !initial && status === 'SCHEDULED';
  const intervalN = Formats.parseInt(interval);
  const daysN = Formats.parseInt(days);
  const repeatError = canRepeat && repeat ? MedicationSchedule.validate(intervalN, daysN) : null;
  const times = canRepeat && repeat && !repeatError ? MedicationSchedule.times(scheduledAt, intervalN!, daysN!) : [];

  const errors = [!name.trim() && 'Informe o nome do medicamento.', !schedDate && 'Informe a data.', repeatError].filter(Boolean) as string[];

  const buildDose = (): MedicationDose => ({
    ...(initial ?? {}),
    babyId,
    name: name.trim(),
    dosage: nz(dosage),
    scheduledAt: status === 'GIVEN' && !initial && givenPreset ? givenAt : scheduledAt,
    status,
    givenAt: status === 'GIVEN' ? givenAt : null,
    notes: nz(notes),
  });

  const persist = async (scope: Scope) => {
    if (initial) {
      const updated = buildDose();
      await repos.records.saveMedication(updated);
      if (scope === 'following' && followingCount > 0) await repos.records.applyEditToFollowing(initial, updated);
    } else if (canRepeat && repeat) {
      const seriesId = await repos.records.nextSeriesId();
      await repos.records.saveMedicationDoses(times.map((t) => ({ ...buildDose(), scheduledAt: t, seriesId })));
    } else {
      await repos.records.saveMedication({ ...buildDose(), seriesId: null });
    }
    showToast('Medicamento salvo.');
    goBack();
  };

  const doDelete = async (scope: Scope) => {
    if (!initial) return;
    if (scope === 'following') await repos.records.deleteFollowingInSeries(initial);
    await repos.records.delete('MEDICATION', initial.id!);
    showToast('Medicamento excluído.');
    goBack();
  };

  const shifted = initial ? minutesBetween(initial.scheduledAt, scheduledAt) !== 0 : false;
  const changed = !!initial && (initial.name !== name.trim() || (initial.dosage ?? '') !== dosage.trim() || shifted || (initial.notes ?? '') !== notes.trim());

  return (
    <>
      <FormPage
        title={initial ? 'Editar medicamento' : givenPreset ? 'Registrar dose dada' : 'Novo medicamento'}
        errors={errors}
        onSave={() => {
          if (initial && followingCount > 0 && changed) {
            setAsk({ kind: 'save' });
            return;
          }
          return persist('one');
        }}
        onDelete={
          initial
            ? async () => {
                if (followingCount > 0) setAsk({ kind: 'delete' });
                else await doDelete('one');
              }
            : undefined
        }
      >
        {initial && followingCount > 0 && (
          <Card>
            <p className="small-text">
              Esta dose faz parte de uma série: há mais {followingCount} agendada(s) depois dela. Ao salvar mudanças, você escolhe se valem só para esta ou para as próximas.
            </p>
          </Card>
        )}
        {overdue && (
          <Card tone="warn">
            <p>Não registrado: o horário dessa dose já passou. Ela foi realizada?</p>
            <div className="btn-row">
              <button type="button" className="btn small" onClick={() => setStatus('GIVEN')}>
                Realizado
              </button>
              <button type="button" className="btn outline small" onClick={() => setStatus('SKIPPED')}>
                Não realizado
              </button>
            </div>
          </Card>
        )}
        <TextField label="Nome do medicamento" value={name} onChange={setName} required autoFocus={!initial} />
        <TextField label="Dose (opcional)" value={dosage} onChange={setDosage} hint="Como na receita. Ex.: 5 ml, 10 gotas" />
        <ChoiceChips
          legend="Situação"
          value={status}
          onChange={(v) => v && setStatus(v)}
          options={MEDICATION_STATUSES.map((s) => ({ value: s, label: medicationStatusLabel[s] }))}
        />
        {!(status === 'GIVEN' && !initial && givenPreset) && (
          <div className="field-row">
            <DateField label={status === 'GIVEN' ? 'Data prevista' : 'Data'} value={schedDate} onChange={setSchedDate} required />
            <TimeField label="Horário previsto" value={schedTime} onChange={setSchedTime} />
          </div>
        )}
        {status === 'GIVEN' && (
          <div className="field-row">
            <DateField label="Data em que foi dada" value={givenDate} onChange={setGivenDate} required />
            <TimeField label="Horário em que foi dada" value={givenTime} onChange={setGivenTime} />
          </div>
        )}
        {canRepeat && (
          <>
            <CheckField label="Repetir esse horário" hint="Agenda várias doses de uma vez, de tantas em tantas horas, até o limite de dias." checked={repeat} onChange={setRepeat} />
            {repeat && (
              <>
                <div className="field-row">
                  <TextField label="A cada quantas horas" value={interval} onChange={setIntervalText} inputMode="numeric" />
                  <TextField label="Por quantos dias" value={days} onChange={setDays} inputMode="numeric" hint={`Limite de ${MedicationSchedule.MAX_DAYS} dias.`} />
                </div>
                {times.length > 0 && (
                  <p className="muted" role="status">
                    Serão agendadas {times.length} doses, a última em {Formats.dateTime(times[times.length - 1])}.
                  </p>
                )}
              </>
            )}
          </>
        )}
        <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
        <Card>
          <p className="small-text">{Texts.MEDICATION_NOTE}</p>
        </Card>
      </FormPage>
      <Dialog
        open={ask !== null}
        onClose={() => setAsk(null)}
        title={ask?.kind === 'delete' ? 'Excluir quais doses?' : 'Aplicar a quais doses?'}
        actions={
          <>
            <button type="button" className="btn text" onClick={() => setAsk(null)}>
              Cancelar
            </button>
            <button
              type="button"
              className="btn outline"
              onClick={() => {
                const kind = ask!.kind;
                setAsk(null);
                void (kind === 'delete' ? doDelete('one') : persist('one'));
              }}
            >
              Só esta dose
            </button>
            <button
              type="button"
              className="btn"
              onClick={() => {
                const kind = ask!.kind;
                setAsk(null);
                void (kind === 'delete' ? doDelete('following') : persist('following'));
              }}
            >
              Esta e as próximas
            </button>
          </>
        }
      >
        <p>
          Essa dose faz parte de uma série. Há mais {followingCount} agendada(s) depois dela. Você quer {ask?.kind === 'delete' ? 'excluir só esta ou também as próximas?' : 'mudar só esta ou também as próximas?'}
        </p>
        {ask?.kind === 'save' && <p className="small-text muted">Se o horário mudou, as próximas mudam o mesmo tanto. Doses já realizadas não são alteradas.</p>}
      </Dialog>
    </>
  );
}
