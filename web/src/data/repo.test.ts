// @vitest-environment node
// Testes da camada de dados com um IndexedDB de mentira (fake-indexeddb). Dados 100% fictícios.

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CrescerBemDB } from './db';
import { createRepos } from './repo';
import { Prefs } from './prefs';
import type { Baby, MedicationDose } from '../domain/types';

// localStorage simples em memória (no Node não existe).
const store = new Map<string, string>();
vi.stubGlobal('localStorage', {
  getItem: (k: string) => store.get(k) ?? null,
  setItem: (k: string, v: string) => void store.set(k, v),
  removeItem: (k: string) => void store.delete(k),
});

let db: CrescerBemDB;
let repos: ReturnType<typeof createRepos>;

beforeEach(async () => {
  store.clear();
  db = new CrescerBemDB(`teste-${Math.random()}`);
  await db.open();
  repos = createRepos(db);
});

const baby = (over: Partial<Baby> = {}): Baby => ({ name: 'Bebê Teste', birthDate: '2026-09-01', colorIndex: 0, ...over });

describe('bebês', () => {
  it('salvar um bebê novo o deixa ativo e atribui id', async () => {
    const id = await repos.babies.save(baby());
    expect(id).toBeGreaterThan(0);
    expect(Prefs.getActiveBabyId()).toBe(id);
  });

  it('lista do mais novo para o mais velho', async () => {
    await repos.babies.save(baby({ name: 'Velho', birthDate: '2025-01-01' }));
    await repos.babies.save(baby({ name: 'Novo', birthDate: '2026-10-01' }));
    expect((await repos.babies.list()).map((b) => b.name)).toEqual(['Novo', 'Velho']);
  });

  it('bebê ativo cai para o primeiro quando o escolhido não existe', async () => {
    await repos.babies.save(baby({ name: 'A', birthDate: '2026-01-01' }));
    const list = await repos.babies.list();
    expect(repos.babies.resolveActive(list, 999)!.name).toBe('A');
    expect(repos.babies.resolveActive([], null)).toBeNull();
  });

  it('próxima cor usa a primeira ainda livre', async () => {
    await repos.babies.save(baby({ colorIndex: 0 }));
    await repos.babies.save(baby({ colorIndex: 1 }));
    expect(await repos.babies.nextColorIndex(8)).toBe(2);
  });

  it('excluir o bebê apaga todos os registros e fotos dele, e só os dele', async () => {
    const a = await repos.babies.save(baby({ name: 'A' }));
    const b = await repos.babies.save(baby({ name: 'B' }));
    for (const babyId of [a, b]) {
      const id = await repos.records.saveMoment({ babyId, dateTime: '2026-11-01T10:00', title: 'x' });
      await repos.photos.sync(babyId, 'MOMENT', id, [], [new Blob(['foto'])]);
      await repos.records.saveGrowth({ babyId, date: '2026-11-01', type: 'WEIGHT', value: 4 });
      await repos.records.saveFeeding({ babyId, dateTime: '2026-11-01T10:00', type: 'FORMULA', firstTime: false });
    }
    await repos.babies.delete((await repos.babies.get(a))!);

    expect(await db.moments.where('babyId').equals(a).count()).toBe(0);
    expect(await db.photos.where('babyId').equals(a).count()).toBe(0);
    expect(await db.growth.where('babyId').equals(a).count()).toBe(0);
    expect(await db.moments.where('babyId').equals(b).count()).toBe(1);
    expect(await db.photos.where('babyId').equals(b).count()).toBe(1);
    expect(await repos.babies.get(a)).toBeUndefined();
  });
});

describe('registros', () => {
  let babyId: number;
  beforeEach(async () => {
    babyId = await repos.babies.save(baby());
  });

  it('salvar sem id cria; salvar com id atualiza', async () => {
    const id = await repos.records.saveGrowth({ babyId, date: '2026-11-01', type: 'WEIGHT', value: 4.0 });
    await repos.records.saveGrowth({ ...(await repos.records.getGrowth(id))!, value: 4.2 });
    expect(await db.growth.count()).toBe(1);
    expect((await repos.records.getGrowth(id))!.value).toBe(4.2);
  });

  it('medidas listadas da mais recente para a mais antiga', async () => {
    await repos.records.saveGrowth({ babyId, date: '2026-11-01', type: 'WEIGHT', value: 4.0 });
    await repos.records.saveGrowth({ babyId, date: '2026-11-20', type: 'WEIGHT', value: 4.6 });
    expect((await repos.records.listGrowth(babyId)).map((g) => g.value)).toEqual([4.6, 4.0]);
  });

  it('vacinas sugeridas não duplicam', async () => {
    const saved = (await repos.babies.get(babyId))!;
    const first = await repos.records.addSuggestedVaccines(saved);
    expect(first).toBeGreaterThan(0);
    expect(await repos.records.addSuggestedVaccines(saved)).toBe(0);
    const list = await repos.records.listVaccines(babyId);
    expect(list).toHaveLength(first);
    expect(list[0].recommendedAgeMonths).toBe(0);
  });

  it('vacinas avulsas (sem idade) ficam no fim da lista', async () => {
    await repos.records.saveVaccine({ babyId, name: 'Avulsa', status: 'SCHEDULED', scheduledDate: '2026-01-01' });
    await repos.records.saveVaccine({ babyId, name: 'Com idade', status: 'SCHEDULED', recommendedAgeMonths: 12 });
    expect((await repos.records.listVaccines(babyId)).map((v) => v.name)).toEqual(['Com idade', 'Avulsa']);
  });

  it('próximas consultas: só agendadas, a partir de agora, mais próxima primeiro', async () => {
    const base = { babyId, status: 'SCHEDULED' as const };
    await repos.records.saveAppointment({ ...base, title: 'Longe', dateTime: '2026-12-20T09:00' });
    await repos.records.saveAppointment({ ...base, title: 'Perto', dateTime: '2026-11-20T09:00' });
    await repos.records.saveAppointment({ ...base, title: 'Passada', dateTime: '2026-11-01T09:00' });
    await repos.records.saveAppointment({ ...base, title: 'Cancelada', dateTime: '2026-11-21T09:00', status: 'CANCELED' });
    const next = await repos.records.upcomingAppointments(babyId, '2026-11-19T00:00');
    expect(next.map((a) => a.title)).toEqual(['Perto', 'Longe']);
  });

  it('alimentos já oferecidos: um por nome, com a data da primeira vez', async () => {
    const f = (dateTime: string, food: string) => ({ babyId, dateTime, type: 'FRUIT' as const, food, firstTime: false });
    await repos.records.saveFeeding(f('2026-11-05T10:00', ' Banana '));
    await repos.records.saveFeeding(f('2026-11-03T10:00', 'banana'));
    await repos.records.saveFeeding(f('2026-11-10T10:00', 'Maçã'));
    await repos.records.saveFeeding({ babyId, dateTime: '2026-11-11T10:00', type: 'WATER', firstTime: false });
    const tried = await repos.records.foodsTried(babyId);
    expect(tried).toEqual([
      { food: 'Maçã', firstDateTime: '2026-11-10T10:00' },
      { food: 'banana', firstDateTime: '2026-11-03T10:00' },
    ]);
  });

  it('alimentação de um dia não inclui o dia seguinte à meia-noite', async () => {
    const f = (dateTime: string) => ({ babyId, dateTime, type: 'FORMULA' as const, firstTime: false });
    await repos.records.saveFeeding(f('2026-11-17T23:59'));
    await repos.records.saveFeeding(f('2026-11-18T00:00'));
    expect(await repos.records.listFeedingsOn(babyId, '2026-11-17')).toHaveLength(1);
  });

  it('excluir um registro exclui as fotos dele', async () => {
    const id = await repos.records.saveMoment({ babyId, dateTime: '2026-11-01T10:00', title: 'x' });
    await repos.photos.sync(babyId, 'MOMENT', id, [], [new Blob(['a']), new Blob(['b'])]);
    expect(await repos.photos.forOwner('MOMENT', id)).toHaveLength(2);
    await repos.records.delete('MOMENT', id);
    expect(await repos.records.getMoment(id)).toBeUndefined();
    expect(await db.photos.count()).toBe(0);
  });

  it('sincronizar fotos mantém as escolhidas, apaga as removidas e adiciona as novas', async () => {
    const id = await repos.records.saveMoment({ babyId, dateTime: '2026-11-01T10:00', title: 'x' });
    await repos.photos.sync(babyId, 'MOMENT', id, [], [new Blob(['a']), new Blob(['b'])]);
    const [first] = await repos.photos.forOwner('MOMENT', id);
    await repos.photos.sync(babyId, 'MOMENT', id, [first.id!], [new Blob(['c'])]);
    const after = await repos.photos.forOwner('MOMENT', id);
    expect(after).toHaveLength(2);
    expect(after[0].id).toBe(first.id);
  });
});

describe('medicamentos em série', () => {
  let babyId: number;
  const dose = (h: number, over: Partial<MedicationDose> = {}): MedicationDose => ({
    babyId,
    name: 'Remédio',
    dosage: '5 ml',
    scheduledAt: `2026-11-18T${String(h).padStart(2, '0')}:00`,
    status: 'SCHEDULED',
    seriesId: 1,
    ...over,
  });

  beforeEach(async () => {
    babyId = await repos.babies.save(baby());
  });

  it('próximo número de série começa em 1 e avança', async () => {
    expect(await repos.records.nextSeriesId()).toBe(1);
    await repos.records.saveMedicationDoses([dose(8, { seriesId: 1 })]);
    expect(await repos.records.nextSeriesId()).toBe(2);
  });

  it('doses seguintes: mesma série, ainda agendadas, depois da dose', async () => {
    const ids = await repos.records.saveMedicationDoses([dose(8), dose(12), dose(16, { status: 'GIVEN' }), dose(20), dose(10, { seriesId: 2 })]);
    const first = (await repos.records.getMedication(ids[0]))!;
    const following = await repos.records.followingInSeries(first);
    expect(following.map((m) => m.scheduledAt)).toEqual(['2026-11-18T12:00', '2026-11-18T20:00']);
  });

  it('editar a dose repete nome, dose, observações e deslocamento nas seguintes', async () => {
    const ids = await repos.records.saveMedicationDoses([dose(8), dose(12), dose(16)]);
    const original = (await repos.records.getMedication(ids[0]))!;
    const updated = { ...original, name: 'Novo nome', dosage: '10 gotas', scheduledAt: '2026-11-18T08:30' };
    const n = await repos.records.applyEditToFollowing(original, updated);
    expect(n).toBe(2);
    const second = (await repos.records.getMedication(ids[1]))!;
    expect(second.name).toBe('Novo nome');
    expect(second.dosage).toBe('10 gotas');
    expect(second.scheduledAt).toBe('2026-11-18T12:30');
  });

  it('excluir as seguintes não mexe na própria dose', async () => {
    const ids = await repos.records.saveMedicationDoses([dose(8), dose(12), dose(16)]);
    const first = (await repos.records.getMedication(ids[0]))!;
    expect(await repos.records.deleteFollowingInSeries(first)).toBe(2);
    expect(await db.medications.count()).toBe(1);
  });
});

describe('linha do tempo e relatório', () => {
  it('junta todos os tipos de registro em ordem cronológica', async () => {
    const babyId = await repos.babies.save(baby());
    await repos.records.saveMoment({ babyId, dateTime: '2026-11-17T15:00', title: 'Sorriso' });
    await repos.records.saveFeeding({ babyId, dateTime: '2026-11-17T09:00', type: 'FORMULA', amountMl: 90, firstTime: false });
    await repos.records.saveGrowth({ babyId, date: '2026-11-17', type: 'WEIGHT', value: 4.2 });
    await repos.records.saveMoment({ babyId, dateTime: '2026-11-18T00:00', title: 'Fora do dia' });
    const items = await repos.timeline.range(babyId, '2026-11-17', '2026-11-17', '2026-11-17');
    expect(items.map((i) => i.type)).toEqual(['GROWTH', 'FEEDING', 'MOMENT']);
  });

  it('dados do relatório só trazem o bebê e o período pedidos; fotos só se pedidas', async () => {
    const a = await repos.babies.save(baby({ name: 'A' }));
    const b = await repos.babies.save(baby({ name: 'B' }));
    const momentA = await repos.records.saveMoment({ babyId: a, dateTime: '2026-11-05T10:00', title: 'A' });
    await repos.records.saveMoment({ babyId: b, dateTime: '2026-11-05T10:00', title: 'B' });
    await repos.records.saveMoment({ babyId: a, dateTime: '2026-12-05T10:00', title: 'Fora' });
    await repos.photos.sync(a, 'MOMENT', momentA, [], [new Blob(['foto'])]);

    const withoutPhotos = await repos.timeline.loadReportData(a, '2026-11-01', '2026-11-30', false);
    expect(withoutPhotos.moments.map((m) => m.title)).toEqual(['A']);
    expect(withoutPhotos.photos).toEqual({});

    const withPhotos = await repos.timeline.loadReportData(a, '2026-11-01', '2026-11-30', true);
    expect(Object.keys(withPhotos.photos)).toEqual([`MOMENT:${momentA}`]);
  });
});

describe('apagar tudo', () => {
  it('limpa o banco e as preferências', async () => {
    const babyId = await repos.babies.save(baby());
    await repos.records.saveGrowth({ babyId, date: '2026-11-01', type: 'WEIGHT', value: 4 });
    Prefs.setTheme('DARK');
    await repos.eraseEverything();
    for (const table of db.allTables) expect(await table.count()).toBe(0);
    expect(Prefs.getActiveBabyId()).toBeNull();
    expect(Prefs.getTheme()).toBe('SYSTEM');
    expect(store.size).toBe(0);
  });
});
