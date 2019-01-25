import { GameData, GameView, Player, PlayerUser, Status, Source, Simul, Clock } from './interfaces';

import * as game from './game';
import * as status from './status';
import * as router from './router';
import viewStatus from './view/status';

export { GameData, Player, PlayerUser, Status, Source, Simul, Clock, game, status, router };

export const view: GameView = {
  status: viewStatus
};

export const perf = {
  icons: {
    ultraBullet: "{",
    bullet: "T",
    blitz: ")",
    rapid: "C",
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
