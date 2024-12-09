import type { Outcome, PairingExt } from './interfaces';

export const isOutcome = (s: PairingExt | string): s is Outcome => s == 'absent' || s == 'late' || s == 'bye';
