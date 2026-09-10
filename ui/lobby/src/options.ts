import type { GameMode, GameType } from './interfaces';

export const variantIds: Record<VariantKey, number> = {
  standard: 1,
  chess960: 2,
  kingOfTheHill: 4,
  threeCheck: 5,
  crazyhouse: 10,
  antichess: 6,
  atomic: 7,
  horde: 8,
  racingKings: 9,
  fromPosition: 3,
};

export const variantsForGameType = (gameType: GameType): VariantKey[] =>
  gameType === 'hook'
    ? Object.keys(variantIds).filter((key: VariantKey) => key !== 'fromPosition')
    : (Object.keys(variantIds) as VariantKey[]);

export const variantsWhereWhiteIsBetter: VariantKey[] = [
  'antichess',
  'atomic',
  'horde',
  'racingKings',
  'threeCheck',
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];
