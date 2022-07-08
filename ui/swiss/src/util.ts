import { Outcome } from './interfaces';

export const isOutcome = (s: any): s is Outcome => s == 'absent' || s == 'late' || s == 'bye';
