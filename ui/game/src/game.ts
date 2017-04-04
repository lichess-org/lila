import { Data, Player } from './interfaces';
import * as status from './status';

export function playable(data: Data): boolean {
  return data.game.status.id < status.ids.aborted && !imported(data);
}

export function isPlayerPlaying(data: Data): boolean {
  return playable(data) && !data.player.spectator;
}

export function isPlayerTurn(data: Data): boolean {
  return isPlayerPlaying(data) && data.game.player == data.player.color;
}

export function mandatory(data: Data): boolean {
  return !!data.tournament || !!data.simul;
}

export function playedTurns(data: Data): number {
  return data.game.turns - data.game.startedAtTurn;
}

export function bothPlayersHavePlayed(data: Data): boolean {
  return playedTurns(data) > 1;
}

export function abortable(data: Data): boolean {
  return playable(data) && !bothPlayersHavePlayed(data) && !mandatory(data);
}

export function takebackable(data: Data): boolean {
  return playable(data) &&
    data.takebackable &&
    !data.tournament &&
    !data.simul &&
    bothPlayersHavePlayed(data) &&
    !data.player.proposingTakeback &&
    !data.opponent.proposingTakeback;
}

export function drawable(data: Data): boolean {
  return playable(data) &&
    data.game.turns >= 2 &&
    !data.player.offeringDraw &&
    !hasAi(data);
}

export function resignable(data: Data): boolean {
  return playable(data) && !abortable(data);
}

// can the current player go berserk?
export function berserkableBy(data: Data): boolean {
  return !!data.tournament &&
    data.tournament.berserkable &&
    isPlayerPlaying(data) &&
    !bothPlayersHavePlayed(data);
}

export function moretimeable(data: Data): boolean {
  return !!data.clock && isPlayerPlaying(data) && !mandatory(data);
}

export function imported(data: Data): boolean {
  return data.game.source === 'import';
}

export function replayable(data: Data): boolean {
  return imported(data) || status.finished(data) ||
    (status.aborted(data) && bothPlayersHavePlayed(data));
}

export function getPlayer(data: Data, color: Color): Player;
export function getPlayer(data: Data, color?: Color): Player | null {
  if (data.player.color == color) return data.player;
  if (data.opponent.color == color) return data.opponent;
  return null;
}

export function hasAi(data: Data): boolean {
  return data.player.ai || data.opponent.ai;
}

export function userAnalysable(data: Data): boolean {
  return playable(data) && (!data.clock || !isPlayerPlaying(data));
}

export function isCorrespondence(data: Data): boolean {
  return data.game.speed === 'correspondence';
}

export function setOnGame(data: Data, color: Color, onGame: boolean): void {
  var player = getPlayer(data, color);
  onGame = onGame || player.ai;
  player.onGame = onGame;
  if (onGame) setIsGone(data, color, false);
}

export function setIsGone(data: Data, color: Color, isGone: boolean): void {
  var player = getPlayer(data, color);
  isGone = isGone && !player.ai;
  player.isGone = isGone;
  if (!isGone && player.user) player.user.online = true;
}

export function nbMoves(data: Data, color: Color): number {
  return Math.floor((data.game.turns + (color == 'white' ? 1 : 0)) / 2);
}

export function isSwitchable(data: Data): boolean {
  return !hasAi(data) && (!!data.simul || isCorrespondence(data));
}
