import { GameMode, GameType, InputValue, RealValue, TimeMode, Variant } from './interfaces';

export const variants: Variant[] = [
  { id: 1, icon: '', key: 'standard', name: 'Standard' },
  { id: 10, icon: '', key: 'crazyhouse', name: 'Crazyhouse' },
  { id: 2, icon: '', key: 'chess960', name: 'Chess960' },
  { id: 4, icon: '', key: 'kingOfTheHill', name: 'King of the Hill' },
  { id: 5, icon: '', key: 'threeCheck', name: 'Three-check' },
  { id: 6, icon: '', key: 'antichess', name: 'Antichess' },
  { id: 7, icon: '', key: 'atomic', name: 'Atomic' },
  { id: 8, icon: '', key: 'horde', name: 'Horde' },
  { id: 9, icon: '', key: 'racingKings', name: 'Racing Kings' },
  { id: 3, icon: '', key: 'fromPosition', name: 'From Position' },
];

export const variantsBlindMode: Variant[] = variants.filter(({ key }: Variant) =>
  [
    'standard',
    'chess960',
    'kingOfTheHill',
    'threeCheck',
    'fromPosition',
    'antichess',
    'atomic',
    'racingKings',
    'horde',
  ].includes(key)
);

export const variantsForGameType = (baseVariants: Variant[], gameType: GameType): Variant[] =>
  gameType === 'hook' ? baseVariants.filter(({ key }) => key !== 'fromPosition') : baseVariants;

export const variantsWhereWhiteIsBetter: VariantKey[] = ['antichess', 'atomic', 'horde', 'racingKings', 'threeCheck'];

export const speeds: { key: Speed; name: string; icon: string }[] = [
  { icon: '', key: 'ultraBullet', name: 'UltraBullet' },
  { icon: '', key: 'bullet', name: 'Bullet' },
  { icon: '', key: 'blitz', name: 'Blitz' },
  { icon: '', key: 'rapid', name: 'Rapid' },
  { icon: '', key: 'classical', name: 'Classical' },
  { icon: '', key: 'correspondence', name: 'Correspondence' },
];

export const timeModes = (trans: Trans): { id: number; key: TimeMode; name: string }[] => [
  { id: 1, key: 'realTime', name: trans('realTime') },
  { id: 2, key: 'correspondence', name: trans('correspondence') },
  { id: 0, key: 'unlimited', name: trans('unlimited') },
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
  max: InputValue
): InputValue | undefined => {
  for (let i = 0; i < max; i++) {
    if (f(i) === v) return i;
  }
  return undefined;
};

export const gameModes = (trans: Trans): { key: GameMode; name: string }[] => [
  { key: 'casual', name: trans('casual') },
  { key: 'rated', name: trans('rated') },
];

export const colors = (trans: Trans): { key: Color | 'random'; name: string }[] => [
  { key: 'black', name: trans('black') },
  { key: 'random', name: trans('randomColor') },
  { key: 'white', name: trans('white') },
];
