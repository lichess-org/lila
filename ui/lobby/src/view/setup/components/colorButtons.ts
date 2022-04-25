import { h } from 'snabbdom';
import { spinnerVdom } from 'common/spinner';
import LobbyController from '../../../ctrl';
import { colors, variantsWhereWhiteIsBetter } from '../../../options';
import { option } from './option';

const renderBlindModeColorPicker = (ctrl: LobbyController) => [
  ...(ctrl.setupCtrl.gameType === 'hook'
    ? []
    : [
        h('label', { attrs: { for: 'sf_color' } }, ctrl.trans('side')),
        h(
          'select#sf_color',
          {
            on: {
              change: (e: Event) =>
                ctrl.setupCtrl.blindModeColor((e.target as HTMLSelectElement).value as Color | 'random'),
            },
          },
          colors(ctrl.trans).map(color => option(color, ctrl.setupCtrl.blindModeColor()))
        ),
      ]),
  h('button', { on: { click: () => ctrl.setupCtrl.submit(ctrl.setupCtrl.blindModeColor()) } }, 'Create the game'),
];

export const colorButtons = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  const validTime = setupCtrl.timeMode() !== 'realTime' || setupCtrl.time() > 0 || setupCtrl.increment() > 0;
  const validAi =
    setupCtrl.gameType !== 'ai' ||
    setupCtrl.timeMode() !== 'realTime' ||
    setupCtrl.variant() !== 'fromPosition' ||
    setupCtrl.time() >= 1;
  const validFen = setupCtrl.variant() !== 'fromPosition' || !setupCtrl.fenError;
  const randomColorOnly =
    setupCtrl.gameType !== 'ai' &&
    setupCtrl.gameMode() === 'rated' &&
    variantsWhereWhiteIsBetter.includes(setupCtrl.variant());

  const enabledColors: (Color | 'random')[] = [];
  if (validTime && validAi && validFen) {
    enabledColors.push('random');
    if (!randomColorOnly) enabledColors.push('white', 'black');
  }

  return h(
    'div.color-submits',
    ctrl.opts.blindMode
      ? renderBlindModeColorPicker(ctrl)
      : setupCtrl.loading
      ? spinnerVdom()
      : colors(ctrl.trans).map(({ key, name }) =>
          h(
            `button.button.button-metal.color-submits__button.${key}`,
            {
              attrs: { disabled: !enabledColors.includes(key), title: name, value: key },
              on: { click: () => ctrl.setupCtrl.submit(key) },
            },
            h('i')
          )
        )
  );
};
