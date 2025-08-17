import type { ClockConfig, SetData as ClockState } from 'lib/game/clock/clockCtrl';
import { DateMillis } from './interfaces';

interface ClockMove {
  at: DateMillis;
}

export const computeClockState = (
  config: ClockConfig,
  moves: ClockMove[],
  ticking: Color | undefined,
): ClockState | undefined => {
  if (!config) return;
  const state = {
    white: config.initial,
    black: config.initial,
  };
  let lastMoveAt: DateMillis | undefined;
  moves.forEach(({ at }, i) => {
    const color = i % 2 ? 'black' : 'white';
    if (lastMoveAt && i > 1) {
      state[color] = Math.max(0, state[color] - (at - lastMoveAt) / 1000 + config.increment);
    }
    lastMoveAt = at;
  });
  if (ticking && lastMoveAt && moves.length > 1)
    state[ticking] = Math.max(0, state[ticking] - (Date.now() - lastMoveAt) / 1000);
  return {
    ...state,
    ticking,
  };
};
