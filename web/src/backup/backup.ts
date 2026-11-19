// Cópia de segurança: exporta tudo para um arquivo e restaura a partir dele.
// O arquivo é gerado e lido só no aparelho; nada passa por servidor. Como o app não tem nuvem,
// este arquivo é a única forma de guardar os dados fora do navegador ou levá-los a outro aparelho.

import type { CrescerBemDB } from '../data/db';
import { Prefs } from '../data/prefs';
import { parseDate } from '../domain/dates';
import {
  ACCEPTANCES,
  APPOINTMENT_STATUSES,
  BREAST_SIDES,
  FEEDING_TYPES,
  FOOD_TEXTURES,
  INTENSITIES,
  MEASUREMENT_TYPES,
  MEDICATION_STATUSES,
  RECORD_TYPES,
  SEXES,
  SYMPTOMS,
  VACCINE_STATUSES,
  type Photo,
} from '../domain/types';
import { decryptText, encryptText, fromBase64, toBase64, type EncryptedPayload } from './crypto';

export const BACKUP_FORMAT = 'crescer-bem-backup';
export const BACKUP_VERSION = 1;
export const MIN_PASSWORD_LENGTH = 6;

export class InvalidBackupError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InvalidBackupError';
  }
}

/** Foto dentro do arquivo: o Blob vira texto base64. */
interface PhotoRow extends Omit<Photo, 'blob'> {
  mime: string;
  base64: string;
}

type Row = Record<string, unknown>;

export interface BackupData {
  babies: Row[];
  growth: Row[];
  vaccines: Row[];
  appointments: Row[];
  healthEvents: Row[];
  feedings: Row[];
  medications: Row[];
  moments: Row[];
  photos: Row[];
}

interface PlainEnvelope {
  format: typeof BACKUP_FORMAT;
  version: number;
  exportedAt: string;
  encrypted: false;
  data: BackupData;
}

interface EncryptedEnvelope extends EncryptedPayload {
  format: typeof BACKUP_FORMAT;
  version: number;
  exportedAt: string;
  encrypted: true;
}

export type BackupEnvelope = PlainEnvelope | EncryptedEnvelope;

export interface ImportSummary {
  babies: number;
  records: number;
  photos: number;
}

// ---------------------------------------------------------------------------
// Exportar
// ---------------------------------------------------------------------------

async function snapshot(db: CrescerBemDB): Promise<BackupData> {
  const photos = await db.photos.toArray();
  const photoRows: PhotoRow[] = [];
  for (const { blob, ...rest } of photos) {
    photoRows.push({ ...rest, mime: blob.type || 'image/jpeg', base64: toBase64(new Uint8Array(await blob.arrayBuffer())) });
  }
  const rows = (items: object[]) => items as Row[];
  return {
    babies: rows(await db.babies.toArray()),
    growth: rows(await db.growth.toArray()),
    vaccines: rows(await db.vaccines.toArray()),
    appointments: rows(await db.appointments.toArray()),
    healthEvents: rows(await db.healthEvents.toArray()),
    feedings: rows(await db.feedings.toArray()),
    medications: rows(await db.medications.toArray()),
    moments: rows(await db.moments.toArray()),
    photos: rows(photoRows),
  };
}

/**
 * Gera o texto do arquivo de cópia de segurança. Com [password] o conteúdo vai criptografado
 * (recomendado: são dados de saúde); sem senha, vai em texto legível.
 */
export async function createBackup(
  db: CrescerBemDB,
  password: string | null,
  exportedAt: string = new Date().toISOString(),
  iterations?: number,
): Promise<string> {
  const data = await snapshot(db);
  if (password === null) {
    const envelope: PlainEnvelope = { format: BACKUP_FORMAT, version: BACKUP_VERSION, exportedAt, encrypted: false, data };
    return JSON.stringify(envelope);
  }
  if (password.length < MIN_PASSWORD_LENGTH) {
    throw new Error(`A senha precisa ter pelo menos ${MIN_PASSWORD_LENGTH} caracteres.`);
  }
  const payload = await encryptText(JSON.stringify(data), password, iterations);
  const envelope: EncryptedEnvelope = {
    format: BACKUP_FORMAT,
    version: BACKUP_VERSION,
    exportedAt,
    encrypted: true,
    ...payload,
  };
  return JSON.stringify(envelope);
}

/** Nome sugerido para o arquivo, ex.: "crescer-bem-copia-2026-11-19.json". */
export const backupFileName = (day: string): string => `crescer-bem-copia-${day}.json`;

// ---------------------------------------------------------------------------
// Ler e validar
// ---------------------------------------------------------------------------

export function parseEnvelope(text: string): BackupEnvelope {
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw new InvalidBackupError('Este arquivo não parece ser uma cópia de segurança do Crescer Bem.');
  }
  const env = parsed as Partial<BackupEnvelope> | null;
  if (!env || typeof env !== 'object' || env.format !== BACKUP_FORMAT) {
    throw new InvalidBackupError('Este arquivo não parece ser uma cópia de segurança do Crescer Bem.');
  }
  if (typeof env.version !== 'number' || env.version > BACKUP_VERSION) {
    throw new InvalidBackupError('Esta cópia foi feita por uma versão mais nova do Crescer Bem. Atualize o app e tente de novo.');
  }
  return env as BackupEnvelope;
}

/** Diz se o arquivo precisa de senha (sem tentar abrir). */
export function isEncrypted(text: string): boolean {
  return parseEnvelope(text).encrypted === true;
}

// Validadores simples: cada um responde "este valor serve?".
type Check = (value: unknown) => boolean;
const isString: Check = (v) => typeof v === 'string';
const isNum: Check = (v) => typeof v === 'number' && Number.isFinite(v);
const isInt: Check = (v) => typeof v === 'number' && Number.isInteger(v);
const isId: Check = (v) => typeof v === 'number' && Number.isInteger(v) && v > 0;
const isBool: Check = (v) => typeof v === 'boolean';
const oneOf =
  (options: readonly string[]): Check =>
  (v) =>
    typeof v === 'string' && options.includes(v);
const isDate: Check = (v) => {
  if (typeof v !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(v)) return false;
  const { year, month, day } = parseDate(v);
  if (month < 1 || month > 12 || day < 1) return false;
  return day <= new Date(Date.UTC(year, month, 0)).getUTCDate();
};
const isTime: Check = (v) => typeof v === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(v);
const isDateTime: Check = (v) => typeof v === 'string' && isDate(v.slice(0, 10)) && v[10] === 'T' && isTime(v.slice(11));
const optional =
  (check: Check): Check =>
  (v) =>
    v === undefined || v === null || check(v);
const listOf =
  (check: Check): Check =>
  (v) =>
    Array.isArray(v) && v.every(check);

type Spec = Record<string, Check>;

const SPECS: Record<keyof BackupData, Spec> = {
  babies: {
    id: isId,
    name: isString,
    birthDate: isDate,
    birthTime: optional(isTime),
    sex: optional(oneOf(SEXES)),
    bloodType: optional(isString),
    notes: optional(isString),
    colorIndex: isInt,
  },
  growth: {
    id: isId,
    babyId: isId,
    date: isDate,
    type: oneOf(MEASUREMENT_TYPES),
    value: isNum,
    place: optional(isString),
    notes: optional(isString),
  },
  vaccines: {
    id: isId,
    babyId: isId,
    name: isString,
    dose: optional(isString),
    recommendedAgeMonths: optional(isInt),
    scheduledDate: optional(isDate),
    appliedDate: optional(isDate),
    status: oneOf(VACCINE_STATUSES),
    lot: optional(isString),
    place: optional(isString),
    reaction: optional(isString),
    notes: optional(isString),
  },
  appointments: {
    id: isId,
    babyId: isId,
    title: isString,
    dateTime: isDateTime,
    status: oneOf(APPOINTMENT_STATUSES),
    professional: optional(isString),
    place: optional(isString),
    reason: optional(isString),
    guidance: optional(isString),
    notes: optional(isString),
  },
  healthEvents: {
    id: isId,
    babyId: isId,
    dateTime: isDateTime,
    symptoms: listOf(oneOf(SYMPTOMS)),
    otherSymptom: optional(isString),
    intensity: oneOf(INTENSITIES),
    temperatureCelsius: optional(isNum),
    duration: optional(isString),
    actionsTaken: optional(isString),
    soughtCare: isBool,
    notes: optional(isString),
  },
  feedings: {
    id: isId,
    babyId: isId,
    dateTime: isDateTime,
    type: oneOf(FEEDING_TYPES),
    breastSide: optional(oneOf(BREAST_SIDES)),
    durationMinutes: optional(isNum),
    amountMl: optional(isNum),
    food: optional(isString),
    texture: optional(oneOf(FOOD_TEXTURES)),
    acceptance: optional(oneOf(ACCEPTANCES)),
    firstTime: isBool,
    reaction: optional(isString),
    notes: optional(isString),
  },
  medications: {
    id: isId,
    babyId: isId,
    name: isString,
    dosage: optional(isString),
    scheduledAt: isDateTime,
    status: oneOf(MEDICATION_STATUSES),
    givenAt: optional(isDateTime),
    notes: optional(isString),
    seriesId: optional(isInt),
  },
  moments: {
    id: isId,
    babyId: isId,
    dateTime: isDateTime,
    title: isString,
    description: optional(isString),
  },
  photos: {
    id: isId,
    babyId: isId,
    ownerType: oneOf(RECORD_TYPES),
    ownerId: isId,
    addedAt: isDateTime,
    caption: optional(isString),
    mime: (v) => typeof v === 'string' && /^image\/[a-z0-9.+-]+$/i.test(v),
    base64: (v) => typeof v === 'string' && /^[A-Za-z0-9+/]*={0,2}$/.test(v),
  },
};

/** Confere a estrutura e devolve só os campos conhecidos de cada registro. */
export function validateBackupData(raw: unknown): BackupData {
  if (!raw || typeof raw !== 'object') throw new InvalidBackupError('A cópia de segurança está vazia ou danificada.');
  const out = {} as BackupData;
  for (const table of Object.keys(SPECS) as (keyof BackupData)[]) {
    const rows = (raw as Record<string, unknown>)[table];
    if (!Array.isArray(rows)) throw new InvalidBackupError(`A cópia de segurança está incompleta (falta "${table}").`);
    const spec = SPECS[table];
    out[table] = rows.map((row, index) => {
      if (!row || typeof row !== 'object') throw new InvalidBackupError(`Registro ${index + 1} de "${table}" está danificado.`);
      const clean: Record<string, unknown> = {};
      for (const [field, check] of Object.entries(spec)) {
        const value = (row as Record<string, unknown>)[field];
        if (!check(value)) {
          throw new InvalidBackupError(`Registro ${index + 1} de "${table}" tem o campo "${field}" inválido.`);
        }
        if (value !== undefined) clean[field] = value;
      }
      return clean;
    });
  }
  // Integridade: todo registro aponta para um bebê que existe, e ids não se repetem.
  const babyIds = new Set(out.babies.map((b) => b.id as number));
  for (const table of Object.keys(SPECS) as (keyof BackupData)[]) {
    const ids = new Set<number>();
    for (const row of out[table]) {
      const id = row.id as number;
      if (ids.has(id)) throw new InvalidBackupError(`A cópia de segurança tem registros repetidos em "${table}".`);
      ids.add(id);
      if (table !== 'babies' && !babyIds.has(row.babyId as number)) {
        throw new InvalidBackupError(`A cópia de segurança tem um registro de "${table}" sem bebê correspondente.`);
      }
    }
  }
  return out;
}

/** Abre o arquivo (decifrando, se preciso) e devolve os dados já validados. Não altera nada no banco. */
export async function readBackup(text: string, password: string | null): Promise<BackupData> {
  const env = parseEnvelope(text);
  let data: unknown;
  if (env.encrypted) {
    if (password === null || password === '') throw new InvalidBackupError('Esta cópia está protegida por senha.');
    const plain = await decryptText(env, password);
    try {
      data = JSON.parse(plain);
    } catch {
      throw new InvalidBackupError('A cópia de segurança está danificada.');
    }
  } else {
    data = env.data;
  }
  return validateBackupData(data);
}

// ---------------------------------------------------------------------------
// Restaurar
// ---------------------------------------------------------------------------

/**
 * Substitui TODOS os dados do aparelho pelos da cópia. Tudo acontece numa única transação:
 * se algo falhar no meio, os dados atuais continuam intactos.
 */
export async function restoreBackup(
  db: CrescerBemDB,
  text: string,
  password: string | null,
): Promise<ImportSummary> {
  const data = await readBackup(text, password);
  const photos: Photo[] = (data.photos as unknown as PhotoRow[]).map(({ mime, base64, ...rest }) => ({
    ...rest,
    blob: new Blob([fromBase64(base64) as BlobPart], { type: mime }),
  }));

  await db.transaction('rw', db.allTables, async () => {
    for (const table of db.allTables) await table.clear();
    await db.babies.bulkAdd(data.babies as never[]);
    await db.growth.bulkAdd(data.growth as never[]);
    await db.vaccines.bulkAdd(data.vaccines as never[]);
    await db.appointments.bulkAdd(data.appointments as never[]);
    await db.healthEvents.bulkAdd(data.healthEvents as never[]);
    await db.feedings.bulkAdd(data.feedings as never[]);
    await db.medications.bulkAdd(data.medications as never[]);
    await db.moments.bulkAdd(data.moments as never[]);
    await db.photos.bulkAdd(photos);
  });

  // O bebê ativo salvo pode não existir na cópia.
  const active = Prefs.getActiveBabyId();
  if (active !== null && !data.babies.some((b) => b.id === active)) Prefs.setActiveBabyId(null);

  const records =
    data.growth.length +
    data.vaccines.length +
    data.appointments.length +
    data.healthEvents.length +
    data.feedings.length +
    data.medications.length +
    data.moments.length;
  return { babies: data.babies.length, records, photos: data.photos.length };
}
