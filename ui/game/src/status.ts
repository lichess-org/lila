import { GameData, StatusId, StatusName } from './interfaces';

// https://github.com/WandererXII/scalashogi/blob/main/src/main/scala/Status.scala

export const ids: Record<StatusName, StatusId> = {
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
  unknownFinish: 38,
  tryRule: 39,
  perpetualCheck: 40,
  impasse27: 41,
  royalsLost: 42,
  bareKing: 43,
  repetition: 44,
  specialVariantEnd: 45,
};

export function statusIdToName(statusId: StatusId): StatusName | undefined {
  for (const name in ids) {
    if (ids[name as StatusName] === statusId) {
      return name as StatusName;
    }
  }
  return undefined;
}

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
