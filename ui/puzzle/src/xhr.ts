import { defined } from 'common/common';
import throttle from 'common/throttle';
import { PuzzleReplay, PuzzleResult, ThemeKey } from './interfaces';

export function complete(
  puzzleId: string,
  theme: ThemeKey,
  win: boolean,
  replay?: PuzzleReplay
): Promise<PuzzleResult | undefined> {
  return window.lishogi.xhr.json('POST', `/training/complete/${theme}/${puzzleId}`, {
    formData: {
      win,
      ...(replay ? { replayDays: replay.days } : {}),
    },
  });
}

export function vote(puzzleId: string, vote: boolean): Promise<void> {
  return window.lishogi.xhr.json('POST', `/training/${puzzleId}/vote`, {
    formData: { vote },
  });
}

export function voteTheme(
  puzzleId: string,
  theme: ThemeKey,
  vote: boolean | undefined
): Promise<void> {
  return window.lishogi.xhr.json(
    'POST',
    `/training/${puzzleId}/vote/${theme}`,
    defined(vote)
      ? {
          formData: { vote },
        }
      : undefined
  );
}

export const setZen: (zen: boolean) => void = throttle(1000, zen =>
  window.lishogi.xhr.text('POST', '/pref/zen', {
    formData: { zen: zen ? 1 : 0 },
  })
);
