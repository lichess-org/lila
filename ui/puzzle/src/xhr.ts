import * as xhr from 'common/xhr';
import { PuzzleResult, ThemeKey } from './interfaces';
import {defined} from 'common';

export function complete(puzzleId: string, theme: ThemeKey | undefined, win: boolean): Promise<PuzzleResult | undefined> {
  return xhr.json(`/training/complete/${theme || "any"}/${puzzleId}`, {
    method: 'POST',
    body: xhr.form({ win: win ? 1 : 0 })
  });
}

export function vote(puzzleId: string, vote: boolean | undefined): Promise<void> {
  return xhr.json(`/training/${puzzleId}/vote`, {
    method: 'POST',
    body: defined(vote) ? xhr.form({ vote }) : undefined
  });
}
