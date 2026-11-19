import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { BabyAge } from '../../domain/babyAge';
import { dateOf, now, plusDays, startOfDay, timeOf, today } from '../../domain/dates';
import { FeedingSummaries } from '../../domain/feedingSummary';
import { Formats } from '../../domain/formats';
import { Texts } from '../../domain/texts';
import { vaccineDisplayLabel, vaccineDisplayStatus, vaccineReferenceDate, vaccineTitle } from '../../domain/vaccineRules';
import type { Appointment, FeedingType, LocalDate, MeasurementType, Vaccine } from '../../domain/types';
import { Card, DisclaimerCard, EmptyHint, Loading, Row, Section, StatTile, StatusPill, TimelineRow, TypeBadge } from '../components';
import { useBabyContext } from '../context';
import { Icon } from '../icons';
import { Link } from '../link';
import { formPath, navigate } from '../router';
import { NoBaby, VACCINE_STATUS_COLOR, VACCINE_STATUS_ICON } from './shared';

/** Vacinas atrasadas, pendentes ou previstas para os próximos 15 dias. */
function vaccinesDue(vaccines: Vaccine[], day: LocalDate): Vaccine[] {
  return vaccines
    .filter((v) => {
      const status = vaccineDisplayStatus(v, day);
      if (status === 'OVERDUE' || status === 'PENDING') return true;
      return status === 'PLANNED' && !!v.scheduledDate && v.scheduledDate <= plusDays(day, 15);
    })
    .sort((a, b) => (vaccineReferenceDate(a) ?? '').localeCompare(vaccineReferenceDate(b) ?? ''))
    .slice(0, 6);
}

export function TodayScreen() {
  const { baby } = useBabyContext();
  const day = today();
  const state = useLive(async () => {
    if (!baby) return null;
    const id = baby.id!;
    const [items, appointments, vaccines, feedings, growth] = await Promise.all([
      repos.timeline.range(id, day, day, day),
      repos.records.upcomingAppointments(id, startOfDay(day), 5),
      repos.records.listVaccines(id),
      repos.records.listFeedingsOn(id, day),
      repos.records.listGrowth(id),
    ]);
    return {
      items,
      appointments,
      due: vaccinesDue(vaccines, day),
      feeding: FeedingSummaries.summarize(feedings),
      lastWeight: growth.find((g) => g.type === 'WEIGHT'),
      lastHeight: growth.find((g) => g.type === 'HEIGHT'),
    };
  }, [baby?.id, day]);

  if (!baby) {
    return (
      <>
        <NoBaby>{Texts.WELCOME}</NoBaby>
        <Card>
          <p>{Texts.PRIVACY_SUMMARY}</p>
        </Card>
        <DisclaimerCard />
      </>
    );
  }
  if (state.loading || !state.value) return <Loading />;
  const { items, appointments, due, feeding, lastWeight, lastHeight } = state.value;
  const openGrowth = (type: MeasurementType) => navigate(formPath({ type: 'GROWTH', preset: type }));
  const upcomingCount = appointments.length + due.length;

  return (
    <>
      <h1 className="sr-only">Hoje</h1>
      <Card tone="primary">
        <p className="label-text">{Formats.longDate(day)}</p>
        <h2 style={{ fontSize: '1.375rem' }}>{BabyAge.sentence(baby.name, baby.birthDate, day)}</h2>
        <div className="stats">
          <StatTile
            value={lastWeight ? Formats.measurement(lastWeight.type, lastWeight.value) : '—'}
            label={lastWeight ? `Peso em ${Formats.shortDate(lastWeight.date)}` : 'Peso'}
            onClick={() => openGrowth('WEIGHT')}
            onClickLabel="Registrar peso"
          />
          <StatTile
            value={lastHeight ? Formats.measurement(lastHeight.type, lastHeight.value) : '—'}
            label={lastHeight ? `Altura em ${Formats.shortDate(lastHeight.date)}` : 'Altura'}
            onClick={() => openGrowth('HEIGHT')}
            onClickLabel="Registrar altura"
          />
        </div>
      </Card>

      <Section title="Próximos compromissos" subtitle={upcomingCount === 0 ? 'Tudo em dia' : `${upcomingCount} item(ns)`} icon={<Icon name="event" />}>
        {upcomingCount === 0 && <EmptyHint>Nenhuma consulta ou vacina para os próximos dias. Toque em Registrar para agendar uma consulta.</EmptyHint>}
        {appointments.map((a) => (
          <AppointmentRow key={a.id} appointment={a} today={day} />
        ))}
        {due.map((v) => (
          <VaccineDueRow key={v.id} vaccine={v} today={day} />
        ))}
      </Section>

      <Section title="Registros de hoje" subtitle={items.length === 0 ? 'Nada ainda' : `${items.length} registro(s)`} icon={<Icon name="today" />}>
        {items.length === 0 && <EmptyHint>Nada registrado hoje ainda. Toque em Registrar para começar.</EmptyHint>}
        {items.map((item) => (
          <TimelineRow key={`${item.type}-${item.id}`} item={item} />
        ))}
      </Section>

      <Section title="Alimentação de hoje" subtitle={feeding.total === 0 ? 'Nenhum registro' : `${feeding.total} registro(s)`} icon={<Icon name="food" />}>
        <div className="stats" style={{ padding: '0 8px' }}>
          <StatTile value={String(feeding.breastfeedings)} label={`mamadas (${feeding.breastMinutes} min)`} />
          <StatTile value={`${feeding.formulaMl} ml`} label="fórmula" />
          <StatTile value={String(feeding.solidMeals)} label="refeições" />
        </div>
        <div className="btn-row" style={{ padding: '0 8px' }}>
          {(
            [
              ['BREAST_MILK', 'Mamou agora'],
              ['FORMULA', 'Fórmula'],
              ['SOLID_MEAL', 'Comida'],
            ] as [FeedingType, string][]
          ).map(([type, label]) => (
            <Link key={type} className="btn tonal small" to={formPath({ type: 'FEEDING', date: day, preset: type })}>
              {label}
            </Link>
          ))}
        </div>
      </Section>

      <DisclaimerCard />
    </>
  );
}

function AppointmentRow({ appointment, today: day }: { appointment: Appointment; today: LocalDate }) {
  const date = dateOf(appointment.dateTime);
  const time = Formats.time(timeOf(appointment.dateTime));
  const when = date === day ? `Hoje às ${time}` : date === plusDays(day, 1) ? `Amanhã às ${time}` : Formats.dateTime(appointment.dateTime);
  const past = appointment.dateTime < now();
  return (
    <Row
      href={`#/${formPath({ type: 'APPOINTMENT', id: appointment.id })}`}
      badge={<TypeBadge type="APPOINTMENT" />}
      overline="Consulta"
      headline={appointment.title}
      support={past ? `${when} · Foi realizada? Toque para atualizar.` : when}
      actionLabel="Abrir consulta"
    />
  );
}

function VaccineDueRow({ vaccine, today: day }: { vaccine: Vaccine; today: LocalDate }) {
  const status = vaccineDisplayStatus(vaccine, day);
  return (
    <Row
      href={`#/${formPath({ type: 'VACCINE', id: vaccine.id })}`}
      badge={<TypeBadge type="VACCINE" />}
      overline="Vacina"
      headline={vaccineTitle(vaccine)}
      support={vaccine.scheduledDate ? `Prevista para ${Formats.date(vaccine.scheduledDate)}` : undefined}
      trail={<StatusPill label={vaccineDisplayLabel[status]} color={VACCINE_STATUS_COLOR[status]} icon={VACCINE_STATUS_ICON[status]} />}
      actionLabel="Abrir vacina"
    />
  );
}
