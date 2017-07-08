import { GameData, ContinueMode } from './interfaces';

export function player(data: GameData): string {
  return '/' + data.game.id + data.player.id;
}

export function game(data: GameData, color?: Color, embed?: boolean): string {
  return (embed ? '/embed/' : '/') + (data.game ? data.game.id : data) + (color ? '/' + color : '');
}

export function cont(data: GameData, mode: ContinueMode): string {
  return game(data) + '/continue/' + mode;
}
