import { Game, makeGame } from './game';

export function debugCli(play: (game: Game) => void) {
  (window['bot' as any] as any) = {
    threefold: () =>
      play(makeGame('#terrence', 'white', ['e4', 'e5', 'Nf3', 'Nf6', 'Ng1', 'Ng8', 'Nf3', 'Nf6', 'Ng1'])),
    checkmate: () => play(makeGame('#terrence', 'white', ['e4', 'e5', 'Bc4', 'd6', 'Qf3', 'a6'])),
    promotion: () =>
      play({
        ...makeGame('#terrence', 'white'),
        initialFen: '5bnk/PPPPpppp/8/8/8/8/pppPPPPP/RNBQKBNR w KQ - 0 1',
      }),
  };
}
