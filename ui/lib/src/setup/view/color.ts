import { hl, onChange, type VNode } from '@/view';
import { radioGroup } from '@/view/radio';

import { colorChoiceName, colors, type ColorProp } from '../color';
import { option } from '../option';

export const blindModeColorPicker = (colorProp: ColorProp): VNode[] => [
  hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
  hl(
    'select#sf_color',
    {
      on: onChange(colorProp),
    },
    colors.map(color => option({ key: color, name: colorChoiceName(color) }, colorProp())),
  ),
];

export const colorButtons = (colorProp: ColorProp): VNode =>
  hl('div.config-group', [
    hl('div.label', i18n.site.side),
    radioGroup({
      name: 'color',
      active: colorProp(),
      onChange: colorProp,
      buttons: colors.map(c => ({
        key: c,
        content: [hl(`div.color-picker__button.${c}`, hl('icon')), hl('span.text', colorChoiceName(c))],
      })),
      attrs: { class: 'radio color-picker color-cards' },
    }),
  ]);
