import { propWithEffect, type Prop } from '@/index';
import type { ClockConfig, InputValue, RealValue } from './interfaces';
import { clockToSpeed } from '@/game';

export type TimeMode = 'realTime' | 'correspondence' | 'unlimited';

export class TimeControl {
  constructor(
    readonly mode: Prop<TimeMode>,
    readonly modes: TimeMode[],
    // The following three quantities are suffixed with 'V' to draw attention to the
    // fact that they are not the true quantities. They represent the value of the
    // input element. Use time(), increment(), and days() below for the true quantities.
    readonly timeV: Prop<InputValue>,
    readonly incrementV: Prop<InputValue>,
    readonly daysV: Prop<InputValue>,
    readonly presets: ClockConfig[],
  ) {}

  time: () => RealValue = () => timeVToTime(this.timeV());
  increment: () => RealValue = () => incrementVToIncrement(this.incrementV());
  days: () => RealValue = () => daysVToDays(this.daysV());

  isRealTime = (): boolean => this.mode() === 'realTime';

  realTimeValid = (minimumTime: number = 0): boolean =>
    this.time() >= minimumTime && (this.time() > 0 || this.increment() > 0);

  valid = (minimumTimeIfReal: number = 0): boolean =>
    !this.isRealTime() || this.realTimeValid(minimumTimeIfReal);

  initialSeconds = (): Seconds => this.time() * 60;

  notForRatedVariant = (): boolean =>
    !this.isRealTime() ||
    (this.time() < 0.5 && this.increment() === 0) ||
    (this.time() === 0 && this.increment() < 2);

  clockStr = (): string => `${this.time()}+${this.increment()}`;

  speed = (): Speed =>
    this.isRealTime() ? clockToSpeed(this.initialSeconds(), this.increment()) : 'correspondence';

  canSelectMode = (): boolean => this.modes.length > 1;
}

export const timeControlFromStoredValues = (
  mode: Prop<TimeMode>,
  modes: TimeMode[],
  time: RealValue,
  inc: RealValue,
  days: RealValue,
  onChange: () => void,
  presets: ClockConfig[],
): TimeControl =>
  new TimeControl(
    mode,
    modes,
    propWithEffect(sliderInitVal(time, timeVToTime, 100, 14), onChange),
    propWithEffect(sliderInitVal(inc, incrementVToIncrement, 100, 5), onChange),
    propWithEffect(sliderInitVal(days, daysVToDays, 20, 7), onChange),
    presets,
  );

export const timeModes: { id: number; key: TimeMode; name: string }[] = [
  { id: 1, key: 'realTime', name: i18n.site.realTime },
  { id: 2, key: 'correspondence', name: i18n.site.correspondence },
  { id: 0, key: 'unlimited', name: i18n.site.unlimited },
];

export const allTimeModeKeys: TimeMode[] = ['realTime', 'correspondence', 'unlimited'];

// When we store timeV, incrementV, and daysV in local storage, we save the actual time, increment,
// and days, and not the value of the input element. We use this function to recompute the value of the
// input element.
export const sliderInitVal = (
  v: RealValue,
  f: (x: InputValue) => RealValue,
  max: InputValue,
  defaultVal: InputValue,
): InputValue => {
  for (let i = 0; i < max; i++) {
    if (f(i) === v) return i;
  }
  return defaultVal;
};

export const sliderTimes: number[] = [
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
