import * as xhr from 'common/xhr';
import PuzzleStreak from './streak';
import throttle from 'common/throttle';
import { defined } from 'common';
import { PuzzleReplay, PuzzleResult, ThemeKey } from './interfaces';

export function complete(
  puzzleId: string,
  theme: ThemeKey,
  win: boolean,
  replay?: PuzzleReplay,
  streak?: PuzzleStreak
): Promise<PuzzleResult | undefined> {
  return xhr.json(`/training/complete/${theme}/${puzzleId}`, {
    method: 'POST',
    body: xhr.form({
      win,
      ...(replay ? { replayDays: replay.days } : {}),
      ...(streak ? { streakId: streak.currentId(), streakScore: streak.current } : {}),
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

export const setZen = throttle(1000, zen =>
  xhr.text('/pref/zen', {
    method: 'post',
    body: xhr.form({ zen: zen ? 1 : 0 }),
  })
);
