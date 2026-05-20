import type { GameData, ContinueMode } from './interfaces';

export function game(data: GameData | string, color?: Color, embed?: boolean): string {
  const id = typeof data === 'string' ? data : data.game.id;
  return (embed ? '/embed/' : '/') + id + (color ? '/' + color : '');
}

export function cont(data: GameData, mode: ContinueMode): string {
  return game(data) + '/continue/' + mode;
}
