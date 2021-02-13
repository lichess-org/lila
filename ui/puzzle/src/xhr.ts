import { PuzzleRound, PuzzleVote, PuzzleData } from './interfaces';
import * as xhr from 'common/xhr';

export function round(puzzleId: number, win: boolean): Promise<PuzzleRound> {
  return xhr.json(`/training/${puzzleId}/round2`, {
    method: 'POST',
    body: xhr.form({ win: win ? 1 : 0 }),
    headers: { ...xhr.xhrHeader }
  });
}

export function vote(puzzleId: number, v: boolean): Promise<PuzzleVote> {
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
