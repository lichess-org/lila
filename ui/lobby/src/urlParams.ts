import { colors, type ColorChoice } from 'lib/setup/color';

import type { ForceSetupOptions, GameType } from './interfaces';

export interface ParsedUrlParams {
  gameType: GameType;
  forceOptions: ForceSetupOptions;
  friendUser?: string;
}

const gameTypes = ['ai', 'friend', 'hook'] as GameType[];

export function parseUrlParams(url: URL | Location): ParsedUrlParams | undefined {
  if (!gameTypes.includes(url.hash.slice(1) as GameType)) return undefined;

  const gameType = url.hash.slice(1) as GameType;
  const urlParams = new URLSearchParams(url.search);
  const forceOptions: ForceSetupOptions = {};
  const friendUser = urlParams.get('user') ?? undefined;
  const variant = urlParams.get('variant');

  if (variant) forceOptions.variant = variant as VariantKey;

  if (gameType !== 'hook' && urlParams.get('fen')) {
    forceOptions.fen = urlParams.get('fen')!;
    forceOptions.variant = 'fromPosition';
  }

  let timeMode = urlParams.get('time');
  const days = urlParams.get('days');
  const minutesPerSide = urlParams.get('minutesPerSide');
  const increment = urlParams.get('increment');

  if (!timeMode) {
    if (days) timeMode = 'correspondence';
    else if (minutesPerSide || increment) timeMode = 'realTime';
  }

  if (timeMode === 'correspondence') {
    forceOptions.timeMode = 'correspondence';
    if (days) forceOptions.days = parseInt(days);
  } else if (timeMode === 'realTime') {
    forceOptions.timeMode = 'realTime';
    if (minutesPerSide) forceOptions.time = parseFloat(minutesPerSide);
    if (increment) forceOptions.increment = parseInt(increment);
  } else if (timeMode === 'unlimited') {
    forceOptions.timeMode = 'unlimited';
    forceOptions.mode = 'casual';
  }

  if (gameType === 'hook' || gameType === 'friend') {
    const mode = urlParams.get('gameMode');
    if (mode === 'casual' || mode === 'rated') forceOptions.mode = mode;
  }

  const color = urlParams.get('color');
  if (color && colors.some(c => c.key === color)) forceOptions.color = color as ColorChoice;

  return { gameType, forceOptions, friendUser };
}

export function makeUrl(
  gameType: Exclude<GameType, 'local'>,
  forceOptions: ForceSetupOptions = {},
  friendUser?: string,
): string {
  const urlParams = new URLSearchParams();
  const { variant, fen, timeMode, time, increment, days, mode, color } = forceOptions;

  if (variant) urlParams.set('variant', variant);
  if (gameType !== 'hook' && fen) urlParams.set('fen', fen);

  if (timeMode === 'correspondence') {
    urlParams.set('time', timeMode);
    if (days !== undefined) urlParams.set('days', days.toString());
  } else if (timeMode === 'realTime') {
    urlParams.set('time', timeMode);
    if (time !== undefined) urlParams.set('minutesPerSide', time.toString());
    if (increment !== undefined) urlParams.set('increment', increment.toString());
  } else if (timeMode === 'unlimited') urlParams.set('time', timeMode);

  if ((gameType === 'hook' || gameType === 'friend') && mode) urlParams.set('gameMode', mode);
  if (color) urlParams.set('color', color);
  if (gameType === 'friend' && friendUser) urlParams.set('user', friendUser);

  const query = urlParams.toString();
  return `/${query ? `?${query}` : ''}#${gameType}`;
}
