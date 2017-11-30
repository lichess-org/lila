import { GameData, GameView, Player, Status, Source, Simul } from './interfaces';

import * as game from './game';
import * as status from './status';
import * as router from './router';
import viewStatus from './view/status';

export { GameData, Player, Status, Source, Simul, game, status, router };

export const view: GameView = {
  status: viewStatus
};

export const perf = {
  icons: {
    ultraBullet: "{",
    bullet: "T",
    blitz: ")",
    classical: "+",
    correspondence: ";",
    chess960: "'",
    kingOfTheHill: "(",
    threeCheck: ".",
    antichess: "@",
    atomic: ">",
    horde: "_"
  }
};
