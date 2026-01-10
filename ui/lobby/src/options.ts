import * as licon from 'lib/licon';

import type { GameMode, GameType, Variant } from './interfaces';

export const variants: Variant[] = [
  { id: 1, icon: licon.Crown, key: 'standard', name: i18n.variant.standard },
  { id: 10, icon: licon.Crazyhouse, key: 'crazyhouse', name: i18n.variant.crazyhouse },
  { id: 2, icon: licon.DieSix, key: 'chess960', name: i18n.variant.chess960 },
  { id: 4, icon: licon.FlagKingHill, key: 'kingOfTheHill', name: i18n.variant.kingOfTheHill },
  { id: 5, icon: licon.ThreeCheckStack, key: 'threeCheck', name: i18n.variant.threeCheck },
  { id: 6, icon: licon.Antichess, key: 'antichess', name: i18n.variant.antichess },
  { id: 7, icon: licon.Atom, key: 'atomic', name: i18n.variant.atomic },
  { id: 8, icon: licon.Keypad, key: 'horde', name: i18n.variant.horde },
  { id: 9, icon: licon.FlagRacingKings, key: 'racingKings', name: i18n.variant.racingKings },
  { id: 3, icon: licon.Crown, key: 'fromPosition', name: i18n.site.fromPosition },
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

export const speeds: { key: Speed; name: string; icon: string }[] = [
  { icon: licon.UltraBullet, key: 'ultraBullet', name: i18n.site.ultraBullet },
  { icon: licon.Bullet, key: 'bullet', name: i18n.site.bullet },
  { icon: licon.FlameBlitz, key: 'blitz', name: i18n.site.blitz },
  { icon: licon.Rabbit, key: 'rapid', name: i18n.site.rapid },
  { icon: licon.Turtle, key: 'classical', name: i18n.site.classical },
  { icon: licon.PaperAirplane, key: 'correspondence', name: i18n.site.correspondence },
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];
