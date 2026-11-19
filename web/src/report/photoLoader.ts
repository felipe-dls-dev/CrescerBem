// Fotos e imagens no navegador: reduzir antes de guardar e preparar miniaturas para o PDF.

import type { Repos } from '../data/repo';
import type { PdfPhoto, PhotoLoader } from './pdfReport';

/** Maior lado, em pixels, das fotos guardadas (mesmo limite do app Android). */
export const MAX_STORED_PHOTO_SIDE = 1600;
const THUMB_DECODE_PX = 330;

/** Fator de escala para caber em [maxSide] sem ampliar. */
export function scaleFactor(width: number, height: number, maxSide: number): number {
  const longest = Math.max(width, height);
  return longest <= maxSide || longest === 0 ? 1 : maxSide / longest;
}

async function drawScaled(blob: Blob, maxSide: number, quality: number): Promise<{ blob: Blob; width: number; height: number }> {
  const bitmap = await createImageBitmap(blob);
  try {
    const scale = scaleFactor(bitmap.width, bitmap.height, maxSide);
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Não foi possível processar a imagem.');
    // Fundo branco: fotos com transparência não ficam pretas ao virar JPEG.
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(bitmap, 0, 0, width, height);
    const out = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality));
    if (!out) throw new Error('Não foi possível processar a imagem.');
    return { blob: out, width, height };
  } finally {
    bitmap.close();
  }
}

/**
 * Reduz a foto escolhida e a regrava como JPEG. Também descarta os metadados da câmera
 * (como a localização, se houver), pois a imagem é redesenhada do zero.
 */
export async function prepareStoredPhoto(file: Blob): Promise<Blob> {
  return (await drawScaled(file, MAX_STORED_PHOTO_SIDE, 0.85)).blob;
}

const blobToDataUrl = (blob: Blob): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(blob);
  });

/** Carregador de miniaturas para o PDF, lendo as fotos do IndexedDB. */
export function pdfPhotoLoader(repos: Repos): PhotoLoader {
  return async (photoId: number): Promise<PdfPhoto | null> => {
    const photo = await repos.photos.get(photoId);
    if (!photo) return null;
    try {
      const thumb = await drawScaled(photo.blob, THUMB_DECODE_PX, 0.8);
      return { dataUrl: await blobToDataUrl(thumb.blob), width: thumb.width, height: thumb.height };
    } catch {
      return null; // foto ilegível: o relatório sai sem ela
    }
  };
}
