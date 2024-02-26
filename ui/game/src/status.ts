import { GameData } from './interfaces';

// https://github.com/WandererXII/scalashogi/blob/main/src/main/scala/Status.scala

export const ids = {
  created: 10,
  started: 20,
  paused: 21,
  aborted: 25,
  mate: 30,
  resign: 31,
  stalemate: 32,
  timeout: 33,
  draw: 34,
  outoftime: 35,
  cheat: 36,
  noStart: 37,
  tryRule: 39,
  perpetualCheck: 40,
  impasse27: 41,
  royalsLost: 42,
  bareKing: 43,
  repetition: 44,
};

export function started(data: GameData): boolean {
  return data.game.status.id >= ids.started;
}

export function prepaused(data: GameData): boolean {
  return !!data.player.offeringPause && !!data.opponent.offeringPause;
}

export function paused(data: GameData): boolean {
  return data.game.status.id === ids.paused;
}

export function finished(data: GameData): boolean {
  return data.game.status.id >= ids.mate;
}

export function aborted(data: GameData): boolean {
  return data.game.status.id === ids.aborted;
}

export function playing(data: GameData): boolean {
  return started(data) && !finished(data) && !aborted(data);
}
