import type { Prop } from '@/common';

export type ColorChoice = Color | 'random';

export type ColorProp = Prop<ColorChoice>;

export const colors: { key: ColorChoice; name: string }[] = [
  { key: 'black', name: i18n.site.black },
  { key: 'random', name: i18n.site.randomColor },
  { key: 'white', name: i18n.site.white },
];
