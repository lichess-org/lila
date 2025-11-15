import { Game } from './game';
import type { Move } from './interfaces';

export function debugCli(play: (game: Game) => void) {
  (window['bot' as any] as any) = {
    threefold: () =>
      play(
        new Game({
          id: Game.randomId(),
          botKey: '#centipawn',
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
          botKey: '#tal-e',
          pov: 'white',
          initialFen: '5bnk/PPPPpppp/8/8/8/8/pppPPPPP/RNBQKBNR w KQ - 0 1',
          moves: [],
        }),
      ),
    materialImbalance: () =>
      play(
        new Game({
          id: Game.randomId(),
          botKey: '#professor',
          clockConfig: { initial: 7200, increment: 20, moretime: 0 },
          pov: 'white',
          moves: addMoveTimes(
            'Nf3 Nf6 g3 g6 Bg2 Bg7 O-O O-O d4 d5 c4 c5 dxc5 dxc4 Qxd8 Rxd8 Ne5 Na6 Be3 Nd5 Nxc4 Nxe3 fxe3 Nxc5 Nc3 Be6 Na5 Rac8 Rad1 Bh6 Kf2 Kg7 Rd4 Bg5 Rfd1 Rxd4 exd4 b6 Nc6 Na6 Nxa7 Rc7 Nab5 Rd7 d5 Bf5 e4 Bg4 Bf3 Bxf3'.split(
              ' ',
            ),
          ),
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
