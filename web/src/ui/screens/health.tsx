import { useState } from 'react';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { now, today } from '../../domain/dates';
import { Formats } from '../../domain/formats';
import { appointmentStatusLabel, symptomLabel } from '../../domain/labels';
import { Texts } from '../../domain/texts';
import { healthToTimelineItem, medicationReferenceTime, medicationStatusText, medicationToTimelineItem } from '../../domain/timeline';
import { VaccineSchedule, vaccineDisplayLabel, vaccineDisplayStatus, vaccineReferenceDate, vaccineTitle } from '../../domain/vaccineRules';
import { SYMPTOMS, type Appointment, type MedicationDose, type Symptom, type Vaccine } from '../../domain/types';
import { Card, EmptyHint, Loading, Row, Section, StatusPill, TabPanel, Tabs, TypeBadge, showToast } from '../components';
import { useBabyContext } from '../context';
import { SelectField } from '../fields';
import { Icon } from '../icons';
import { Link } from '../link';
import { formPath } from '../router';
import { NoBaby, VACCINE_STATUS_COLOR, VACCINE_STATUS_ICON } from './shared';

type HealthTab = 'vacinas' | 'consultas' | 'medicacoes' | 'sintomas';
const TAB_LIST: { value: HealthTab; label: string }[] = [
  { value: 'vacinas', label: 'Vacinas' },
  { value: 'consultas', label: 'Consultas' },
  { value: 'medicacoes', label: 'Medicações' },
  { value: 'sintomas', label: 'Sintomas' },
];

export function HealthScreen() {
  const { baby } = useBabyContext();
  const [tab, setTab] = useState<HealthTab>('vacinas');
  if (!baby) return <NoBaby>Cadastre o bebê para registrar vacinas, consultas e sintomas.</NoBaby>;
  return (
    <>
      <h1 className="sr-only">Saúde</h1>
      <Tabs tabs={TAB_LIST} value={tab} onChange={setTab} label="Seções de saúde" />
      <TabPanel id={tab}>
        {tab === 'vacinas' && <Vaccines babyId={baby.id!} />}
        {tab === 'consultas' && <Appointments babyId={baby.id!} />}
        {tab === 'medicacoes' && <Medications babyId={baby.id!} />}
        {tab === 'sintomas' && <Symptoms babyId={baby.id!} />}
      </TabPanel>
    </>
  );
}

// ---------------------------------------------------------------------------
// Vacinas
// ---------------------------------------------------------------------------

function Vaccines({ babyId }: { babyId: number }) {
  const { baby } = useBabyContext();
  const day = today();
  const list = useLive(() => repos.records.listVaccines(babyId), [babyId]).value;
  if (!list) return <Loading />;

  const groups = new Map<number | null, Vaccine[]>();
  for (const v of list) {
    const key = v.recommendedAgeMonths ?? null;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(v);
  }

  return (
    <>
      <div className="btn-row">
        <Link className="btn tonal" to={formPath({ type: 'VACCINE' })}>
          <Icon name="add" size={20} />
          Nova vacina
        </Link>
        <button
          type="button"
          className="btn outline"
          onClick={async () => {
            const added = await repos.records.addSuggestedVaccines(baby!);
            showToast(added === 0 ? 'O calendário sugerido já está completo.' : `${added} vacinas previstas foram adicionadas.`);
          }}
        >
          Adicionar calendário sugerido
        </button>
      </div>
      {list.length === 0 && <EmptyHint>Nenhuma vacina registrada. Adicione o calendário sugerido para ver as próximas doses.</EmptyHint>}
      {[...groups].map(([months, vaccines]) => {
        const applied = vaccines.filter((v) => v.status === 'APPLIED').length;
        const overdue = vaccines.filter((v) => vaccineDisplayStatus(v, day) === 'OVERDUE').length;
        const open = vaccines.some((v) => ['OVERDUE', 'PENDING'].includes(vaccineDisplayStatus(v, day)));
        return (
          <Section
            key={months ?? 'outras'}
            title={VaccineSchedule.ageGroupLabel(months)}
            subtitle={`${applied} de ${vaccines.length} aplicadas${overdue > 0 ? ` · ${overdue} atrasada(s)` : ''}`}
            icon={<TypeBadge type="VACCINE" />}
            defaultOpen={open}
          >
            {vaccines.map((v) => {
              const status = vaccineDisplayStatus(v, day);
              const date = vaccineReferenceDate(v);
              return (
                <Row
                  key={v.id}
                  href={`#/${formPath({ type: 'VACCINE', id: v.id })}`}
                  headline={vaccineTitle(v)}
                  support={date ? `${v.status === 'APPLIED' ? 'Aplicada em' : 'Prevista para'} ${Formats.date(date)}` : undefined}
                  trail={<StatusPill label={vaccineDisplayLabel[status]} color={VACCINE_STATUS_COLOR[status]} icon={VACCINE_STATUS_ICON[status]} />}
                  actionLabel="Abrir vacina"
                />
              );
            })}
          </Section>
        );
      })}
      <p className="small-text muted">{Texts.VACCINE_NOTE}</p>
    </>
  );
}

// ---------------------------------------------------------------------------
// Consultas
// ---------------------------------------------------------------------------

function Appointments({ babyId }: { babyId: number }) {
  const list = useLive(() => repos.records.listAppointments(babyId), [babyId]).value;
  if (!list) return <Loading />;
  const at = now();
  const upcoming = list.filter((a) => a.status === 'SCHEDULED' && a.dateTime >= at).sort((a, b) => a.dateTime.localeCompare(b.dateTime));
  const history = list.filter((a) => !upcoming.includes(a));

  const row = (a: Appointment) => {
    const needsConfirm = a.status === 'SCHEDULED' && a.dateTime < at;
    return (
      <Row
        key={a.id}
        href={`#/${formPath({ type: 'APPOINTMENT', id: a.id })}`}
        badge={<TypeBadge type="APPOINTMENT" />}
        overline={Formats.dateTime(a.dateTime)}
        headline={a.title}
        support={[a.status !== 'SCHEDULED' ? appointmentStatusLabel[a.status] : needsConfirm ? 'Foi realizada? Toque para atualizar.' : null, a.professional, a.place].filter(Boolean).join(' · ')}
        actionLabel="Abrir consulta"
      />
    );
  };

  return (
    <>
      <div className="btn-row">
        <Link className="btn tonal" to={formPath({ type: 'APPOINTMENT' })}>
          <Icon name="add" size={20} />
          Nova consulta
        </Link>
      </div>
      <Section title={`Próximas (${upcoming.length})`} icon={<Icon name="event" />} defaultOpen>
        {upcoming.length === 0 ? <EmptyHint>Nenhuma consulta agendada. Toque em Nova consulta para marcar a próxima.</EmptyHint> : upcoming.map(row)}
      </Section>
      <Section title={`Histórico (${history.length})`} icon={<Icon name="clock" />}>
        {history.length === 0 ? <EmptyHint>Nenhuma consulta realizada ou cancelada ainda.</EmptyHint> : history.map(row)}
      </Section>
    </>
  );
}

// ---------------------------------------------------------------------------
// Medicamentos
// ---------------------------------------------------------------------------

function Medications({ babyId }: { babyId: number }) {
  const list = useLive(() => repos.records.listMedications(babyId), [babyId]).value;
  if (!list) return <Loading />;
  const at = now();
  // Doses ainda sem confirmação (inclusive as que já passaram do horário) ficam em "Próximas".
  const upcoming = list.filter((m) => m.status === 'SCHEDULED').sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt));
  const history = list.filter((m) => m.status !== 'SCHEDULED').sort((a, b) => medicationReferenceTime(b).localeCompare(medicationReferenceTime(a)));

  const giveNow = async (m: MedicationDose) => {
    await repos.records.saveMedication({ ...m, status: 'GIVEN', givenAt: now() });
    showToast('Dose registrada como realizada.');
  };

  const row = (m: MedicationDose) => {
    const item = medicationToTimelineItem(m, at);
    const overdue = m.status === 'SCHEDULED' && m.scheduledAt < at;
    return (
      <Row
        key={m.id}
        href={`#/${formPath({ type: 'MEDICATION', id: m.id })}`}
        badge={<TypeBadge type="MEDICATION" />}
        overline={Formats.dateTime(medicationReferenceTime(m))}
        headline={item.title}
        support={medicationStatusText(m, at)}
        trail={overdue ? <span className="pill" style={{ color: 'var(--pending)' }}>Não registrado</span> : undefined}
        actionLabel="Abrir dose de medicamento"
      />
    );
  };

  return (
    <>
      <div className="btn-row">
        <Link className="btn tonal" to={formPath({ type: 'MEDICATION' })}>
          <Icon name="add" size={20} />
          Agendar medicamento
        </Link>
        <Link className="btn outline" to={formPath({ type: 'MEDICATION', preset: 'given' })}>
          Registrar dose dada
        </Link>
      </div>
      <Section title={`Próximas (${upcoming.length})`} icon={<Icon name="pill" />} defaultOpen>
        {upcoming.length === 0 ? (
          <EmptyHint>Nenhuma dose agendada. Toque em Agendar medicamento para marcar os horários.</EmptyHint>
        ) : (
          upcoming.map((m) => (
            <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <div style={{ flex: 1, minWidth: 0 }}>{row(m)}</div>
              <button type="button" className="btn tonal small" onClick={() => void giveNow(m)}>
                Realizar agora
              </button>
            </div>
          ))
        )}
      </Section>
      <Section title={`Histórico (${history.length})`} icon={<Icon name="clock" />}>
        {history.length === 0 ? <EmptyHint>Nenhuma dose registrada ainda.</EmptyHint> : history.map(row)}
      </Section>
      <p className="small-text muted">{Texts.MEDICATION_NOTE}</p>
    </>
  );
}

// ---------------------------------------------------------------------------
// Sintomas
// ---------------------------------------------------------------------------

function Symptoms({ babyId }: { babyId: number }) {
  const [filter, setFilter] = useState<Symptom | 'ALL'>('ALL');
  const list = useLive(() => repos.records.listHealthEvents(babyId), [babyId]).value;
  if (!list) return <Loading />;
  const shown = filter === 'ALL' ? list : list.filter((e) => e.symptoms.includes(filter));
  return (
    <>
      <div className="btn-row">
        <Link className="btn tonal" to={formPath({ type: 'HEALTH_EVENT' })}>
          <Icon name="add" size={20} />
          Registrar sintoma
        </Link>
      </div>
      <SelectField
        label="Filtrar por sintoma"
        value={filter}
        onChange={setFilter}
        options={[{ value: 'ALL', label: 'Todos os sintomas' }, ...SYMPTOMS.map((s) => ({ value: s, label: symptomLabel[s] }))]}
      />
      {shown.length === 0 ? (
        <EmptyHint>Nenhum sintoma registrado. Se notar algum sinal no bebê, toque em Registrar sintoma.</EmptyHint>
      ) : (
        shown.map((e) => {
          const item = healthToTimelineItem(e);
          return (
            <Row
              key={e.id}
              href={`#/${formPath({ type: 'HEALTH_EVENT', id: e.id })}`}
              badge={<TypeBadge type="HEALTH_EVENT" />}
              overline={Formats.dateTime(e.dateTime)}
              headline={item.title}
              support={item.detail}
              actionLabel="Abrir registro de sintoma"
            />
          );
        })
      )}
      <Card>
        <p className="small-text">{Texts.DISCLAIMER}</p>
      </Card>
    </>
  );
}
