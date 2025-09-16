import { hl } from 'lib/snabbdom';
import type LobbyController from '../../../ctrl';
import { colors, variantsWhereWhiteIsBetter } from '../../../options';
import { option } from './option';

const renderBlindModeColorPicker = (ctrl: LobbyController) => [
  ctrl.setupCtrl.gameType !== 'hook' && [
    hl('label', { attrs: { for: 'sf_color' } }, i18n.site.side),
    hl(
      'select#sf_color',
      {
        on: {
          change: (e: Event) =>
            ctrl.setupCtrl.blindModeColor((e.target as HTMLSelectElement).value as Color | 'random'),
        },
      },
      colors.map(color => option(color, ctrl.setupCtrl.blindModeColor())),
    ),
  ],
  hl(
    'button',
    { on: { click: () => ctrl.setupCtrl.submit(ctrl.setupCtrl.blindModeColor()) } },
    i18n.site.createTheGame,
  ),
];

export const colorButtons = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  const enabledColors: (Color | 'random')[] = ['random'];

  const randomColorOnly =
    setupCtrl.gameType === 'hook' ||
    (setupCtrl.gameType !== 'ai' &&
      setupCtrl.gameMode() === 'rated' &&
      variantsWhereWhiteIsBetter.includes(setupCtrl.variant()));
  if (!randomColorOnly) enabledColors.push('white', 'black');

  if (site.blindMode) return hl('div', renderBlindModeColorPicker(ctrl));
  return (
    enabledColors.length > 1 &&
    hl('span.radio-pane', [
      i18n.site.youPlayAs,
      hl(
        'group.radio.color-picker',
        colors.map(({ key, name }) => [
          hl(`input#color-picker-${key}`, {
            attrs: { name: 'color', type: 'radio', value: key, checked: key === 'random' },
          }),
          hl(
            `label.color-picker__button.${key}`,
            { attrs: { title: name, for: `color-picker-${key}` } },
            hl('i'),
          ),
        ]),
      ),
    ])
  );
};
