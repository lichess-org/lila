import type { Prop } from '@/common';

export type ColorOrRandom = Color | 'random';

export type ColorProp = Prop<ColorOrRandom>;

// These are not true quantities. They represent the value of input elements
export type InputValue = number;
// Visible value computed from the input value
export type RealValue = number;

export type TimeMode = 'realTime' | 'correspondence' | 'unlimited';
