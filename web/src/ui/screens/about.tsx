import { BACKUP_STEPS, INSTALL_SECTIONS, NO_ALERTS_NOTE, NO_REMINDERS_NOTE, PRIVACY_SECTIONS, type HelpSection } from '../../content';
import { Card } from '../components';
import { Icon } from '../icons';
import { goBack } from '../router';

function Block({ section }: { section: HelpSection }) {
  return (
    <Card>
      <h2>{section.title}</h2>
      {section.paragraphs?.map((p) => (
        <p key={p}>{p}</p>
      ))}
      {section.steps && (
        <ol style={{ margin: 0, paddingLeft: 22, display: 'flex', flexDirection: 'column', gap: 6 }}>
          {section.steps.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ol>
      )}
    </Card>
  );
}

export function AboutScreen() {
  return (
    <>
      <header style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: -8 }}>
        <button type="button" className="icon-btn" onClick={goBack} aria-label="Voltar">
          <Icon name="back" />
        </button>
        <h1>Privacidade e ajuda</h1>
      </header>
      {PRIVACY_SECTIONS.map((s) => (
        <Block key={s.id} section={s} />
      ))}
      {BACKUP_STEPS.map((s) => (
        <Block key={s.id} section={s} />
      ))}
      {INSTALL_SECTIONS.map((s) => (
        <Block key={s.id} section={s} />
      ))}
      <Card>
        <h2>Lembretes</h2>
        <p>{NO_REMINDERS_NOTE}</p>
      </Card>
      <Card tone="warn">
        <p>{NO_ALERTS_NOTE}</p>
      </Card>
    </>
  );
}
