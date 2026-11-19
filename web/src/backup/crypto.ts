// Criptografia da cópia de segurança com a Web Crypto API (nativa do navegador, sem biblioteca).
// Senha -> chave: PBKDF2-SHA-256. Cifra: AES-GCM de 256 bits (autenticada: senha errada ou arquivo
// alterado falham na decifragem em vez de devolver lixo).

export const KDF_ITERATIONS = 600_000;
const SALT_BYTES = 16;
const IV_BYTES = 12;

export class WrongPasswordError extends Error {
  constructor() {
    super('Senha incorreta ou arquivo alterado.');
    this.name = 'WrongPasswordError';
  }
}

export function toBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

export function fromBase64(text: string): Uint8Array {
  const binary = atob(text);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

async function deriveKey(password: string, salt: Uint8Array, iterations: number): Promise<CryptoKey> {
  const material = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, [
    'deriveKey',
  ]);
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt, iterations, hash: 'SHA-256' },
    material,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  );
}

export interface EncryptedPayload {
  kdf: { name: 'PBKDF2'; hash: 'SHA-256'; iterations: number; salt: string };
  cipher: { name: 'AES-GCM'; iv: string };
  /** Texto cifrado em base64. */
  data: string;
}

export async function encryptText(plain: string, password: string, iterations = KDF_ITERATIONS): Promise<EncryptedPayload> {
  const salt = crypto.getRandomValues(new Uint8Array(SALT_BYTES));
  const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
  const key = await deriveKey(password, salt, iterations);
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(plain));
  return {
    kdf: { name: 'PBKDF2', hash: 'SHA-256', iterations, salt: toBase64(salt) },
    cipher: { name: 'AES-GCM', iv: toBase64(iv) },
    data: toBase64(new Uint8Array(encrypted)),
  };
}

export async function decryptText(payload: EncryptedPayload, password: string): Promise<string> {
  // Limites para um arquivo malicioso não travar o navegador com milhões de iterações.
  if (payload.kdf?.name !== 'PBKDF2' || payload.cipher?.name !== 'AES-GCM') {
    throw new Error('Formato de criptografia não reconhecido.');
  }
  const iterations = payload.kdf.iterations;
  if (!Number.isInteger(iterations) || iterations < 1 || iterations > 5_000_000) {
    throw new Error('Arquivo de cópia de segurança inválido.');
  }
  const salt = fromBase64(payload.kdf.salt);
  const iv = fromBase64(payload.cipher.iv);
  const key = await deriveKey(password, salt, iterations);
  try {
    const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, fromBase64(payload.data));
    return new TextDecoder().decode(plain);
  } catch {
    throw new WrongPasswordError();
  }
}
