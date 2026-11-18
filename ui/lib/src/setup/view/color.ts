import { hl, type VNode } from '@/view';
import { radioGroup } from '@/view/radio';

import { colorChoiceName, colors, type ColorProp } from '../color';

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
