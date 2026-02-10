import { hl, type VNode } from '@/view';
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
  hl('div.config-group', [
    hl('div.label', i18n.site.side),
    hl(
      'group.radio.color-picker.color-cards',
      colors.map(({ key, name }) =>
        hl('div', [
          hl(`input#color-picker-${key}`, {
            attrs: { name: 'color', type: 'radio', value: key, checked: colorProp() === key },
            on: { change: () => colorProp(key) },
          }),
          hl(`label.card-radio`, { attrs: { for: `color-picker-${key}` } }, [
            hl('div.color-picker__button', { class: { [key]: true } }, hl('i')),
            hl('span.text', name),
          ]),
        ]),
      ),
    ),
  ]);
