import { PuzzleData, PuzzleResult } from './interfaces';
import * as xhr from 'common/xhr';

export function round(puzzleId: string, win: boolean): Promise<PuzzleResult | undefined> {
  return xhr.json(`/training/${puzzleId}/round3`, {
    method: 'POST',
    body: xhr.form({ win: win ? 1 : 0 })
  });
}

export function vote(puzzleId: string, v: boolean): Promise<void> {
  return xhr.json(`/training/${puzzleId}/vote`, {
    method: 'POST',
    body: xhr.form({ vote: v ? 1 : 0 })
  });
}

// do NOT set mobile API headers here
// they trigger a compat layer
export const nextPuzzle = (): Promise<PuzzleData> =>
  xhr.json('/training/new', {
    headers: { ...xhr.xhrHeader }
  });
