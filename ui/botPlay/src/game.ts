import { Chess } from 'chessops';
import { makeFen } from 'chessops/fen';
import { randomId } from 'lib/algo';
import { StatusName } from 'lib/game/game';
import { ClockConfig, ClockData } from 'lib/game/clock/clockCtrl';
import { type BotId } from 'lib/bot/types';

export interface Game {
  id: string;
  botId: BotId;
  pov: Color;
  initialFen?: FEN;
  sans: San[];
  clock?: ClockData;
  end?: GameEnd;
}
interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
}

export const makeGame = (botId: BotId, pov: Color, clock?: ClockConfig, sans: San[] = []): Game => ({
  id: randomId(),
  botId,
  pov,
  sans,
  clock: clock && {
    ...clock,
    white: clock.initial,
    black: clock.initial,
    running: false,
  },
});

export const makeEndOf = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};
