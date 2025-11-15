import { json as xhrJson, form as xhrForm, text as xhrText } from 'lib/xhr';
import type PuzzleStreak from './streak';
import { throttlePromiseDelay } from 'lib/async';
import { defined } from 'lib';
import type { PuzzleReplay, PuzzleResult, ThemeKey } from './interfaces';

export const complete = (
  puzzleId: string,
  theme: ThemeKey,
  win: boolean,
  rated: boolean,
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
      rated,
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

// should be kept in sync with `PuzzleForm.report` in scala
const maxReportLength = 2000;

export const report = (puzzleId: string, reason: string): Promise<void> =>
  xhrJson(`/training/${puzzleId}/report`, {
    method: 'POST',
    body: xhrForm({ reason: reason.slice(0, maxReportLength) }),
  });

export const setZen = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhrText('/pref/zen', {
      method: 'post',
      body: xhrForm({ zen: zen ? 1 : 0 }),
    }),
);
