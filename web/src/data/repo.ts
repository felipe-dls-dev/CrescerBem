// Repositórios: criar, ler, editar e excluir registros. Mesmas regras do app Android
// (data/repo/*.kt): excluir um registro exclui as fotos dele; excluir o bebê exclui tudo dele.

import { minutesBetween, now, plusDays, plusMinutes, startOfDay } from '../domain/dates';
import { emptyReportData, recordKey, type ReportData } from '../domain/report';
import {
  appointmentToTimelineItem,
  feedingToTimelineItem,
  growthToTimelineItem,
  healthToTimelineItem,
  medicationReferenceTime,
  medicationToTimelineItem,
  momentToTimelineItem,
  sortedChronologically,
  vaccineToTimelineItem,
  type TimelineItem,
} from '../domain/timeline';
import { VaccineSchedule, vaccineReferenceDate } from '../domain/vaccineRules';
import type {
  Appointment,
  Baby,
  Feeding,
  GrowthMeasurement,
  HealthEvent,
  LocalDate,
  LocalDateTime,
  MedicationDose,
  Moment,
  Photo,
  RecordType,
  Vaccine,
} from '../domain/types';
import { db as defaultDb, type CrescerBemDB } from './db';
import { Prefs } from './prefs';

const cmp = (a: string | number, b: string | number): number => (a < b ? -1 : a > b ? 1 : 0);
const byDateDescIdDesc = <T extends { id?: number }>(key: (item: T) => string) => (a: T, b: T) =>
  cmp(key(b), key(a)) || (b.id ?? 0) - (a.id ?? 0);
const byDateAscIdAsc = <T extends { id?: number }>(key: (item: T) => string) => (a: T, b: T) =>
  cmp(key(a), key(b)) || (a.id ?? 0) - (b.id ?? 0);

export interface FoodTried {
  food: string;
  firstDateTime: LocalDateTime;
}

export function createRepos(db: CrescerBemDB = defaultDb) {
  // --- Bebês ---

  const babies = {
    /** Mais novo primeiro (nascimento decrescente), depois por id. */
    async list(): Promise<Baby[]> {
      const all = await db.babies.toArray();
      return all.sort((a, b) => cmp(b.birthDate, a.birthDate) || (a.id ?? 0) - (b.id ?? 0));
    },

    get: (id: number): Promise<Baby | undefined> => db.babies.get(id),

    /** Bebê selecionado; se nenhum foi escolhido (ou foi excluído), usa o primeiro da lista. */
    resolveActive(list: Baby[], activeId: number | null): Baby | null {
      return list.find((b) => b.id === activeId) ?? list[0] ?? null;
    },

    /** Primeira cor da paleta ainda não usada por outro bebê; se todas estiverem em uso, recomeça pela ordem. */
    async nextColorIndex(paletteSize: number): Promise<number> {
      const all = await db.babies.toArray();
      const used = new Set(all.map((b) => b.colorIndex));
      for (let i = 0; i < paletteSize; i++) if (!used.has(i)) return i;
      return all.length % paletteSize;
    },

    /** Salva e, se for um bebê novo, já o deixa ativo. Retorna o id. */
    async save(baby: Baby): Promise<number> {
      if (baby.id !== undefined) {
        await db.babies.put(baby);
        return baby.id;
      }
      const id = await db.babies.add(baby);
      Prefs.setActiveBabyId(id);
      return id;
    },

    /** Exclui o bebê e tudo que pertence a ele (registros e fotos), numa única transação. */
    async delete(baby: Baby): Promise<void> {
      if (baby.id === undefined) return;
      const id = baby.id;
      await db.transaction('rw', db.allTables, async () => {
        for (const table of db.recordTables) await table.where('babyId').equals(id).delete();
        await db.babies.delete(id);
      });
      if (Prefs.getActiveBabyId() === id) Prefs.setActiveBabyId(null);
    },
  };

  // --- Fotos ---

  const photos = {
    forOwner: async (type: RecordType, ownerId: number): Promise<Photo[]> =>
      (await db.photos.where('[ownerType+ownerId]').equals([type, ownerId]).toArray()).sort(
        byDateAscIdAsc((p) => p.addedAt),
      ),

    /** Deixa o registro com exatamente as fotos [keepIds] mais as [added]: apaga as removidas e grava as novas. */
    async sync(
      babyId: number,
      type: RecordType,
      ownerId: number,
      keepIds: number[],
      added: Blob[],
    ): Promise<void> {
      const existing = await photos.forOwner(type, ownerId);
      const toDelete = existing.filter((p) => !keepIds.includes(p.id!)).map((p) => p.id!);
      if (toDelete.length > 0) await db.photos.bulkDelete(toDelete);
      // Na listagem, fotos do mesmo minuto ficam na ordem de inclusão (desempate pelo id).
      const addedAt = now();
      for (const blob of added) await db.photos.add({ babyId, ownerType: type, ownerId, blob, addedAt });
    },

    deleteFor: async (type: RecordType, ownerId: number): Promise<void> => {
      await db.photos.where('[ownerType+ownerId]').equals([type, ownerId]).delete();
    },

    /** Fotos agrupadas por registro (chave "TIPO:id"); o tipo BABY não entra. */
    async idsByOwner(babyId: number): Promise<Record<string, number[]>> {
      const all = (await db.photos.where('babyId').equals(babyId).toArray()).sort(byDateAscIdAsc((p) => p.addedAt));
      const result: Record<string, number[]> = {};
      for (const p of all) {
        if (p.ownerType === 'BABY') continue;
        (result[recordKey(p.ownerType, p.ownerId)] ??= []).push(p.id!);
      }
      return result;
    },

    get: (id: number): Promise<Photo | undefined> => db.photos.get(id),
  };

  // --- Registros ---

  const save = async <T extends { id?: number }>(table: { add(i: T): Promise<number>; put(i: T): Promise<number> }, item: T) =>
    item.id === undefined ? table.add(item) : table.put(item);

  const records = {
    // Crescimento
    listGrowth: async (babyId: number): Promise<GrowthMeasurement[]> =>
      (await db.growth.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((g) => g.date)),
    getGrowth: (id: number) => db.growth.get(id),
    saveGrowth: (item: GrowthMeasurement) => save(db.growth, item),

    // Vacinas
    async listVaccines(babyId: number): Promise<Vaccine[]> {
      const all = await db.vaccines.where('babyId').equals(babyId).toArray();
      return all.sort((a, b) => {
        // Sem idade recomendada ("outras vacinas") vão para o fim.
        const aNull = a.recommendedAgeMonths == null;
        const bNull = b.recommendedAgeMonths == null;
        if (aNull !== bNull) return aNull ? 1 : -1;
        if (!aNull && !bNull && a.recommendedAgeMonths !== b.recommendedAgeMonths) {
          return a.recommendedAgeMonths! - b.recommendedAgeMonths!;
        }
        return cmp(vaccineReferenceDate(a) ?? '', vaccineReferenceDate(b) ?? '') || (a.id ?? 0) - (b.id ?? 0);
      });
    },
    getVaccine: (id: number) => db.vaccines.get(id),
    saveVaccine: (item: Vaccine) => save(db.vaccines, item),

    /** Adiciona as vacinas do calendário sugerido que ainda não existem. Retorna quantas entraram. */
    async addSuggestedVaccines(baby: Baby): Promise<number> {
      const existing = await db.vaccines.where('babyId').equals(baby.id!).toArray();
      const missing = VaccineSchedule.missingFor(baby, existing);
      if (missing.length > 0) await db.vaccines.bulkAdd(missing);
      return missing.length;
    },

    // Consultas
    listAppointments: async (babyId: number): Promise<Appointment[]> =>
      (await db.appointments.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((a) => a.dateTime)),
    /** Consultas ainda agendadas a partir de [from], da mais próxima para a mais distante. */
    async upcomingAppointments(babyId: number, from: LocalDateTime, limit = 5): Promise<Appointment[]> {
      const all = await db.appointments.where('babyId').equals(babyId).toArray();
      return all
        .filter((a) => a.status === 'SCHEDULED' && a.dateTime >= from)
        .sort((a, b) => cmp(a.dateTime, b.dateTime))
        .slice(0, limit);
    },
    getAppointment: (id: number) => db.appointments.get(id),
    saveAppointment: (item: Appointment) => save(db.appointments, item),

    // Medicamentos
    listMedications: async (babyId: number): Promise<MedicationDose[]> =>
      (await db.medications.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((m) => m.scheduledAt)),
    getMedication: (id: number) => db.medications.get(id),
    saveMedication: (item: MedicationDose) => save(db.medications, item),

    /** Agenda várias doses de uma vez (ex.: de 8 em 8 horas). Retorna os ids. */
    saveMedicationDoses: (items: MedicationDose[]) => db.medications.bulkAdd(items, { allKeys: true }) as Promise<number[]>,

    /** Número para ligar as doses de uma mesma repetição. */
    nextSeriesId: async (): Promise<number> => {
      const last = await db.medications.orderBy('seriesId').last();
      return (last?.seriesId ?? 0) + 1;
    },

    /** Doses da mesma série, ainda agendadas, marcadas para depois de [dose]. */
    async followingInSeries(dose: MedicationDose): Promise<MedicationDose[]> {
      if (dose.seriesId == null) return [];
      const inSeries = await db.medications.where('seriesId').equals(dose.seriesId).toArray();
      return inSeries
        .filter((m) => m.id !== dose.id && m.status === 'SCHEDULED' && m.scheduledAt > dose.scheduledAt)
        .sort(byDateAscIdAsc((m) => m.scheduledAt));
    },

    /**
     * Repete nas doses seguintes a edição feita em [original] → [updated]: nome, dose, observações e o
     * deslocamento de horário (se a dose mudou 30 min, as próximas também mudam 30 min).
     */
    async applyEditToFollowing(original: MedicationDose, updated: MedicationDose): Promise<number> {
      const following = await records.followingInSeries(original);
      const shift = minutesBetween(original.scheduledAt, updated.scheduledAt);
      await db.medications.bulkPut(
        following.map((m) => ({
          ...m,
          name: updated.name,
          dosage: updated.dosage,
          notes: updated.notes,
          scheduledAt: plusMinutes(m.scheduledAt, shift),
        })),
      );
      return following.length;
    },

    async deleteFollowingInSeries(dose: MedicationDose): Promise<number> {
      const following = await records.followingInSeries(dose);
      if (following.length > 0) await db.medications.bulkDelete(following.map((m) => m.id!));
      return following.length;
    },

    // Sintomas e mal-estar
    listHealthEvents: async (babyId: number): Promise<HealthEvent[]> =>
      (await db.healthEvents.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((e) => e.dateTime)),
    getHealthEvent: (id: number) => db.healthEvents.get(id),
    saveHealthEvent: (item: HealthEvent) => save(db.healthEvents, item),

    // Alimentação
    async listFeedingsOn(babyId: number, day: LocalDate): Promise<Feeding[]> {
      const from = startOfDay(day);
      const to = startOfDay(plusDays(day, 1));
      const all = await db.feedings.where('babyId').equals(babyId).toArray();
      return all.filter((f) => f.dateTime >= from && f.dateTime < to).sort(byDateAscIdAsc((f) => f.dateTime));
    },
    listFeedings: async (babyId: number): Promise<Feeding[]> =>
      (await db.feedings.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((f) => f.dateTime)),
    /** Alimentos já oferecidos, com a data da primeira vez; o mais recente primeiro. */
    async foodsTried(babyId: number): Promise<FoodTried[]> {
      const all = await db.feedings.where('babyId').equals(babyId).toArray();
      const groups = new Map<string, FoodTried>();
      for (const f of all) {
        const food = f.food?.trim();
        if (!food) continue;
        const key = food.toLowerCase();
        const current = groups.get(key);
        if (!current || f.dateTime < current.firstDateTime) groups.set(key, { food, firstDateTime: f.dateTime });
      }
      return [...groups.values()].sort((a, b) => cmp(b.firstDateTime, a.firstDateTime));
    },
    getFeeding: (id: number) => db.feedings.get(id),
    saveFeeding: (item: Feeding) => save(db.feedings, item),

    // Momentos
    listMoments: async (babyId: number): Promise<Moment[]> =>
      (await db.moments.where('babyId').equals(babyId).toArray()).sort(byDateDescIdDesc((m) => m.dateTime)),
    getMoment: (id: number) => db.moments.get(id),
    saveMoment: (item: Moment) => save(db.moments, item),

    /** Exclui o registro e as fotos vinculadas a ele. */
    async delete(type: RecordType, id: number): Promise<void> {
      await db.transaction('rw', db.allTables, async () => {
        await photos.deleteFor(type, id);
        switch (type) {
          case 'GROWTH':
            return db.growth.delete(id);
          case 'VACCINE':
            return db.vaccines.delete(id);
          case 'APPOINTMENT':
            return db.appointments.delete(id);
          case 'HEALTH_EVENT':
            return db.healthEvents.delete(id);
          case 'FEEDING':
            return db.feedings.delete(id);
          case 'MOMENT':
            return db.moments.delete(id);
          case 'MEDICATION':
            return db.medications.delete(id);
          case 'BABY':
            throw new Error('Use babies.delete para excluir um bebê.');
        }
      });
    },
  };

  // --- Linha do tempo e relatório ---

  const timeline = {
    /** Junta todos os tipos de registro de [start] a [end] (inclusive) numa linha do tempo. */
    async range(babyId: number, start: LocalDate, end: LocalDate, today: LocalDate): Promise<TimelineItem[]> {
      const data = await timeline.loadData(babyId, start, end);
      return sortedChronologically([
        ...data.measurements.map(growthToTimelineItem),
        ...data.vaccines.flatMap((v) => vaccineToTimelineItem(v, today) ?? []),
        ...data.appointments.map(appointmentToTimelineItem),
        ...data.healthEvents.map(healthToTimelineItem),
        ...data.feedings.map(feedingToTimelineItem),
        ...data.moments.map(momentToTimelineItem),
        ...data.medications.map((m) => medicationToTimelineItem(m, now())),
      ]);
    },

    /** Carrega os registros do período (sem fotos). */
    async loadData(babyId: number, start: LocalDate, end: LocalDate): Promise<ReportData> {
      const from = startOfDay(start);
      const to = startOfDay(plusDays(end, 1));
      const inDay = (date: LocalDate) => date >= start && date <= end;
      const inTime = (t: LocalDateTime) => t >= from && t < to;
      const forBaby = <T>(table: { where(k: string): { equals(v: number): { toArray(): Promise<T[]> } } }) =>
        table.where('babyId').equals(babyId).toArray();
      const [measurements, vaccines, appointments, healthEvents, feedings, moments, medications] = await Promise.all([
        forBaby<GrowthMeasurement>(db.growth),
        forBaby<Vaccine>(db.vaccines),
        forBaby<Appointment>(db.appointments),
        forBaby<HealthEvent>(db.healthEvents),
        forBaby<Feeding>(db.feedings),
        forBaby<Moment>(db.moments),
        forBaby<MedicationDose>(db.medications),
      ]);
      return {
        ...emptyReportData(),
        measurements: measurements.filter((m) => inDay(m.date)),
        vaccines: vaccines.filter((v) => {
          const ref = vaccineReferenceDate(v);
          return ref !== null && inDay(ref);
        }),
        appointments: appointments.filter((a) => inTime(a.dateTime)),
        healthEvents: healthEvents.filter((e) => inTime(e.dateTime)),
        feedings: feedings.filter((f) => inTime(f.dateTime)),
        moments: moments.filter((m) => inTime(m.dateTime)),
        medications: medications.filter((m) => inTime(medicationReferenceTime(m))),
      };
    },

    /** Carrega os registros do período para o relatório (as fotos só se pedidas). */
    async loadReportData(babyId: number, start: LocalDate, end: LocalDate, includePhotos: boolean): Promise<ReportData> {
      const data = await timeline.loadData(babyId, start, end);
      return { ...data, photos: includePhotos ? await photos.idsByOwner(babyId) : {} };
    },
  };

  // --- Apagar tudo ---

  /** "Apagar todos os dados" em Ajustes: banco, fotos e preferências deste aparelho. */
  async function eraseEverything(): Promise<void> {
    await db.transaction('rw', db.allTables, async () => {
      for (const table of db.allTables) await table.clear();
    });
    Prefs.clearAll();
  }

  return { babies, photos, records, timeline, eraseEverything, db };
}

export type Repos = ReturnType<typeof createRepos>;

/** Instância padrão usada pelo app. */
export const repos: Repos = createRepos();
