import type { GameData } from './interfaces';

export function game(data: GameData | string, color?: Color, embed?: boolean): string {
  const id = typeof data === 'string' ? data : data.game.id;
  return (embed ? '/embed/' : '/game/') + id + (color ? '/' + color : '');
}
