import { Chess } from 'chessops';
import { makeFen } from 'chessops/fen';
import { randomId } from 'lib/algo';
import { StatusName } from 'lib/game/game';
import type { ClockConfig, SetData as ClockState } from 'lib/game/clock/clockCtrl';
import { type BotId } from 'lib/bot/types';
import { DateMillis } from './interfaces';

export interface Move {
  san: San;
  at: DateMillis;
}

export interface Game {
  id: string;
  botId: BotId;
  pov: Color;
  clockConfig?: ClockConfig;
  initialFen?: FEN;
  moves: Move[];
  end?: GameEnd;
}
interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
}

export const makeGame = (botId: BotId, pov: Color, clockConfig?: ClockConfig, moves: Move[] = []): Game => ({
  id: randomId(),
  botId,
  pov,
  clockConfig,
  moves,
});

export const makeEndOf = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};

export const isClockTicking = (game: Game): Color | undefined =>
  game.end || game.moves.length < 2 ? undefined : game.moves.length % 2 ? 'black' : 'white';

export const computeClockState = (game: Game): ClockState | undefined => {
  const config = game.clockConfig;
  if (!config) return;
  const state = {
    white: config.initial,
    black: config.initial,
  };
  let lastMoveAt: DateMillis | undefined;
  game.moves.forEach(({ at }, i) => {
    const color = i % 2 ? 'black' : 'white';
    if (lastMoveAt && i > 1) {
      state[color] = Math.max(0, state[color] - (at - lastMoveAt) / 1000 + config.increment);
    }
    lastMoveAt = at;
  });
  const ticking = isClockTicking(game);
  if (ticking && lastMoveAt && game.moves.length > 1) state[ticking] -= (Date.now() - lastMoveAt) / 1000;
  return {
    ...state,
    ticking,
  };
};
