// Banco de dados local (IndexedDB via Dexie). Nada sai do navegador.
// Espelha as tabelas de data/db/AppDatabase.kt do app Android.

import Dexie, { type Table } from 'dexie';
import type {
  Appointment,
  Baby,
  Feeding,
  GrowthMeasurement,
  HealthEvent,
  MedicationDose,
  Moment,
  Photo,
  Vaccine,
} from '../domain/types';

export const DB_NAME = 'crescer-bem';

export class CrescerBemDB extends Dexie {
  babies!: Table<Baby, number>;
  growth!: Table<GrowthMeasurement, number>;
  vaccines!: Table<Vaccine, number>;
  appointments!: Table<Appointment, number>;
  healthEvents!: Table<HealthEvent, number>;
  feedings!: Table<Feeding, number>;
  medications!: Table<MedicationDose, number>;
  moments!: Table<Moment, number>;
  photos!: Table<Photo, number>;

  constructor(name: string = DB_NAME) {
    super(name);
    this.version(1).stores({
      babies: '++id, birthDate',
      growth: '++id, babyId, [babyId+date]',
      vaccines: '++id, babyId',
      appointments: '++id, babyId, [babyId+dateTime]',
      healthEvents: '++id, babyId, [babyId+dateTime]',
      feedings: '++id, babyId, [babyId+dateTime]',
      medications: '++id, babyId, [babyId+scheduledAt], seriesId',
      moments: '++id, babyId, [babyId+dateTime]',
      photos: '++id, babyId, [ownerType+ownerId]',
    });
  }

  /** Todas as tabelas de registros que pertencem a um bebê (todas, menos `babies`). */
  get recordTables(): Table<{ babyId: number }, number>[] {
    return [
      this.growth,
      this.vaccines,
      this.appointments,
      this.healthEvents,
      this.feedings,
      this.medications,
      this.moments,
      this.photos,
    ] as Table<{ babyId: number }, number>[];
  }

  get allTables(): Table<unknown, number>[] {
    return [this.babies, ...this.recordTables] as Table<unknown, number>[];
  }
}

export const db = new CrescerBemDB();
