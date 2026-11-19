import { useEffect, useMemo, useRef, useState } from 'react';
import { repos } from '../data/repo';
import { Prefs } from '../data/prefs';
import { useLive } from '../data/live';
import type { LocalDate } from '../domain/types';
import { Loading, Logo, ToastHost } from './components';
import { BabyContext } from './context';
import { BabyForm } from './forms/baby';
import { FeedingForm, HealthEventForm, MedicationForm } from './forms/care';
import { AppointmentForm, GrowthForm, MomentForm, VaccineForm } from './forms/simple';
import { Icon } from './icons';
import { Link } from './link';
import { parseFormRoute, TABS, useRoute, type Route } from './router';
import { AboutScreen } from './screens/about';
import { BabyScreen } from './screens/baby';
import { CalendarScreen } from './screens/calendar';
import { FeedingScreen } from './screens/feeding';
import { HealthScreen } from './screens/health';
import { RecordPicker } from './screens/picker';
import { ReportScreen } from './screens/report';
import { SettingsScreen } from './screens/settings';
import { TodayScreen } from './screens/today';
import { useActiveBabyId, useApplyTheme, useThemeMode } from './theme';

const TITLES: Record<string, string> = {
  hoje: 'Hoje',
  calendario: 'Calendário',
  bebe: 'Bebê',
  saude: 'Saúde',
  alimentacao: 'Alimentação',
  ajustes: 'Ajustes',
  relatorio: 'Relatório em PDF',
  sobre: 'Privacidade e ajuda',
  form: 'Registro',
};

function Screen({ route }: { route: Route }) {
  const first = route.path.split('/')[0];
  switch (first) {
    case 'calendario':
      return <CalendarScreen />;
    case 'bebe':
      return <BabyScreen />;
    case 'saude':
      return <HealthScreen />;
    case 'alimentacao':
      return <FeedingScreen />;
    case 'ajustes':
      return <SettingsScreen />;
    case 'relatorio':
      return <ReportScreen />;
    case 'sobre':
      return <AboutScreen />;
    case 'form': {
      const target = parseFormRoute(route);
      switch (target.type) {
        case 'BABY':
          return <BabyForm target={target} />;
        case 'MOMENT':
          return <MomentForm target={target} />;
        case 'VACCINE':
          return <VaccineForm target={target} />;
        case 'APPOINTMENT':
          return <AppointmentForm target={target} />;
        case 'HEALTH_EVENT':
          return <HealthEventForm target={target} />;
        case 'GROWTH':
          return <GrowthForm target={target} />;
        case 'FEEDING':
          return <FeedingForm target={target} />;
        case 'MEDICATION':
          return <MedicationForm target={target} />;
      }
      return <TodayScreen />;
    }
    default:
      return <TodayScreen />;
  }
}

export function App() {
  const route = useRoute();
  const babiesState = useLive(() => repos.babies.list(), []);
  const activeId = useActiveBabyId();
  const babies = useMemo(() => babiesState.value ?? [], [babiesState.value]);
  const baby = repos.babies.resolveActive(babies, activeId);
  const effective = useApplyTheme(baby?.colorIndex ?? 0);
  const mode = useThemeMode();
  const [picker, setPicker] = useState<{ open: boolean; date?: LocalDate }>({ open: false });
  const main = useRef<HTMLElement>(null);
  const firstRender = useRef(true);

  const first = route.path.split('/')[0] || 'hoje';
  const isTab = TABS.some((t) => t.path === first) || route.path === '';

  // Título da aba e foco no conteúdo a cada troca de tela (ajuda leitores de tela e teclado).
  useEffect(() => {
    document.title = `${TITLES[first] ?? 'Crescer Bem'} · Crescer Bem`;
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    main.current?.focus({ preventScroll: true });
  }, [route.path, first]);

  const ctx = useMemo(() => ({ baby, babies, openPicker: (date?: LocalDate) => setPicker({ open: true, date }) }), [baby, babies]);

  if (babiesState.loading) return <Loading />;

  return (
    <BabyContext.Provider value={ctx}>
      <a className="skip-link" href="#conteudo" onClick={(e) => { e.preventDefault(); main.current?.focus(); }}>
        Pular para o conteúdo
      </a>
      <div className="app">
        {isTab && (
          <header className="topbar">
            <span className="brand">
              <Logo />
              <span className="sr-only">Crescer Bem</span>
            </span>
            {babies.length > 1 ? (
              <label style={{ flex: 1, minWidth: 0 }}>
                <span className="sr-only">Bebê ativo</span>
                <select
                  className="select"
                  style={{ border: 0, background: 'transparent', fontWeight: 700, fontSize: '1.0625rem' }}
                  value={baby?.id ?? ''}
                  onChange={(e) => Prefs.setActiveBabyId(Number(e.target.value))}
                >
                  {babies.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <span style={{ flex: 1, fontWeight: 700, fontSize: '1.0625rem', overflowWrap: 'anywhere' }}>{baby ? baby.name : 'Crescer Bem'}</span>
            )}
            <button
              type="button"
              className="icon-btn"
              aria-label={effective === 'dark' ? 'Usar tema claro' : 'Usar tema escuro'}
              onClick={() => Prefs.setTheme(effective === 'dark' ? 'LIGHT' : 'DARK')}
              data-mode={mode}
            >
              <Icon name={effective === 'dark' ? 'sun' : 'moon'} />
            </button>
            <Link className="icon-btn" to="relatorio" ariaLabel="Relatório em PDF">
              <Icon name="report" />
            </Link>
            <Link className="icon-btn" to="ajustes" ariaLabel="Ajustes">
              <Icon name="settings" />
            </Link>
          </header>
        )}

        <main id="conteudo" ref={main} tabIndex={-1} className={`content ${isTab ? '' : 'no-nav'}`} style={{ outline: 'none' }}>
          <Screen key={`${route.path}?${route.params.toString()}`} route={route} />
        </main>

        {isTab && (
          <>
            {baby && (
              <button type="button" className="fab" onClick={() => setPicker({ open: true })}>
                <Icon name="add" />
                Registrar
              </button>
            )}
            <nav className="bottom-nav" aria-label="Principal">
              <div className="inner">
                {TABS.map((tab) => (
                  <a key={tab.path} href={`#/${tab.path}`} aria-current={first === tab.path ? 'page' : undefined}>
                    <span className="pill">
                      <Icon name={tab.icon} />
                    </span>
                    {tab.label}
                  </a>
                ))}
              </div>
            </nav>
          </>
        )}
      </div>
      <RecordPicker open={picker.open} date={picker.date} onClose={() => setPicker({ open: false })} />
      <ToastHost />
    </BabyContext.Provider>
  );
}
