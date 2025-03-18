import { Chess } from 'chessops';
import { makeFen } from 'chessops/fen';
import { randomId } from 'common/algo';
import { StatusName } from 'game';
import { BotId } from 'local';

export interface Game {
  id: string;
  botId: BotId;
  pov: Color;
  sans: San[];
  end?: GameEnd;
}
interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
}

export const makeGame = (botId: BotId, pov: Color, sans: San[] = []): Game => ({
  id: randomId(),
  botId,
  pov,
  sans,
});

export const makeEndOf = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};
