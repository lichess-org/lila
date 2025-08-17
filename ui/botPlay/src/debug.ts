import { Game, Move } from './game';

export function debugCli(play: (game: Game) => void) {
  (window['bot' as any] as any) = {
    threefold: () =>
      play(
        new Game(
          '#terrence',
          'white',
          undefined,
          undefined,
          addMoveTimes(['e4', 'e5', 'Nf3', 'Nf6', 'Ng1', 'Ng8', 'Nf3', 'Nf6', 'Ng1']),
        ),
      ),
    checkmate: () =>
      play(
        new Game(
          '#terrence',
          'white',
          undefined,
          undefined,
          addMoveTimes(['e4', 'e5', 'Bc4', 'd6', 'Qf3', 'a6']),
        ),
      ),
    promotion: () =>
      play(
        new Game('#terrence', 'white', undefined, '5bnk/PPPPpppp/8/8/8/8/pppPPPPP/RNBQKBNR w KQ - 0 1', []),
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
