import * as licon from 'lib/licon';

import type { GameMode, GameType, Variant } from './interfaces';

export const variants: Variant[] = [
  { id: 1, icon: licon.Crown, key: 'standard', name: 'Standard' },
  { id: 10, icon: licon.Crazyhouse, key: 'crazyhouse', name: 'Crazyhouse' },
  { id: 2, icon: licon.DieSix, key: 'chess960', name: 'Chess960' },
  { id: 4, icon: licon.FlagKingHill, key: 'kingOfTheHill', name: 'King of the Hill' },
  { id: 5, icon: licon.ThreeCheckStack, key: 'threeCheck', name: 'Three-check' },
  { id: 6, icon: licon.Antichess, key: 'antichess', name: 'Antichess' },
  { id: 7, icon: licon.Atom, key: 'atomic', name: 'Atomic' },
  { id: 8, icon: licon.Keypad, key: 'horde', name: 'Horde' },
  { id: 9, icon: licon.FlagRacingKings, key: 'racingKings', name: 'Racing Kings' },
  { id: 3, icon: licon.Crown, key: 'fromPosition', name: 'From Position' },
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
  { icon: licon.UltraBullet, key: 'ultraBullet', name: 'UltraBullet' },
  { icon: licon.Bullet, key: 'bullet', name: 'Bullet' },
  { icon: licon.FlameBlitz, key: 'blitz', name: 'Blitz' },
  { icon: licon.Rabbit, key: 'rapid', name: 'Rapid' },
  { icon: licon.Turtle, key: 'classical', name: 'Classical' },
  { icon: licon.PaperAirplane, key: 'correspondence', name: 'Correspondence' },
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];
