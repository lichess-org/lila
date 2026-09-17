import { variants } from 'lib/game/perf';

import type { GameMode, GameType } from './interfaces';

export const variantsForGameType = (gameType: GameType): VariantKey[] =>
  gameType === 'hook' ? variants.filter(key => key !== 'fromPosition') : variants;

export const variantsWhereWhiteIsBetter: VariantKey[] = [
  'antichess',
  'atomic',
  'horde',
  'racingKings',
  'threeCheck',
];

export const gameModes: GameMode[] = ['casual', 'rated'];
