import { Game, makeGame } from './game';

export function debugCli(play: (game: Game) => void) {
  (window['bot' as any] as any) = {
    threefold: () =>
      play(makeGame('#terrence', 'white', ['e4', 'e5', 'Nf3', 'Nf6', 'Ng1', 'Ng8', 'Nf3', 'Nf6', 'Ng1'])),
  };
}
