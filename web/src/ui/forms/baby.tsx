import { useEffect, useState } from 'react';
import { repos } from '../../data/repo';
import { Formats } from '../../domain/formats';
import { Plausibility } from '../../domain/plausibility';
import { sexLabel } from '../../domain/labels';
import { SEXES, type Baby, type Sex } from '../../domain/types';
import { today } from '../../domain/dates';
import { Card, ConfirmDialog, Loading, Section, showToast } from '../components';
import { useBabyContext } from '../context';
import { CheckField, ChoiceChips, DateField, SelectField, TextField, TimeField } from '../fields';
import { Icon } from '../icons';
import { PhotoAttachments, usePhotoDraft } from '../photos';
import { goBack, navigate, type FormTarget } from '../router';
import { BABY_PALETTE, paletteEntry, setColorPreview } from '../theme';
import { FormPage, nz, useLoaded } from './common';

const BLOOD_TYPES = ['', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function BabyForm({ target }: { target: FormTarget }) {
  const { loading, value } = useLoaded(target.id, repos.babies.get);
  if (loading) return <Loading />;
  return <BabyBody initial={value} />;
}

function BabyBody({ initial }: { initial?: Baby }) {
  const { babies } = useBabyContext();
  const [name, setName] = useState(initial?.name ?? '');
  const [birthDate, setBirthDate] = useState(initial?.birthDate ?? '');
  const [birthTime, setBirthTime] = useState(initial?.birthTime ?? '');
  const [sex, setSex] = useState<Sex | null>(initial?.sex ?? null);
  const [bloodType, setBloodType] = useState(initial?.bloodType ?? '');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [colorIndex, setColorIndex] = useState(initial?.colorIndex ?? 0);
  const [weight, setWeight] = useState('');
  const [length, setLength] = useState('');
  const [head, setHead] = useState('');
  const [addVaccines, setAddVaccines] = useState(true);
  const [warnings, setWarnings] = useState<string[]>([]);
  const photos = usePhotoDraft('BABY', initial?.id);

  // Bebê novo: sugere a primeira cor da paleta que ainda não foi usada.
  useEffect(() => {
    if (!initial) void repos.babies.nextColorIndex(BABY_PALETTE.length).then(setColorIndex);
  }, [initial]);

  // A cor escolhida já vale na hora, para o usuário testar sem salvar; ao sair, volta à do bebê ativo.
  useEffect(() => {
    setColorPreview(colorIndex);
  }, [colorIndex]);
  useEffect(() => () => setColorPreview(null), []);

  const w = Formats.parseDecimal(weight);
  const l = Formats.parseDecimal(length);
  const h = Formats.parseDecimal(head);
  const badNumber = [weight, length, head].some((t, i) => t.trim() !== '' && [w, l, h][i] === null);

  const errors = [
    !name.trim() && 'Informe o nome ou um apelido.',
    !birthDate && 'Informe a data de nascimento.',
    birthDate > today() && 'A data de nascimento não pode estar no futuro.',
    badNumber && 'Confira os números do nascimento (use vírgula, ex.: 3,250).',
  ].filter(Boolean) as string[];

  const persist = async () => {
    const saved: Baby = {
      ...(initial ?? {}),
      name: name.trim(),
      birthDate,
      birthTime: birthTime || null,
      sex,
      bloodType: nz(bloodType),
      notes: nz(notes),
      colorIndex,
    };
    const id = await repos.babies.save(saved);
    await photos.commit(id, id);
    if (!initial) {
      // Medidas ao nascer viram registros de crescimento na data de nascimento.
      const births = [
        ['WEIGHT', w],
        ['HEIGHT', l],
        ['HEAD_CIRCUMFERENCE', h],
      ] as const;
      for (const [type, value] of births) {
        if (value !== null && value > 0) await repos.records.saveGrowth({ babyId: id, date: birthDate, type, value });
      }
      if (addVaccines) await repos.records.addSuggestedVaccines({ ...saved, id });
    }
    showToast(initial ? 'Dados salvos.' : `${saved.name} foi cadastrado(a).`);
    if (initial) goBack();
    else navigate('hoje');
  };

  return (
    <>
      <FormPage
        title={initial ? 'Dados do bebê' : 'Cadastrar bebê'}
        errors={errors}
        onSave={() => {
          const found = [
            w !== null ? Plausibility.measurementWarning('WEIGHT', w) : null,
            l !== null ? Plausibility.measurementWarning('HEIGHT', l) : null,
            h !== null ? Plausibility.measurementWarning('HEAD_CIRCUMFERENCE', h) : null,
          ].filter(Boolean) as string[];
          if (!initial && found.length > 0) {
            setWarnings(found);
            return;
          }
          return persist();
        }}
      >
        <TextField label="Nome ou apelido" value={name} onChange={setName} required autoFocus={!initial} />
        <DateField label="Data de nascimento" value={birthDate} onChange={setBirthDate} max={today()} required />
        <TimeField label="Hora do nascimento (opcional)" value={birthTime} onChange={setBirthTime} />
        <ChoiceChips legend="Sexo (opcional)" value={sex} onChange={setSex} options={SEXES.map((s) => ({ value: s, label: sexLabel[s] }))} allowNone hint="Toque de novo para desmarcar (prefiro não informar)." />
        <SelectField label="Tipo sanguíneo (opcional)" value={bloodType} onChange={setBloodType} options={BLOOD_TYPES.map((b) => ({ value: b, label: b || 'Não sei / não informar' }))} />

        <fieldset className="field">
          <legend>Cor do tema</legend>
          <div className="color-swatches" role="radiogroup" aria-label="Cor do tema">
            {BABY_PALETTE.map((p, i) => (
              <button
                key={p.label}
                type="button"
                role="radio"
                aria-checked={colorIndex === i}
                aria-label={p.label}
                className="swatch"
                style={{ background: p.primary }}
                onClick={() => setColorIndex(i)}
              >
                {colorIndex === i && <Icon name="check" size={24} />}
              </button>
            ))}
          </div>
          <span className="hint">Cor escolhida: {paletteEntry(colorIndex).label}. O app já mostra a cor; ela só fica gravada ao salvar.</span>
        </fieldset>

        {!initial && (
          <>
            <Section title="Mais dados do nascimento" subtitle="Peso, comprimento e perímetro cefálico">
              <div className="pad" style={{ display: 'flex', flexDirection: 'column', gap: 16, padding: '0 8px' }}>
                <TextField label="Peso ao nascer (kg)" value={weight} onChange={setWeight} inputMode="decimal" hint="Ex.: 3,250" />
                <TextField label="Comprimento ao nascer (cm)" value={length} onChange={setLength} inputMode="decimal" hint="Ex.: 49,0" />
                <TextField label="Perímetro cefálico ao nascer (cm)" value={head} onChange={setHead} inputMode="decimal" hint="Ex.: 34,0" />
              </div>
            </Section>
            <CheckField label="Adicionar calendário de vacinas sugerido" hint="Cria as vacinas previstas com base na data de nascimento. Você pode editar depois." checked={addVaccines} onChange={setAddVaccines} />
          </>
        )}

        <TextField label="Observações (opcional)" value={notes} onChange={setNotes} multiline />
        <PhotoAttachments draft={photos} label="Foto do bebê" hint="A última foto adicionada aparece no perfil. Fica só neste aparelho." />
        {initial && babies.length > 0 && (
          <Card>
            <p className="small-text">Para excluir este bebê e todos os registros, vá em Ajustes.</p>
          </Card>
        )}
      </FormPage>
      <ConfirmDialog
        open={warnings.length > 0}
        title="Confira os números"
        message={
          <>
            {warnings.map((m) => (
              <p key={m}>{m}</p>
            ))}
          </>
        }
        confirmLabel="Salvar mesmo assim"
        cancelLabel="Corrigir"
        onCancel={() => setWarnings([])}
        onConfirm={() => {
          setWarnings([]);
          void persist();
        }}
      />
    </>
  );
}
