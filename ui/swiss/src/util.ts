import { Outcome } from './interfaces';

export function isOutcome(s: any): s is Outcome {
  return s == 'absent' || s == 'late' || s == 'bye';
}
