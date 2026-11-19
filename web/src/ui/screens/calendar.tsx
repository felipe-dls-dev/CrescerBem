import { useState } from 'react';
import { repos } from '../../data/repo';
import { useLive } from '../../data/live';
import { CalendarGrid } from '../../domain/calendarGrid';
import { atDay, lengthOfMonth, plusMonthsYm, today, yearMonthOf, parseDate, type YearMonth } from '../../domain/dates';
import { Formats, MONTH_NAMES } from '../../domain/formats';
import { recordTypeLabel } from '../../domain/labels';
import { RECORD_TYPES, type LocalDate, type RecordType } from '../../domain/types';
import { EmptyHint, Loading, TimelineRow } from '../components';
import { useBabyContext } from '../context';
import { Icon, RECORD_ICON } from '../icons';
import { CATEGORY_COLORS } from '../theme';
import { NoBaby } from './shared';

const LEGEND_TYPES = RECORD_TYPES.filter((t) => t !== 'BABY');

export function CalendarScreen() {
  const { baby, openPicker } = useBabyContext();
  const todayDate = today();
  const [month, setMonth] = useState<YearMonth>(() => yearMonthOf(todayDate));
  const [selected, setSelected] = useState<LocalDate>(todayDate);

  const state = useLive(
    async () => (baby ? repos.timeline.range(baby.id!, atDay(month, 1), atDay(month, lengthOfMonth(month.year, month.month)), todayDate) : null),
    [baby?.id, month.year, month.month, todayDate],
  );

  if (!baby) return <NoBaby>Cadastre o bebê para ver o calendário de registros.</NoBaby>;
  if (state.loading || !state.value) return <Loading />;

  const items = state.value;
  const markers = CalendarGrid.markersByDay(items);
  const cells = CalendarGrid.cells(month);
  const dayItems = items.filter((i) => i.date === selected);
  const goMonth = (delta: number) => {
    const next = plusMonthsYm(month, delta);
    setMonth(next);
    setSelected(atDay(next, 1));
  };

  return (
    <>
      <h1 className="sr-only">Calendário</h1>
      <div className="cal-head">
        <button type="button" className="icon-btn" onClick={() => goMonth(-1)} aria-label="Mês anterior">
          <Icon name="left" />
        </button>
        <h2 aria-live="polite">{Formats.monthYear(month)}</h2>
        <button type="button" className="icon-btn" onClick={() => goMonth(1)} aria-label="Próximo mês">
          <Icon name="right" />
        </button>
      </div>
      <div className="btn-row" style={{ justifyContent: 'center' }}>
        <button
          type="button"
          className="btn text small"
          onClick={() => {
            setMonth(yearMonthOf(todayDate));
            setSelected(todayDate);
          }}
        >
          Ir para hoje
        </button>
      </div>

      <div className="cal-grid" role="group" aria-label={`Dias de ${Formats.monthYear(month)}`}>
        {CalendarGrid.weekdayInitials().map((initial, i) => (
          <span key={i} className="dow" aria-hidden="true">
            {initial}
          </span>
        ))}
        {cells.map((cell, i) => {
          if (!cell) return <span key={`e${i}`} />;
          const types = markers.get(cell) ?? [];
          const { day } = parseDate(cell);
          const count = items.filter((it) => it.date === cell).length;
          const label = `${day} de ${MONTH_NAMES[month.month - 1]}${cell === todayDate ? ', hoje' : ''}. ${
            count === 0 ? 'Sem registros' : `${count} registro(s): ${types.map((t) => recordTypeLabel[t]).join(', ')}`
          }`;
          return (
            <button key={cell} type="button" className={`cal-cell ${cell === todayDate ? 'today' : ''}`} aria-pressed={cell === selected} aria-label={label} onClick={() => setSelected(cell)}>
              <span aria-hidden="true">{day}</span>
              <span className="dots" aria-hidden="true">
                {types.slice(0, 4).map((t) => (
                  <span key={t} className="dot" style={{ background: CATEGORY_COLORS[t] }} />
                ))}
              </span>
            </button>
          );
        })}
      </div>

      <div className="legend" aria-label="Legenda dos marcadores">
        {LEGEND_TYPES.map((t: RecordType) => (
          <span key={t}>
            <span className="dot" style={{ width: 9, height: 9, borderRadius: '50%', background: CATEGORY_COLORS[t], display: 'inline-block' }} />
            <Icon name={RECORD_ICON[t]} size={14} />
            {recordTypeLabel[t]}
          </span>
        ))}
      </div>

      <section aria-labelledby="day-title" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <h2 id="day-title">{Formats.longDate(selected)}</h2>
        {dayItems.length === 0 ? <EmptyHint>Nenhum registro neste dia.</EmptyHint> : dayItems.map((item) => <TimelineRow key={`${item.type}-${item.id}`} item={item} />)}
        <div>
          <button type="button" className="btn tonal" onClick={() => openPicker(selected)}>
            <Icon name="add" size={20} />
            Registrar neste dia
          </button>
        </div>
      </section>
    </>
  );
}
