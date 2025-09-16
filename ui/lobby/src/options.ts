import * as licon from 'lib/licon';

import type {
  ColorOrRandom,
  GameMode,
  GameType,
  InputValue,
  RealValue,
  TimeMode,
  Variant,
} from './interfaces';

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

export const timeModes: { id: number; key: TimeMode; name: string }[] = [
  { id: 1, key: 'realTime', name: i18n.site.realTime },
  { id: 2, key: 'correspondence', name: i18n.site.correspondence },
  { id: 0, key: 'unlimited', name: i18n.site.unlimited },
];

export const keyToId = (key: string, items: { id: number; key: string }[]): number =>
  items.find(item => item.key === key)!.id;

export const sliderTimes = [
  0,
  1 / 4,
  1 / 2,
  3 / 4,
  1,
  3 / 2,
  2,
  3,
  4,
  5,
  6,
  7,
  8,
  9,
  10,
  11,
  12,
  13,
  14,
  15,
  16,
  17,
  18,
  19,
  20,
  25,
  30,
  35,
  40,
  45,
  60,
  75,
  90,
  105,
  120,
  135,
  150,
  165,
  180,
];

export const timeVToTime = (v: InputValue): RealValue => (v < sliderTimes.length ? sliderTimes[v] : 180);

export const incrementVToIncrement = (v: InputValue): RealValue => {
  if (v <= 20) return v;
  switch (v) {
    case 21:
      return 25;
    case 22:
      return 30;
    case 23:
      return 35;
    case 24:
      return 40;
    case 25:
      return 45;
    case 26:
      return 60;
    case 27:
      return 90;
    case 28:
      return 120;
    case 29:
      return 150;
    default:
      return 180;
  }
};

export const daysVToDays = (v: InputValue): RealValue => {
  if (v <= 3) return v;
  switch (v) {
    case 4:
      return 5;
    case 5:
      return 7;
    case 6:
      return 10;
    default:
      return 14;
  }
};

// When we store timeV, incrementV, and daysV in local storage, we save the actual time, increment,
// and days, and not the value of the input element. We use this function to recompute the value of the
// input element.
export const sliderInitVal = (
  v: RealValue,
  f: (x: InputValue) => RealValue,
  max: InputValue,
): InputValue | undefined => {
  for (let i = 0; i < max; i++) {
    if (f(i) === v) return i;
  }
  return undefined;
};

export const gameModes: { key: GameMode; name: string }[] = [
  { key: 'casual', name: i18n.site.casual },
  { key: 'rated', name: i18n.site.rated },
];

export const colors: { key: ColorOrRandom; name: string }[] = [
  { key: 'black', name: i18n.site.black },
  { key: 'random', name: i18n.site.randomColor },
  { key: 'white', name: i18n.site.white },
];
