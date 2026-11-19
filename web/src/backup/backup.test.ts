// @vitest-environment node
// Cópia de segurança: ida e volta, senha errada, arquivo adulterado e dados inválidos. Tudo fictício.

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CrescerBemDB } from '../data/db';
import { createRepos } from '../data/repo';
import { Prefs } from '../data/prefs';
import { fromBase64, toBase64, WrongPasswordError } from './crypto';
import { createBackup, InvalidBackupError, isEncrypted, parseEnvelope, readBackup, restoreBackup } from './backup';

const store = new Map<string, string>();
vi.stubGlobal('localStorage', {
  getItem: (k: string) => store.get(k) ?? null,
  setItem: (k: string, v: string) => void store.set(k, v),
  removeItem: (k: string) => void store.delete(k),
});

const FAST = 1000; // iterações baixas só nos testes, para ficarem rápidos
const PASSWORD = 'senha-de-teste';

let db: CrescerBemDB;
let repos: ReturnType<typeof createRepos>;

async function fill(target: ReturnType<typeof createRepos>) {
  const babyId = await target.babies.save({ name: 'Bebê Teste', birthDate: '2026-09-01', sex: 'FEMALE', colorIndex: 2 });
  await target.records.saveGrowth({ babyId, date: '2026-11-05', type: 'WEIGHT', value: 4.25, notes: 'ok' });
  await target.records.addSuggestedVaccines((await target.babies.get(babyId))!);
  await target.records.saveAppointment({ babyId, title: 'Pediatra', dateTime: '2026-11-30T09:30', status: 'SCHEDULED' });
  await target.records.saveHealthEvent({
    babyId,
    dateTime: '2026-11-12T03:00',
    symptoms: ['COLIC', 'FEVER'],
    intensity: 'MODERATE',
    temperatureCelsius: 37.9,
    soughtCare: true,
  });
  await target.records.saveFeeding({ babyId, dateTime: '2026-11-12T06:00', type: 'FRUIT', food: 'Banana', firstTime: true });
  await target.records.saveMedicationDoses([
    { babyId, name: 'Remédio', scheduledAt: '2026-11-18T08:00', status: 'SCHEDULED', seriesId: 1 },
    { babyId, name: 'Remédio', scheduledAt: '2026-11-18T16:00', status: 'GIVEN', givenAt: '2026-11-18T16:10', seriesId: 1 },
  ]);
  const momentId = await target.records.saveMoment({ babyId, dateTime: '2026-11-02T15:00', title: 'Primeiro sorriso' });
  await target.photos.sync(babyId, 'MOMENT', momentId, [], [new Blob([new Uint8Array([1, 2, 3, 250, 255])], { type: 'image/jpeg' })]);
  return babyId;
}

async function dump(d: CrescerBemDB) {
  const photos = await d.photos.toArray();
  return {
    babies: await d.babies.toArray(),
    growth: await d.growth.toArray(),
    vaccines: await d.vaccines.toArray(),
    appointments: await d.appointments.toArray(),
    healthEvents: await d.healthEvents.toArray(),
    feedings: await d.feedings.toArray(),
    medications: await d.medications.toArray(),
    moments: await d.moments.toArray(),
    photos: await Promise.all(
      photos.map(async ({ blob, ...rest }) => ({ ...rest, type: blob.type, bytes: [...new Uint8Array(await blob.arrayBuffer())] })),
    ),
  };
}

beforeEach(async () => {
  store.clear();
  db = new CrescerBemDB(`teste-${Math.random()}`);
  await db.open();
  repos = createRepos(db);
});

describe('exportar e importar', () => {
  it('ida e volta com senha preserva todos os dados, inclusive os bytes das fotos', async () => {
    await fill(repos);
    const before = await dump(db);
    const file = await createBackup(db, PASSWORD, '2026-11-19T10:00:00Z', FAST);

    const other = new CrescerBemDB(`teste-${Math.random()}`);
    await other.open();
    const summary = await restoreBackup(other, file, PASSWORD);

    expect(await dump(other)).toEqual(before);
    expect(summary.babies).toBe(1);
    expect(summary.photos).toBe(1);
    expect(summary.records).toBeGreaterThan(20);
  });

  it('ida e volta sem senha também funciona', async () => {
    await fill(repos);
    const before = await dump(db);
    const file = await createBackup(db, null);
    expect(isEncrypted(file)).toBe(false);
    const other = new CrescerBemDB(`teste-${Math.random()}`);
    await other.open();
    await restoreBackup(other, file, null);
    expect(await dump(other)).toEqual(before);
  });

  it('arquivo criptografado não deixa dados de saúde à mostra', async () => {
    await fill(repos);
    const file = await createBackup(db, PASSWORD, undefined, FAST);
    expect(isEncrypted(file)).toBe(true);
    for (const secret of ['Bebê Teste', 'Pediatra', 'Banana', 'Primeiro sorriso']) {
      expect(file).not.toContain(secret);
    }
  });

  it('senha curta é recusada', async () => {
    await expect(createBackup(db, '123', undefined, FAST)).rejects.toThrow(/pelo menos/);
  });

  it('restaurar substitui os dados atuais (não mistura)', async () => {
    const babyId = await fill(repos);
    const file = await createBackup(db, PASSWORD, undefined, FAST);
    await repos.records.saveMoment({ babyId, dateTime: '2026-11-03T10:00', title: 'Criado depois da cópia' });
    await restoreBackup(db, file, PASSWORD);
    const titles = (await db.moments.toArray()).map((m) => m.title);
    expect(titles).toEqual(['Primeiro sorriso']);
  });

  it('depois de restaurar, novos registros continuam com ids novos', async () => {
    const babyId = await fill(repos);
    const file = await createBackup(db, null);
    await restoreBackup(db, file, null);
    const maxBefore = Math.max(...(await db.moments.toArray()).map((m) => m.id!));
    const id = await repos.records.saveMoment({ babyId, dateTime: '2026-11-04T10:00', title: 'Novo' });
    expect(id).toBeGreaterThan(maxBefore);
  });

  it('bebê ativo que não existe na cópia é descartado', async () => {
    await fill(repos);
    const file = await createBackup(db, null);
    Prefs.setActiveBabyId(999);
    await restoreBackup(db, file, null);
    expect(Prefs.getActiveBabyId()).toBeNull();
  });
});

describe('arquivos com problema não alteram os dados atuais', () => {
  let before: Awaited<ReturnType<typeof dump>>;
  let good: string;

  beforeEach(async () => {
    await fill(repos);
    before = await dump(db);
    good = await createBackup(db, PASSWORD, undefined, FAST);
  });

  it('senha errada', async () => {
    await expect(restoreBackup(db, good, 'outra-senha')).rejects.toBeInstanceOf(WrongPasswordError);
    expect(await dump(db)).toEqual(before);
  });

  it('sem senha em arquivo protegido', async () => {
    await expect(restoreBackup(db, good, null)).rejects.toBeInstanceOf(InvalidBackupError);
    expect(await dump(db)).toEqual(before);
  });

  it('conteúdo cifrado adulterado é detectado', async () => {
    const env = JSON.parse(good);
    const bytes = fromBase64(env.data);
    bytes[bytes.length - 20] ^= 0xff;
    env.data = toBase64(bytes);
    await expect(restoreBackup(db, JSON.stringify(env), PASSWORD)).rejects.toBeInstanceOf(WrongPasswordError);
    expect(await dump(db)).toEqual(before);
  });

  it('texto que não é JSON', async () => {
    await expect(restoreBackup(db, 'isto não é json', null)).rejects.toBeInstanceOf(InvalidBackupError);
    expect(await dump(db)).toEqual(before);
  });

  it('JSON de outro programa', async () => {
    await expect(restoreBackup(db, JSON.stringify({ foo: 1 }), null)).rejects.toBeInstanceOf(InvalidBackupError);
    expect(await dump(db)).toEqual(before);
  });

  it('versão mais nova que a suportada', async () => {
    const env = JSON.parse(good);
    env.version = 99;
    expect(() => parseEnvelope(JSON.stringify(env))).toThrow(/mais nova/);
  });

  it('número de iterações absurdo é recusado sem travar', async () => {
    const env = JSON.parse(good);
    env.kdf.iterations = 999_999_999;
    await expect(restoreBackup(db, JSON.stringify(env), PASSWORD)).rejects.toThrow(/inválido/);
  });

  async function plainWith(mutate: (data: Record<string, Record<string, unknown>[]>) => void): Promise<string> {
    const env = JSON.parse(await createBackup(db, null));
    mutate(env.data);
    return JSON.stringify(env);
  }

  it('data inválida num registro', async () => {
    const file = await plainWith((d) => (d.growth[0].date = '2026-02-31'));
    await expect(restoreBackup(db, file, null)).rejects.toThrow(/"date" inválido/);
    expect(await dump(db)).toEqual(before);
  });

  it('valor de tipo desconhecido', async () => {
    const file = await plainWith((d) => (d.healthEvents[0].symptoms = ['INVENTADO']));
    await expect(restoreBackup(db, file, null)).rejects.toThrow(/"symptoms" inválido/);
  });

  it('registro que aponta para um bebê inexistente', async () => {
    const file = await plainWith((d) => (d.moments[0].babyId = 12345));
    await expect(restoreBackup(db, file, null)).rejects.toThrow(/sem bebê correspondente/);
    expect(await dump(db)).toEqual(before);
  });

  it('ids repetidos', async () => {
    const file = await plainWith((d) => d.babies.push({ ...d.babies[0] }));
    await expect(restoreBackup(db, file, null)).rejects.toThrow(/repetidos/);
  });

  it('tabela faltando', async () => {
    const file = await plainWith((d) => delete (d as Record<string, unknown>).photos);
    await expect(restoreBackup(db, file, null)).rejects.toThrow(/incompleta/);
  });

  it('campos desconhecidos são descartados (não entram no banco)', async () => {
    const file = await plainWith((d) => (d.moments[0].script = '<img onerror=alert(1)>'));
    const data = await readBackup(file, null);
    expect(data.moments[0]).not.toHaveProperty('script');
  });
});
