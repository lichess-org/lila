import type { GameMode, GameType, Variant } from './interfaces';

export const variants: Variant[] = [
  {
    id: 1,
    key: 'standard',
    description: i18n.variant.standardTitle,
  },
  {
    id: 2,
    key: 'chess960',
    description: i18n.variant.chess960Title,
  },
  {
    id: 4,
    key: 'kingOfTheHill',
    description: i18n.variant.kingOfTheHillTitle,
  },
  {
    id: 5,
    key: 'threeCheck',
    description: i18n.variant.threeCheckTitle,
  },
  {
    id: 10,
    key: 'crazyhouse',
    description: i18n.variant.crazyhouseTitle,
  },
  {
    id: 6,
    key: 'antichess',
    description: i18n.variant.antichessTitle,
  },
  {
    id: 7,
    key: 'atomic',
    description: i18n.variant.atomicTitle,
  },
  {
    id: 8,
    key: 'horde',
    description: i18n.variant.hordeTitle,
  },
  {
    id: 9,
    key: 'racingKings',
    description: i18n.variant.racingKingsTitle,
  },
  {
    id: 3,
    key: 'fromPosition',
    description: i18n.variant.fromPositionTitle,
  },
];

export const variantsForGameType = (baseVariants: Variant[], gameType: GameType): Variant[] =>
  gameType === 'hook' ? baseVariants.filter(({ key }) => key !== 'fromPosition') : baseVariants;

export const variantsWhereWhiteIsBetter: VariantKey[] = [
  'antichess',
  'atomic',
  'horde',
  'racingKings',
  'threeCheck',
];

export const speeds: { key: Speed }[] = [
  { key: 'ultraBullet' },
  { key: 'bullet' },
  { key: 'blitz' },
  { key: 'rapid' },
  { key: 'classical' },
  { key: 'correspondence' },
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];
