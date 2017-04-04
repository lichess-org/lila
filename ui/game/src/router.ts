import { Data, ContinueMode } from './interfaces';

export function player(data: Data): string {
  return '/' + data.game.id + data.player.id;
}

export function game(data: Data, color?: Color, embed?: boolean): string {
  return (embed ? '/embed/' : '/') + (data.game ? data.game.id : data) + (color ? '/' + color : '');
}

export function forecasts(data: Data): string {
  return player(data) + '/forecasts';
}

export function cont(data: Data, mode: ContinueMode): string {
  return game(data) + '/continue/' + mode;
}
