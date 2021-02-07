import * as xhr from 'common/xhr';
import { PuzzleReplay, PuzzleResult, ThemeKey } from './interfaces';
import { defined } from 'common';

export function complete(
  puzzleId: string,
  theme: ThemeKey,
  win: boolean,
  replay?: PuzzleReplay
): Promise<PuzzleResult | undefined> {
  return xhr.json(`/training/complete/${theme}/${puzzleId}`, {
    method: 'POST',
    body: xhr.form({
      win,
      ...(replay ? { replayDays: replay.days } : {}),
    }),
  });
}

export function vote(puzzleId: string, vote: boolean): Promise<void> {
  return xhr.json(`/training/${puzzleId}/vote`, {
    method: 'POST',
    body: xhr.form({ vote }),
  });
}

export function voteTheme(puzzleId: string, theme: ThemeKey, vote: boolean | undefined): Promise<void> {
  return xhr.json(`/training/${puzzleId}/vote/${theme}`, {
    method: 'POST',
    body: defined(vote) ? xhr.form({ vote }) : undefined,
  });
}
