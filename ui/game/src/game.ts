import { GameData, Player } from './interfaces';
import * as status from './status';

export * from './interfaces';

export function playable(data: GameData): boolean {
  return data.game.status.id < status.ids.aborted && !imported(data) && !status.paused(data);
}

export function playableEvenPaused(data: GameData): boolean {
  return data.game.status.id < status.ids.aborted && !imported(data);
}

export function isPlayerPlaying(data: GameData): boolean {
  return playable(data) && !data.player.spectator;
}

export function isPlayerTurn(data: GameData): boolean {
  return isPlayerPlaying(data) && data.game.player == data.player.color;
}

export function isFriendGame(data: GameData): boolean {
  return data.game.source === 'friend';
}

export function isClassical(data: GameData): boolean {
  return data.game.perf === 'classical';
}

export function mandatory(data: GameData): boolean {
  return !!data.tournament || !!data.simul;
}

export function playedPlies(data: GameData): number {
  return data.game.plies - (data.game.startedAtPly || 0);
}

export function bothPlayersHavePlayed(data: GameData): boolean {
  return playedPlies(data) > 1;
}

export function abortable(data: GameData): boolean {
  return playable(data) && !bothPlayersHavePlayed(data) && !mandatory(data);
}

export function takebackable(data: GameData): boolean {
  return (
    playable(data) &&
    data.takebackable &&
    bothPlayersHavePlayed(data) &&
    !data.player.proposingTakeback &&
    !data.opponent.proposingTakeback
  );
}

export function drawable(data: GameData): boolean {
  return playable(data) && data.game.plies >= 2 && !data.player.offeringDraw && !hasAi(data);
}

export function pausable(data: GameData): boolean {
  return (
    data.game.variant.key === 'chushogi' &&
    playable(data) &&
    data.game.plies >= 20 &&
    !data.player.offeringPause &&
    !hasAi(data)
  );
}

export function resignable(data: GameData): boolean {
  return playable(data) && !abortable(data);
}

// can the current player go berserk?
export function berserkableBy(data: GameData): boolean {
  return !!data.tournament && data.tournament.berserkable && isPlayerPlaying(data) && !bothPlayersHavePlayed(data);
}

export function moretimeable(data: GameData): boolean {
  return (
    isPlayerPlaying(data) &&
    data.moretimeable &&
    (!!data.clock ||
      (!!data.correspondence && data.correspondence[data.opponent.color] < data.correspondence.increment - 3600))
  );
}

export function imported(data: GameData): boolean {
  return data.game.source === 'import';
}

export function replayable(data: GameData): boolean {
  return imported(data) || status.finished(data) || (status.aborted(data) && bothPlayersHavePlayed(data));
}

export function getPlayer(data: GameData, color: Color | undefined): Player;
export function getPlayer(data: GameData, color?: Color): Player | null {
  if (data.player.color === color) return data.player;
  if (data.opponent.color === color) return data.opponent;
  return null;
}

export function hasAi(data: GameData): boolean {
  return !!(data.player.ai || data.opponent.ai);
}

export function userAnalysable(data: GameData): boolean {
  return status.finished(data) || (playable(data) && (!data.clock || !isPlayerPlaying(data)));
}

export function isCorrespondence(data: GameData): boolean {
  return data.game.speed === 'correspondence';
}

export function setOnGame(data: GameData, color: Color, onGame: boolean): void {
  const player = getPlayer(data, color);
  onGame = onGame || !!player.ai;
  player.onGame = onGame;
  if (onGame) setGone(data, color, false);
}

export function setGone(data: GameData, color: Color, gone: number | boolean): void {
  const player = getPlayer(data, color);
  player.gone = !player.ai && gone;
  if (player.gone === false && player.user) player.user.online = true;
}

export function nbMoves(data: GameData, color: Color): number {
  return Math.floor((data.game.plies + (color == 'sente' ? 1 : 0)) / 2);
}

export function isSwitchable(data: GameData): boolean {
  return !hasAi(data) && (!!data.simul || isCorrespondence(data));
}
