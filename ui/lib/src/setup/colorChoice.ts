import type { ColorOrRandom, ColorProp } from './interfaces';
import { hl, type VNode } from '@/snabbdom';
import { option } from './option';

const colors: { key: ColorOrRandom; name: string }[] = [
  { key: 'black', name: i18n.site.black },
  { key: 'random', name: i18n.site.randomColor },
  { key: 'white', name: i18n.site.white },
];

export const blindModeColorPicker: (c: ColorProp) => VNode[] = (colorProp: ColorProp) => [
  hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
  hl(
    'select#sf_color',
    {
      on: {
        change: (e: Event) => colorProp((e.target as HTMLSelectElement).value as ColorOrRandom),
      },
    },
    colors.map(color => option(color, colorProp())),
  ),
];

export const colorButtons: (c: ColorProp) => VNode = colorProp =>
  hl('div.radio-pane', [
    i18n.site.youPlayAs,
    hl(
      'group.radio.color-picker',
      colors.map(({ key, name }) => [
        hl(`input#color-picker-${key}`, {
          attrs: { name: 'color', type: 'radio', value: key, checked: key === 'random' },
          on: { change: () => colorProp(key) },
        }),
        hl(
          `label.color-picker__button.${key}`,
          { attrs: { title: name, for: `color-picker-${key}` } },
          hl('i'),
        ),
      ]),
    ),
  ]);
