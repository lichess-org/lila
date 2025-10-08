import type { SetData as ClockState } from 'lib/game/clock/clockCtrl';
import type { DateMillis } from './interfaces';
import type { Game } from './game';

export const computeClockState = (g: Game): ClockState | undefined => {
  const config = g.clockConfig;
  if (!config) return;
  const initial = config.initial || 5;
  const state = {
    white: initial,
    black: initial,
  };
  let lastMoveAt: DateMillis | undefined;
  g.moves.forEach(({ at }, i) => {
    const color = i % 2 ? 'black' : 'white';
    if (lastMoveAt && i > 1) {
      state[color] = Math.max(0, state[color] - (at - lastMoveAt) / 1000 + config.increment);
    }
    lastMoveAt = at;
  });

  const ticking = g.isClockTicking();

  if (ticking && lastMoveAt && g.moves.length > 1) {
    // the clock is ticking for the current player, substract time since last move
    state[ticking] = Math.max(0, state[ticking] - (Date.now() - lastMoveAt) / 1000);
  } else if (g.end && lastMoveAt) {
    // the game ended, substract time between last move and game end
    const millisBetweenLastMoveAndEnd = g.end.at - lastMoveAt;
    state[g.turn()] = Math.max(0, state[g.turn()] - millisBetweenLastMoveAndEnd / 1000);
  }

  return {
    ...state,
    ticking,
  };
};
