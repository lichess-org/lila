import { GameData } from './interfaces';

// https://github.com/lichess-org/scalachess/blob/master/core/src/main/scala/Status.scala

export const ids = {
  created: 10,
  started: 20,
  aborted: 25,
  mate: 30,
  resign: 31,
  stalemate: 32,
  timeout: 33,
  draw: 34,
  outoftime: 35,
  cheat: 36,
  noStart: 37,
  variantEnd: 60,
};

export const started = (data: GameData): boolean => data.game.status.id >= ids.started;

export const finished = (data: GameData): boolean => data.game.status.id >= ids.mate;

export const aborted = (data: GameData): boolean => data.game.status.id === ids.aborted;

export const playing = (data: GameData): boolean => started(data) && !finished(data) && !aborted(data);
