import { useEffect, useState } from 'react';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { BabyAge } from '../../domain/babyAge';
import { today } from '../../domain/dates';
import { Formats } from '../../domain/formats';
import { measurementDecimals, measurementLabel, measurementUnit, sexLabel } from '../../domain/labels';
import { MEASUREMENT_TYPES, type GrowthMeasurement, type MeasurementType } from '../../domain/types';
import { Card, EmptyHint, Loading, LineChart, Row, Section, TypeBadge } from '../components';
import { useBabyContext } from '../context';
import { Icon } from '../icons';
import { Link } from '../link';
import { formPath } from '../router';
import { NoBaby } from './shared';

/** Foto de perfil: a última foto adicionada ao bebê. */
function useProfilePhoto(babyId: number | undefined): string | null {
  const photo = useLive(async () => {
    if (babyId === undefined) return null;
    const list = await repos.photos.forOwner('BABY', babyId);
    return list[list.length - 1] ?? null;
  }, [babyId]).value;
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!photo) {
      setUrl(null);
      return;
    }
    const u = URL.createObjectURL(photo.blob);
    setUrl(u);
    return () => URL.revokeObjectURL(u);
  }, [photo]);
  return url;
}

export function BabyScreen() {
  const { baby } = useBabyContext();
  const day = today();
  const photoUrl = useProfilePhoto(baby?.id);
  const state = useLive(async () => {
    if (!baby) return null;
    const [growth, moments] = await Promise.all([repos.records.listGrowth(baby.id!), repos.records.listMoments(baby.id!)]);
    return { growth, moments };
  }, [baby?.id]);

  if (!baby) return <NoBaby>Cadastre o bebê para acompanhar o crescimento.</NoBaby>;
  if (state.loading || !state.value) return <Loading />;
  const { growth, moments } = state.value;

  return (
    <>
      <h1 className="sr-only">Bebê</h1>
      <Card tone="primary">
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          {photoUrl && <img className="avatar" src={photoUrl} alt={`Foto de ${baby.name}`} />}
          <div style={{ minWidth: 0 }}>
            <h2 style={{ fontSize: '1.375rem', overflowWrap: 'anywhere' }}>{baby.name}</h2>
            <p>{BabyAge.sentence(baby.name, baby.birthDate, day)}</p>
          </div>
        </div>
        <p className="small-text">
          Nasceu em {Formats.date(baby.birthDate)}
          {baby.birthTime ? ` às ${Formats.time(baby.birthTime)}` : ''}
          {[baby.sex ? sexLabel[baby.sex] : null, baby.bloodType ? `Tipo sanguíneo ${baby.bloodType}` : null].filter(Boolean).map((t) => ` · ${t}`)}
        </p>
        {baby.notes && <p className="small-text">{baby.notes}</p>}
        <div>
          <Link className="btn tonal small" to={formPath({ type: 'BABY', id: baby.id })}>
            <Icon name="edit" size={18} />
            Editar dados
          </Link>
        </div>
      </Card>

      <h2>Crescimento</h2>
      {MEASUREMENT_TYPES.map((type) => (
        <GrowthSection key={type} type={type} items={growth.filter((g) => g.type === type)} />
      ))}
      <p className="small-text muted">O gráfico mostra só a evolução dos valores que você registrou. Converse com o pediatra sobre o crescimento do bebê.</p>

      <Section
        title="Momentos"
        subtitle={moments.length === 0 ? 'Nenhum ainda' : `${moments.length} momento(s)`}
        icon={<TypeBadge type="MOMENT" />}
      >
        {moments.length === 0 && <EmptyHint>Guarde aqui o primeiro sorriso, o primeiro banho e outros marcos.</EmptyHint>}
        {moments.map((m) => (
          <Row
            key={m.id}
            href={`#/${formPath({ type: 'MOMENT', id: m.id })}`}
            overline={Formats.dateTime(m.dateTime)}
            headline={m.title}
            support={m.description}
            actionLabel="Abrir momento"
          />
        ))}
        <div style={{ padding: '4px 8px' }}>
          <Link className="btn tonal small" to={formPath({ type: 'MOMENT' })}>
            <Icon name="add" size={18} />
            Novo momento
          </Link>
        </div>
      </Section>
    </>
  );
}

function GrowthSection({ type, items }: { type: MeasurementType; items: GrowthMeasurement[] }) {
  const label = measurementLabel[type].toLowerCase();
  const chronological = [...items].sort((a, b) => a.date.localeCompare(b.date) || a.id! - b.id!);
  const last = items[0];
  const values = items.map((i) => i.value);
  const delta = (index: number): string | null => {
    const older = items[index + 1];
    if (!older) return null;
    const d = items[index].value - older.value;
    const sign = d > 0 ? '+' : d < 0 ? '−' : '';
    return `${sign}${Formats.decimal(Math.abs(d), measurementDecimals[type])} ${measurementUnit[type]}`;
  };
  return (
    <Section
      title={measurementLabel[type]}
      subtitle={last ? `${Formats.measurement(type, last.value)} em ${Formats.date(last.date)}` : 'Sem registros'}
      icon={<TypeBadge type="GROWTH" />}
    >
      {chronological.length > 0 ? (
        <div style={{ padding: '0 8px' }}>
          <LineChart
            points={chronological.map((g) => ({ date: g.date, value: g.value }))}
            type={type}
            description={`Gráfico de ${label}: de ${Formats.measurement(type, chronological[0].value)} em ${Formats.date(chronological[0].date)} para ${Formats.measurement(type, chronological[chronological.length - 1].value)} em ${Formats.date(chronological[chronological.length - 1].date)}.`}
          />
          <p className="small-text muted">
            Menor: {Formats.measurement(type, Math.min(...values))} · Maior: {Formats.measurement(type, Math.max(...values))}
          </p>
        </div>
      ) : (
        <EmptyHint>Nenhuma medida de {label} ainda.</EmptyHint>
      )}
      {items.slice(0, 5).map((g, i) => (
        <Row
          key={g.id}
          href={`#/${formPath({ type: 'GROWTH', id: g.id })}`}
          headline={Formats.measurement(type, g.value)}
          support={[Formats.date(g.date), delta(i), g.place].filter(Boolean).join(' · ')}
          actionLabel="Abrir medida"
        />
      ))}
      <div style={{ padding: '4px 8px' }}>
        <Link className="btn tonal small" to={formPath({ type: 'GROWTH', preset: type })}>
          <Icon name="add" size={18} />
          Nova medida de {label}
        </Link>
      </div>
    </Section>
  );
}
