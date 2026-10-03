import { hl, type VNode } from '@/view';

import { colorChoiceName, colors, type ColorChoice, type ColorProp } from '../color';
import { option } from '../option';

export const blindModeColorPicker = (colorProp: ColorProp): VNode[] => [
  hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
  hl(
    'select#sf_color',
    {
      on: {
        change: (e: Event) => colorProp((e.target as HTMLSelectElement).value as ColorChoice),
      },
    },
    colors.map(color => option({ key: color, name: colorChoiceName(color) }, colorProp())),
  ),
];

export const colorButtons = (colorProp: ColorProp): VNode =>
  hl('div.config-group', [
    hl('div.label', i18n.site.side),
    hl(
      'group.radio.color-picker.color-cards',
      colors.map(c =>
        hl('div', [
          hl(`input#color-picker-${c}`, {
            attrs: { name: 'color', type: 'radio', value: c, checked: colorProp() === c },
            on: { change: () => colorProp(c) },
          }),
          hl(`label.card-radio`, { attrs: { for: `color-picker-${c}` } }, [
            hl('div.color-picker__button', { class: { [c]: true } }, hl('icon')),
            hl('span.text', colorChoiceName(c)),
          ]),
        ]),
      ),
    ),
  ]);
