import { json as xhrJson, form as xhrForm } from 'lib/xhr';

import type { GuessPosition, GuessResult } from './interfaces';

export function next(): Promise<GuessPosition> {
  return xhrJson('/training/guess/next');
}

export function guess(id: string, isPuzzle: boolean): Promise<GuessResult> {
  return xhrJson(`/training/guess/${id}`, {
    method: 'POST',
    body: xhrForm({ isPuzzle }),
  });
}

export function solve(id: string, win: boolean): Promise<GuessResult> {
  return xhrJson(`/training/guess/${id}/solve`, {
    method: 'POST',
    body: xhrForm({ win }),
  });
}
