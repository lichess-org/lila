import { hl, type VNode } from '@/snabbdom';
import { option } from '../option';
import { colors, type ColorChoice, type ColorProp } from '../color';

export const blindModeColorPicker = (colorProp: ColorProp): VNode[] => [
  hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
  hl(
    'select#sf_color',
    {
      on: {
        change: (e: Event) => colorProp((e.target as HTMLSelectElement).value as ColorChoice),
      },
    },
    colors.map(color => option(color, colorProp())),
  ),
];

export const colorButtons = (colorProp: ColorProp): VNode =>
  hl('div.radio-pane', [
    i18n.site.youPlayAs,
    hl(
      'group.radio.color-picker',
      colors.map(({ key, name }) => [
        hl(`input#color-picker-${key}`, {
          attrs: { name: 'color', type: 'radio', value: key, checked: key === colorProp() },
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
