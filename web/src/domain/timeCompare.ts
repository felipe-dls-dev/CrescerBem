import type { LocalDateTime } from './types';

// Os textos "AAAA-MM-DDTHH:mm" ordenam corretamente como texto.
export const isBeforeTime = (a: LocalDateTime, b: LocalDateTime): boolean => a < b;
export const isAfterTime = (a: LocalDateTime, b: LocalDateTime): boolean => a > b;
