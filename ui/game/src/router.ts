import { GameData, ContinueMode } from './interfaces';

export function player(data: GameData): string {
  return '/' + data.game.id + data.player.id;
}

export function game(data: GameData, color?: Color, embed?: boolean): string;
export function game(data: string, color?: Color, embed?: boolean): string;
export function game(data: any, color?: Color, embed?: boolean): string {
  const id = data.game ? data.game.id : data;
  return (embed ? '/embed/' : '/') + id + (color ? '/' + color : '');
}

export function cont(data: GameData, mode: ContinueMode): string {
  return game(data) + '/continue/' + mode;
}
