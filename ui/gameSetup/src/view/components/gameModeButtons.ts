import { MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';
import { GameMode } from '../../interfaces';
import { gameModes } from '../../options';

export const gameModeButtons = (ctrl: SetupCtrl): MaybeVNode => {
  if (!ctrl.root.user) return null;

  return h(
    'div.mode-choice.buttons',
    h(
      'group.radio',
      gameModes(ctrl.root.trans).map(({ key, name }) => {
        const disabled = key === 'rated' && ctrl.ratedModeDisabled();
        return h('div', [
          h(`input#sf_mode_${key}.checked_${key === ctrl.gameMode()}`, {
            attrs: {
              name,
              type: 'radio',
              value: key,
              checked: key === ctrl.gameMode(),
              disabled,
            },
            on: {
              change: (e: Event) => ctrl.gameMode((e.target as HTMLInputElement).value as GameMode),
            },
          }),
          h('label', { class: { disabled }, attrs: { for: `sf_mode_${key}` } }, name),
        ]);
      }),
    ),
  );
};
