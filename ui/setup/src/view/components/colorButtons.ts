import { h } from 'snabbdom';
import { spinnerVdom } from 'common/spinner';
import { SetupCtrl } from '../../ctrl';
import { colors, variantsWhereWhiteIsBetter } from '../../options';
import { option } from './option';

const renderBlindModeColorPicker = (ctrl: SetupCtrl) => [
  ...(ctrl.gameType === 'hook'
    ? []
    : [
        h('label', { attrs: { for: 'sf_color' } }, ctrl.root.trans('side')),
        h(
          'select#sf_color',
          {
            on: {
              change: (e: Event) =>
                ctrl.blindModeColor((e.target as HTMLSelectElement).value as Color | 'random'),
            },
          },
          colors(ctrl.root.trans).map(color => option(color, ctrl.blindModeColor())),
        ),
      ]),
  h('button', { on: { click: () => ctrl.submit(ctrl.blindModeColor()) } }, 'Create the game'),
];

export const colorButtons = (ctrl: SetupCtrl) => {
  const enabledColors: (Color | 'random')[] = [];
  if (ctrl.valid()) {
    enabledColors.push('random');

    const randomColorOnly =
      ctrl.gameType !== 'ai' &&
      ctrl.gameMode() === 'rated' &&
      variantsWhereWhiteIsBetter.includes(ctrl.variant());
    if (!randomColorOnly) enabledColors.push('white', 'black');
  }

  return h(
    'div.color-submits',
    lichess.blindMode
      ? renderBlindModeColorPicker(ctrl)
      : ctrl.loading
      ? spinnerVdom()
      : colors(ctrl.root.trans).map(({ key, name }) =>
          h(
            `button.button.button-metal.color-submits__button.${key}`,
            {
              attrs: { disabled: !enabledColors.includes(key), title: name, value: key },
              on: { click: () => ctrl.submit(key) },
            },
            h('i'),
          ),
        ),
  );
};
