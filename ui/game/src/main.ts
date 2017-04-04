/// <reference types="types/lichess" />
/// <reference types="types/mithril" />

import { GameView } from './interfaces';

import * as game from './game';
import * as status from './status';
import * as router from './router';
import viewStatus from './view/status';
import * as viewMod from './view/mod';

export { game, status, router };

export const view: GameView = {
  status: viewStatus,
  mod: viewMod
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
