import { recordTypeLabel } from '../../domain/labels';
import type { LocalDate, RecordType } from '../../domain/types';
import { Dialog, Row, TypeBadge } from '../components';
import { RECORD_HINT } from '../icons';
import { formPath, navigate } from '../router';

interface Choice {
  label: string;
  hint: string;
  type: RecordType;
  preset?: string;
}

const CHOICES: Choice[] = [
  { type: 'MOMENT', label: 'Momento', hint: RECORD_HINT.MOMENT },
  { type: 'FEEDING', label: 'Alimentação', hint: RECORD_HINT.FEEDING },
  { type: 'HEALTH_EVENT', label: 'Sintoma', hint: RECORD_HINT.HEALTH_EVENT },
  { type: 'GROWTH', label: 'Medida', hint: RECORD_HINT.GROWTH },
  { type: 'VACCINE', label: 'Vacina', hint: RECORD_HINT.VACCINE },
  { type: 'APPOINTMENT', label: 'Consulta', hint: RECORD_HINT.APPOINTMENT },
  { type: 'MEDICATION', label: 'Agendar medicamento', hint: 'Horários das doses, com repetição' },
  { type: 'MEDICATION', label: 'Dose de medicamento dada', hint: 'Registrar o que já foi dado', preset: 'given' },
];

/** Folha "Registrar": escolhe o tipo de registro e abre o formulário (já com a data, se houver). */
export function RecordPicker({ open, date, onClose }: { open: boolean; date?: LocalDate; onClose: () => void }) {
  return (
    <Dialog open={open} onClose={onClose} title="O que você quer registrar?" sheet>
      <div style={{ display: 'flex', flexDirection: 'column', maxHeight: '60dvh', overflowY: 'auto' }}>
        {CHOICES.map((c) => (
          <Row
            key={c.label}
            onClick={() => {
              onClose();
              navigate(formPath({ type: c.type, date, preset: c.preset }));
            }}
            badge={<TypeBadge type={c.type} />}
            headline={c.label || recordTypeLabel[c.type]}
            support={c.hint}
          />
        ))}
      </div>
    </Dialog>
  );
}
