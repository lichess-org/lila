import { hl } from 'lib/snabbdom';
import type LobbyController from '../../../ctrl';
import { colors, variantsWhereWhiteIsBetter } from '../../../options';
import { option } from './option';
import { ColorOrRandom } from '@/interfaces';

const renderBlindModeColorPicker = (ctrl: LobbyController) => [
  ctrl.setupCtrl.gameType !== 'hook' && [
    hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
    hl(
      'select#sf_color',
      {
        on: {
          change: (e: Event) => ctrl.setupCtrl.color((e.target as HTMLSelectElement).value as ColorOrRandom),
        },
      },
      colors.map(color => option(color, ctrl.setupCtrl.color())),
    ),
  ],
];

export const colorButtons = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;

  const randomColorOnly =
    setupCtrl.gameType === 'hook' ||
    (setupCtrl.gameType !== 'ai' &&
      setupCtrl.gameMode() === 'rated' &&
      variantsWhereWhiteIsBetter.includes(setupCtrl.variant()));

  return randomColorOnly
    ? undefined
    : site.blindMode
      ? hl('div', renderBlindModeColorPicker(ctrl))
      : hl('span.radio-pane', [
          i18n.site.youPlayAs,
          hl(
            'group.radio.color-picker',
            colors.map(({ key, name }) => [
              hl(`input#color-picker-${key}`, {
                attrs: { name: 'color', type: 'radio', value: key, checked: key === 'random' },
                on: { change: () => ctrl.setupCtrl.color(key) },
              }),
              hl(
                `label.color-picker__button.${key}`,
                { attrs: { title: name, for: `color-picker-${key}` } },
                hl('i'),
              ),
            ]),
          ),
        ]);
};
