import { useState } from 'react';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { minusDays, plusDays, timeOf, today } from '../../domain/dates';
import { FeedingSummaries } from '../../domain/feedingSummary';
import { Formats } from '../../domain/formats';
import { feedingTypeLabel } from '../../domain/labels';
import { describeFeeding } from '../../domain/timeline';
import type { Feeding, FeedingType } from '../../domain/types';
import { Card, EmptyHint, Loading, Row, Section, StatTile, TypeBadge } from '../components';
import { useBabyContext } from '../context';
import { Icon } from '../icons';
import { Link } from '../link';
import { formPath } from '../router';
import { NoBaby } from './shared';

const QUICK: [FeedingType, string][] = [
  ['BREAST_MILK', 'Mamou agora'],
  ['FORMULA', 'Fórmula'],
  ['FRUIT', 'Fruta'],
  ['SOLID_MEAL', 'Papa salgada'],
  ['WATER', 'Água'],
];

const GROUPS: { title: string; types: FeedingType[] }[] = [
  { title: 'Aleitamento materno', types: ['BREAST_MILK'] },
  { title: 'Fórmula', types: ['FORMULA'] },
  { title: 'Introdução alimentar', types: ['FRUIT', 'SOLID_MEAL'] },
  { title: 'Água e outros', types: ['WATER', 'OTHER'] },
];

export function FeedingScreen() {
  const { baby } = useBabyContext();
  const todayDate = today();
  const [day, setDay] = useState(todayDate);
  const state = useLive(async () => {
    if (!baby) return null;
    const [feedings, foods] = await Promise.all([repos.records.listFeedingsOn(baby.id!, day), repos.records.foodsTried(baby.id!)]);
    return { feedings, foods };
  }, [baby?.id, day]);

  if (!baby) return <NoBaby>Cadastre o bebê para registrar a alimentação.</NoBaby>;
  if (state.loading || !state.value) return <Loading />;
  const { feedings, foods } = state.value;
  const summary = FeedingSummaries.summarize(feedings);

  const feedingRow = (f: Feeding) => (
    <Row
      key={f.id}
      href={`#/${formPath({ type: 'FEEDING', id: f.id })}`}
      overline={Formats.time(timeOf(f.dateTime))}
      headline={feedingTypeLabel[f.type]}
      support={describeFeeding(f) || undefined}
      actionLabel="Abrir registro de alimentação"
    />
  );

  return (
    <>
      <h1 className="sr-only">Alimentação</h1>
      <div className="btn-row" aria-label="Registro rápido" role="group">
        {QUICK.map(([type, label]) => (
          <Link key={type} className="btn tonal small" to={formPath({ type: 'FEEDING', date: day, preset: type })}>
            {label}
          </Link>
        ))}
      </div>

      <div className="cal-head">
        <button type="button" className="icon-btn" onClick={() => setDay(minusDays(day, 1))} aria-label="Dia anterior">
          <Icon name="left" />
        </button>
        <h2 aria-live="polite">{day === todayDate ? 'Hoje' : Formats.longDate(day)}</h2>
        <button type="button" className="icon-btn" onClick={() => setDay(plusDays(day, 1))} aria-label="Próximo dia">
          <Icon name="right" />
        </button>
      </div>
      {day !== todayDate && (
        <div className="btn-row" style={{ justifyContent: 'center' }}>
          <button type="button" className="btn text small" onClick={() => setDay(todayDate)}>
            Ir para hoje
          </button>
        </div>
      )}

      <Card>
        <h2>Resumo do dia</h2>
        <div className="stats">
          <StatTile value={String(summary.breastfeedings)} label={`no peito (${summary.breastMinutes} min)`} />
          <StatTile value={`${summary.formulaMl} ml`} label="fórmula" />
          <StatTile value={`${summary.waterMl} ml`} label="água" />
          <StatTile value={String(summary.solidMeals)} label="refeições" />
        </div>
        {summary.newFoods.length > 0 && <p className="small-text">Alimentos novos: {summary.newFoods.join(', ')}</p>}
      </Card>

      {feedings.length === 0 && <EmptyHint>Nenhum registro neste dia. Use os botões rápidos no topo para registrar.</EmptyHint>}
      {GROUPS.map((group) => {
        const items = feedings.filter((f) => group.types.includes(f.type));
        if (items.length === 0) return null;
        return (
          <Section key={group.title} title={group.title} subtitle={`${items.length} registro(s)`} icon={<TypeBadge type="FEEDING" />} defaultOpen>
            {items.map(feedingRow)}
          </Section>
        );
      })}

      <Section title="Alimentos já experimentados" subtitle={foods.length === 0 ? 'Nenhum ainda' : `${foods.length} alimento(s)`} icon={<Icon name="food" />}>
        {foods.length === 0 && <EmptyHint>Os alimentos registrados na introdução alimentar aparecem aqui.</EmptyHint>}
        {foods.map((f) => (
          <Row key={f.food.toLowerCase()} headline={f.food} support={`desde ${Formats.date(f.firstDateTime.slice(0, 10))}`} />
        ))}
      </Section>
      <p className="small-text muted">A introdução de outros alimentos costuma começar por volta dos 6 meses. Converse com o pediatra antes de mudar a alimentação do bebê.</p>
    </>
  );
}
