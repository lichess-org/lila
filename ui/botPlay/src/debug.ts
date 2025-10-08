import { Game } from './game';
import type { Move } from './interfaces';

export function debugCli(play: (game: Game) => void) {
  (window['bot' as any] as any) = {
    threefold: () =>
      play(
        new Game({
          id: Game.randomId(),
          botKey: 'terrence',
          pov: 'white',
          initialFen: undefined,
          moves: addMoveTimes(['e4', 'e5', 'Nf3', 'Nf6', 'Ng1', 'Ng8', 'Nf3', 'Nf6', 'Ng1']),
        }),
      ),
    checkmate: () =>
      play(
        new Game({
          id: Game.randomId(),
          botKey: '#terrence',
          pov: 'white',
          initialFen: undefined,
          moves: addMoveTimes(['e4', 'e5', 'Bc4', 'd6', 'Qf3', 'a6']),
        }),
      ),
    promotion: () =>
      play(
        new Game({
          id: Game.randomId(),
          botKey: '#terrence',
          pov: 'white',
          initialFen: '5bnk/PPPPpppp/8/8/8/8/pppPPPPP/RNBQKBNR w KQ - 0 1',
          moves: [],
        }),
      ),
  };
}

const addMoveTimes = (moves: San[]): Move[] => {
  const startedAt = Date.now() - 1000 * moves.length;
  return moves.map((san, i) => ({
    san,
    at: startedAt + 1000 * i,
  }));
};
