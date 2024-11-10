import { json as xhrJson, form as xhrForm, text as xhrText } from 'common/xhr';
import type PuzzleStreak from './streak';
import { throttlePromiseDelay } from 'common/timing';
import { defined } from 'common';
import { PuzzleReplay, PuzzleResult, ThemeKey } from './interfaces';
import { StoredProp } from 'common/storage';

export const complete = (
  puzzleId: string,
  theme: ThemeKey,
  win: boolean,
  rated: StoredProp<boolean>,
  replay?: PuzzleReplay,
  streak?: PuzzleStreak,
  color?: Color,
): Promise<PuzzleResult> =>
  xhrJson(`/training/complete/${theme}/${puzzleId}`, {
    method: 'POST',
    body: xhrForm({
      win,
      ...(replay ? { replayDays: replay.days } : {}),
      ...(streak ? { streakId: streak.nextId(), streakScore: streak.data.index } : {}),
      rated: rated(),
      color,
    }),
  });

export const vote = (puzzleId: string, vote: boolean): Promise<void> =>
  xhrJson(`/training/${puzzleId}/vote`, {
    method: 'POST',
    body: xhrForm({ vote }),
  });

export const voteTheme = (puzzleId: string, theme: ThemeKey, vote: boolean | undefined): Promise<void> =>
  xhrJson(`/training/${puzzleId}/vote/${theme}`, {
    method: 'POST',
    body: defined(vote) ? xhrForm({ vote }) : undefined,
  });

export const report = (puzzleId: string, reason: string): Promise<void> =>
  xhrJson(`/training/${puzzleId}/report`, {
    method: 'POST',
    body: xhrForm({ reason: reason }),
  });

export const setZen = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhrText('/pref/zen', {
      method: 'post',
      body: xhrForm({ zen: zen ? 1 : 0 }),
    }),
);
