import type { Prop } from '@/common';

export type ColorOrRandom = Color | 'random';

export type ColorProp = Prop<ColorOrRandom>;
