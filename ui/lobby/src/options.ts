import { icons, type Icon } from 'lib/icons';

import type { GameMode, GameType, Variant } from './interfaces';

export const variants: Variant[] = [
  {
    id: 1,
    icon: icons.CrownElite,
    key: 'standard',
    name: i18n.variant.standard,
    description: i18n.variant.standardTitle,
  },
  {
    id: 2,
    icon: icons.DieSix,
    key: 'chess960',
    name: i18n.variant.chess960,
    description: i18n.variant.chess960Title,
  },
  {
    id: 4,
    icon: icons.FlagKingHill,
    key: 'kingOfTheHill',
    name: i18n.variant.kingOfTheHill,
    description: i18n.variant.kingOfTheHillTitle,
  },
  {
    id: 5,
    icon: icons.ThreeCheckStack,
    key: 'threeCheck',
    name: i18n.variant.threeCheck,
    description: i18n.variant.threeCheckTitle,
  },
  {
    id: 10,
    icon: icons.Crazyhouse,
    key: 'crazyhouse',
    name: i18n.variant.crazyhouse,
    description: i18n.variant.crazyhouseTitle,
  },
  {
    id: 6,
    icon: icons.Antichess,
    key: 'antichess',
    name: i18n.variant.antichess,
    description: i18n.variant.antichessTitle,
  },
  {
    id: 7,
    icon: icons.Atom,
    key: 'atomic',
    name: i18n.variant.atomic,
    description: i18n.variant.atomicTitle,
  },
  {
    id: 8,
    icon: icons.Keypad,
    key: 'horde',
    name: i18n.variant.horde,
    description: i18n.variant.hordeTitle,
  },
  {
    id: 9,
    icon: icons.FlagRacingKings,
    key: 'racingKings',
    name: i18n.variant.racingKings,
    description: i18n.variant.racingKingsTitle,
  },
  {
    id: 3,
    icon: icons.Pencil,
    key: 'fromPosition',
    name: i18n.variant.fromPosition,
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

export const speeds: { key: Speed; name: string; icon: Icon }[] = [
  { icon: icons.UltraBullet, key: 'ultraBullet', name: i18n.site.ultraBullet },
  { icon: icons.Bullet, key: 'bullet', name: i18n.site.bullet },
  { icon: icons.FlameBlitz, key: 'blitz', name: i18n.site.blitz },
  { icon: icons.Rabbit, key: 'rapid', name: i18n.site.rapid },
  { icon: icons.Turtle, key: 'classical', name: i18n.site.classical },
  { icon: icons.PaperAirplane, key: 'correspondence', name: i18n.site.correspondence },
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];
