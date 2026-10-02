import type { Prop } from '@/index';

export type ColorChoice = Color | 'random';

export type ColorProp = Prop<ColorChoice>;

export const colors: ColorChoice[] = ['white', 'random', 'black'];

export const colorChoiceName = (color: ColorChoice): string =>
  color === 'random' ? i18n.site.randomColor : i18n.site[color];
